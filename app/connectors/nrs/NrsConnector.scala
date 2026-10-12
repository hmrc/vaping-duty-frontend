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

package connectors.nrs

import config.FrontendAppConfig
import models.nrs.{NrsPayload, NrsSubmissionResult}
import org.apache.pekko.pattern.CircuitBreaker
import play.api.Logging
import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpErrorFunctions.is5xx
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.RandomUUIDGenerator

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object NrsConnector {
  case class NrsCircuitBreaker(breaker: CircuitBreaker)
}

@Singleton
class NrsConnector @Inject()(
                              httpClient: HttpClientV2,
                              appConfig: FrontendAppConfig,
                              nrsCircuitBreaker: NrsConnector.NrsCircuitBreaker,
                              uuidGenerator: RandomUUIDGenerator
                            )(implicit ec: ExecutionContext) extends Logging {

  private val CORRELATION_ID_HEADER = "X-Correlation-Id"

  private def shouldTripCircuitBreaker: Try[HttpResponse] => Boolean = {
    case Success(response) => is5xx(response.status)
    case Failure(_) => true
  }

  def submitToNrs(payload: NrsPayload)(implicit hc: HeaderCarrier): Future[NrsSubmissionResult] = {
    // Get or generate correlation ID
    val correlationId = hc.headers(Seq(CORRELATION_ID_HEADER)).headOption match {
      case Some((_, existingId)) => existingId
      case None => 
        val newId = uuidGenerator.uuid
        logger.info(s"Generated correlation ID for NRS submission: $newId")
        newId
    }
    
    val vpdReference = payload.metadata.searchKeys.getOrElse("zvpd", "No VPD reference")

    logger.info(s"NRS submission: CorrelationId: $correlationId, VPD Reference: $vpdReference")

    val httpCall = httpClient
      .post(url"${appConfig.nrsSubmissionUrl}")(hc)
      .setHeader("X-API-KEY" -> appConfig.nrsApiKey)
      .setHeader("X-Correlation-Id" -> correlationId)
      .withBody(Json.toJson(payload))
      .execute[HttpResponse]

    nrsCircuitBreaker.breaker
      .withCircuitBreaker(httpCall, shouldTripCircuitBreaker)
      .map { response =>
        response.status match {
          case ACCEPTED =>
            logger.info(s"NRS submission successful for CorrelationId: $correlationId")
            NrsSubmissionResult.Success
          case status if is5xx(status) =>
            logger.warn(s"NRS submission failed with retryable 5xx status: $status for CorrelationId: $correlationId")
            NrsSubmissionResult.RetryableFailure
          case status =>
            logger.warn(s"NRS submission failed with permanent 4xx status: $status for CorrelationId: $correlationId")
            NrsSubmissionResult.PermanentFailure
        }
      }
      .recover { case e: Exception =>
        logger.error(s"NRS submission failed with error for CorrelationId: $correlationId: ${e.getMessage}", e)
        NrsSubmissionResult.RetryableFailure
      }
  }
}
