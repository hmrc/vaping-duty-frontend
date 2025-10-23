/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.actions

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.mvc.Results.*
import play.api.mvc.*
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.CredentialStrength.strong
import uk.gov.hmrc.auth.core.{ConfidenceLevel, CredentialStrength, Enrolment, *}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait IdentifyAction
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]

class IdentifyActionImpl @Inject() (
  override val authConnector: AuthConnector,
  config: FrontendAppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifyAction
    with AuthorisedFunctions
    with Logging {

  private def predicate: Predicate =
    AuthProviders(GovernmentGateway) and
      Enrolment(config.enrolmentServiceName) and
      CredentialStrength(strong) and
      Organisation and
      ConfidenceLevel.L50

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(predicate).retrieve(internalId and groupIdentifier and allEnrolments) {
      case optInternalId ~ optGroupId ~ enrolments =>
      val identifiers = for {
          internalId <- optInternalId.toRight("Unable to retrieve internalId")
          groupId <- optGroupId.toRight("Unable to retrieve groupIdentifier")
          approvalId <- getApprovalId(enrolments)
        } yield {
          (internalId, groupId, approvalId)
        }

        identifiers match
          case Right((internalId, groupId, approvalId)) => block(IdentifierRequest(request, approvalId, groupId, internalId))
          case Left(error) => throw AuthorisationException.fromString(error)
          
    } recover {
      case e: AuthorisationException =>
        logger.debug(s"Got AuthorisationException:", e)
        handleAuthException(e)
    }
  }

  private def handleAuthException: PartialFunction[AuthorisationException, Result] = {
    case _: NoActiveSession => Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
    case _                  => Redirect(routes.UnauthorisedController.onPageLoad())
  }

  private def getApprovalId(enrolments: Enrolments): Either[String, String] =
    enrolments
      .enrolments
      .find(_.key == config.enrolmentServiceName)
      .flatMap(_.getIdentifier(config.enrolmentIdentifierKey))
      .map(_.value)
      .toRight(s"Unable to retrieve ${config.enrolmentIdentifierKey} from enrolments")
}
