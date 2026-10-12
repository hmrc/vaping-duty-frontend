/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.nrs

import connectors.nrs.NrsConnector
import models.identifiers.VpdId
import models.nrs.*
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import repositories.NrsWorkItemRepository
import services.nrs.NrsService.nonRepudiationIdentityRetrievals
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Retrieval, ~}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}
import utils.{DateTimeService, NrsUtils}

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsService @Inject()(
                            override val authConnector: AuthConnector,
                            clock: Clock,
                            nrsConnector: NrsConnector,
                            nrsUtils: NrsUtils,
                            dateTimeService: DateTimeService,
                            nrsWorkItemRepository: NrsWorkItemRepository
                          )(using ExecutionContext) extends AuthorisedFunctions with Logging {

  /**
   * Queue a work item for NRS submission. This is the method that should be called
   * after successful ETMP submission.
   *
   * @param payload      The JSON payload to submit to NRS
   * @param notableEvent The notable event identifier
   * @param periodKey    The period key for the return
   * @param vpdId        The VPD identifier
   * @return Future[Unit] - fire and forget, failures are logged
   */
  def makeWorkItemAndQueue(
                            payload: JsValue,
                            notableEvent: String,
                            periodKey: String,
                            vpdId: VpdId
                          )(using HeaderCarrier, Request[_]): Future[Unit] = {
    val payloadString = Json.stringify(payload)
    val checksum = nrsUtils.sha256Hash(payloadString)
    val timestamp = dateTimeService.timestamp
    val userAuthToken = retrieveUserAuthToken()
    val headerData = extractHeaders()

    for {
      identityData <- retrieveIdentityData()
      metaData = NrsMetadata.create(
        payLoad = payloadString,
        sha256Hash = checksum,
        identityData = identityData,
        submissionTimeStamp = timestamp,
        userAuthToken = userAuthToken,
        userHeaderData = headerData,
        vpdId = vpdId.value,
        periodKey = periodKey
      )
      encodedPayload = nrsUtils.encode(payloadString)
      _ <- nrsWorkItemRepository.pushNew(NrsSubmissionWorkItem(NrsPayload(encodedPayload, metaData))).map { _ =>
          logger.info(s"Successfully queued NRS work item for notable event: $notableEvent")
          ()
        }
        .recover { case ex =>
          logger.warn(s"Failed to queue NRS work item for notable event: $notableEvent", ex)
          ()
        }
    } yield ()
  }

  /**
   * Submit directly to NRS (used by the scheduled service to process work items).
   * Controllers should NOT call this method directly - use makeWorkItemAndQueue instead.
   *
   * @param payload The NRS payload to submit
   * @param hc      HeaderCarrier for HTTP context
   * @return Future[NrsSubmissionResult]
   */
  def submitToNrs(payload: NrsPayload)(using hc: HeaderCarrier): Future[NrsSubmissionResult] = {
    nrsConnector.submitToNrs(payload).map {
      case NrsSubmissionResult.Success =>
        logger.info(s"Successfully submitted to NRS for notable event: ${payload.metadata.notableEvent}")
        NrsSubmissionResult.Success
      case failure@NrsSubmissionResult.RetryableFailure =>
        logger.error(s"Failed to submit to NRS (retryable) for notable event: ${payload.metadata.notableEvent}")
        failure
      case failure@NrsSubmissionResult.PermanentFailure =>
        logger.error(s"Failed to submit to NRS (permanent) for notable event: ${payload.metadata.notableEvent}")
        failure
    }
  }

  /**
   * Process all pending work items from the queue. This method is called by the scheduler.
   * It pulls work items, submits them to NRS, and updates their status accordingly.
   * Continues processing until no more items are available.
   *
   * @return Future[Unit]
   */
  def processAll(): Future[Unit] = {
    given HeaderCarrier = HeaderCarrier()

    def processNext(): Future[Unit] =
      nrsWorkItemRepository.pullOutstanding(Instant.now(clock).minusSeconds(60), Instant.now(clock)).flatMap {
        case None =>
          logger.debug("No pending NRS work items to process")
          Future.successful(())
        case Some(workItem) =>
          logger.info(s"Processing NRS work item: ${workItem.id}")
          processResult(workItem).flatMap(_ => processNext())
      }

    processNext()
  }

  private def processResult(workItem: WorkItem[NrsSubmissionWorkItem])(using hc: HeaderCarrier) = {
    submitToNrs(workItem.item.payload).flatMap {
      case NrsSubmissionResult.Success =>
        nrsWorkItemRepository.complete(workItem.id, ProcessingStatus.Succeeded).map { _ =>
          logger.info(s"Successfully processed NRS work item: ${workItem.id}")
          ()
        }
      case NrsSubmissionResult.RetryableFailure =>
        nrsWorkItemRepository.markAs(workItem.id, ProcessingStatus.Failed).map { _ =>
          logger.warn(s"Failed to process NRS work item: ${workItem.id} with retryable error. Will retry with exponential backoff.")
          ()
        }
      case NrsSubmissionResult.PermanentFailure =>
        nrsWorkItemRepository.markAs(workItem.id, ProcessingStatus.PermanentlyFailed).map { _ =>
          logger.warn(s"Permanently failed to process NRS work item: ${workItem.id} with permanent error. Will not retry.")
          ()
        }
    }.recoverWith { case ex =>
      logger.error(s"Error processing NRS work item: ${workItem.id}", ex)
      nrsWorkItemRepository.markAs(workItem.id, ProcessingStatus.Failed).map(_ => ())
    }
  }

  private def retrieveUserAuthToken()(using hc: HeaderCarrier): String = {
    hc.authorization match {
      case Some(authToken) => authToken.value
      case _ =>
        logger.warn("[NrsService] - No auth token available for NRS")
        // scalafix:off DisableSyntax.throw
        throw new InternalServerException("No auth token available for NRS")
    }
  }

  private def retrieveIdentityData()(using hc: HeaderCarrier): Future[IdentityData] =
    authorised().retrieve(nonRepudiationIdentityRetrievals) {
      case affinityGroup ~ internalId ~ groupId ~ credentials ~ confidenceLevel ~ credentialRole ~ credentialStrength =>
        Future.successful(
          IdentityData(
            affinityGroup = affinityGroup,
            internalId = internalId,
            groupIdentifier = groupId,
            optionalCredentials = credentials,
            confidenceLevel = confidenceLevel,
            credentialRole = credentialRole,
            credentialStrength = credentialStrength
          )
        )
    }

  private def extractHeaders()(using request: Request[_]): Map[String, String] = {
    request.headers.toSimpleMap
  }
}

object NrsService {

  type NonRepudiationIdentityRetrievals =
    Option[AffinityGroup]
      ~ Option[String]
      ~ Option[String]
      ~ Option[Credentials]
      ~ ConfidenceLevel
      ~ Option[CredentialRole]
      ~ Option[String]

  val nonRepudiationIdentityRetrievals: Retrieval[NonRepudiationIdentityRetrievals] =
    Retrievals.affinityGroup and
      Retrievals.internalId and
      Retrievals.groupIdentifier and
      Retrievals.credentials and
      Retrievals.confidenceLevel and
      Retrievals.credentialRole and
      Retrievals.credentialStrength
}