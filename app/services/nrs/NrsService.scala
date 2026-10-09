/*
 * Copyright 2026 HM Revenue & Customs
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
import models.identifiers.{PeriodKey, VpdId}
import models.nrs.{Credentials, CredentialRole, IdentityData}
import models.returns.submit.ReturnCreateRequest
import play.api.Logging
import play.api.mvc.RequestHeader
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, ConfidenceLevel}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsService @Inject()(
  nrsConnector: NrsConnector,
  nrsPayloadBuilder: NrsPayloadBuilder,
  authConnector: AuthConnector
)(implicit ec: ExecutionContext) extends Logging {

  def submitNrsData(
    returnData: ReturnCreateRequest,
    vpdId: VpdId,
    periodKey: PeriodKey
  )(implicit hc: HeaderCarrier, request: RequestHeader): Future[Unit] = {
    
    (for {
      identityData <- retrieveIdentityData()
      authToken = extractAuthToken()
      headers = extractHeaders()
      workItem = nrsPayloadBuilder.buildWorkItem(
        returnData,
        vpdId,
        periodKey,
        identityData,
        authToken,
        headers
      )
      _ <- nrsConnector.submitNrsWorkItem(workItem)
    } yield ())
      .recover { case ex =>
        logger.warn(s"Failed to submit NRS work item for VPD ID: ${vpdId.value}, Period: ${periodKey.value}: ${ex.getMessage}", ex)
        // Soft failure - don't propagate error
        ()
      }
  }

  private def retrieveIdentityData()(implicit hc: HeaderCarrier): Future[IdentityData] = {
    authConnector.authorise(
      uk.gov.hmrc.auth.core.AuthProviders(uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway),
      internalId and 
      credentials and 
      confidenceLevel and 
      groupIdentifier and 
      credentialRole and 
      affinityGroup and 
      credentialStrength
    ).map {
      case optInternalId ~ optCreds ~ confLevel ~ optGroupId ~ optCredRole ~ optAffinity ~ optCredStrength =>
        IdentityData(
          internalId = optInternalId,
          optionalCredentials = optCreds.map(c => Credentials(c.providerId, c.providerType)),
          confidenceLevel = confLevel,
          groupIdentifier = optGroupId,
          credentialRole = optCredRole.map(mapCredentialRole),
          affinityGroup = optAffinity,
          credentialStrength = optCredStrength
        )
    }
  }

  private def mapCredentialRole(role: uk.gov.hmrc.auth.core.CredentialRole): CredentialRole = {
    role match {
      case uk.gov.hmrc.auth.core.Admin => CredentialRole.Admin
      case uk.gov.hmrc.auth.core.Assistant => CredentialRole.Assistant
      case uk.gov.hmrc.auth.core.User => CredentialRole.User
    }
  }

  private def extractAuthToken()(implicit hc: HeaderCarrier): String = {
    hc.authorization.map(_.value).getOrElse("")
  }

  private def extractHeaders()(implicit request: RequestHeader): Map[String, String] = {
    request.headers.toSimpleMap
  }
}