/*
 * Copyright 2025 HM Revenue & Customs
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
import models.requests.NoEnrolmentIdentifierRequest
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.CredentialStrength.strong
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

/**
 * Authentication action to check the user is an approved manufacturer
 * This requires authenticated GG user with
 *    Credential Strength: Strong
 *    Affinity Group: Organisation
 *    Confidence Level: at least 50 (the lowest level - the only value for Organisation logins)
 *    Enrolment: VPD enrolment showing an approved vaping manufacturer
 */

trait NoEnrolmentAuthAction
  extends ActionBuilder[NoEnrolmentIdentifierRequest, AnyContent]
    with ActionFunction[Request, NoEnrolmentIdentifierRequest]

class NoEnrolmentAuthActionImpl @Inject()(override val authConnector: AuthConnector,
                                                         config: FrontendAppConfig,
                                                         val parser: BodyParsers.Default)
                                                        (implicit val executionContext: ExecutionContext)
  extends NoEnrolmentAuthAction
    with AuthorisedFunctions
    with Logging {

  private def predicate: Predicate =
    AuthProviders(GovernmentGateway) and
      CredentialStrength(strong) and
      Organisation and
      ConfidenceLevel.L50

  override def invokeBlock[A](request: Request[A], block: NoEnrolmentIdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(predicate).retrieve(internalId and groupIdentifier and allEnrolments) {
      case optInternalId ~ optGroupId ~ enrolments =>
        val identifiers = for {
          internalId <- optInternalId.toRight("Unable to retrieve internalId")
          groupId <- optGroupId.toRight("Unable to retrieve groupIdentifier")
          optApprovalId = getApprovalId(enrolments)
        } yield {
          (internalId, groupId, optApprovalId)
        }

        identifiers match {
          case Right((internalId, groupId, optApprovalId)) => block(NoEnrolmentIdentifierRequest(request, optApprovalId, groupId, internalId))
          case Left(error) => Future.failed(AuthorisationException.fromString(error))
        }

    } recover {
      case e: AuthorisationException =>
        logger.debug("Got AuthorisationException:", e)
        handleAuthException(e)
    }
  }

  private def handleAuthException: PartialFunction[AuthorisationException, Result] = {
    case _: NoActiveSession => Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
    case _                  => Redirect(routes.UnauthorisedController.onPageLoad())
  }

  private def getApprovalId(enrolments: Enrolments): Option[String] =
    enrolments
      .enrolments
      .find(_.key == config.enrolmentServiceName)
      .flatMap(_.getIdentifier(config.enrolmentIdentifierKey))
      .map(_.value)
}
