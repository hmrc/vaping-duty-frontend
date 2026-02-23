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

package controllers.contactPreference

import connectors.SubmitPreferencesConnector
import controllers.actions.*
import models.emailverification.{EmailVerificationDetails, VerificationDetails}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditService, EmailVerificationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.contactPreference.SubmitEmailView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitEmailController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: ApprovedVapingManufacturerAuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       emailVerificationService: EmailVerificationService,
                                       submitPreferencesConnector: SubmitPreferencesConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: SubmitEmailView,
                                       auditService: AuditService
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val email = request.userAnswers.emailAddress.getOrElse("")

      emailVerificationService.retrieveAddressStatus(
        VerificationDetails(request.credId),
        email,
        request.userAnswers
      ).value.flatMap {
        case Left(error) =>
          logger.info("[SubmitEmailController][onPageLoad] Error retrieving email verification status with status: " +
            s"${error.status} and message: ${error.message}")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Right(verificationDetails) =>
          emailVerificationService.redirectIfLocked(
            Future.successful(Ok(view(verificationDetails.emailAddress))),
            verificationDetails.isLocked
          )
      }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val email = request.userAnswers.emailAddress.getOrElse("")

      emailVerificationService.retrieveAddressStatus(
        VerificationDetails(request.credId),
        email,
        request.userAnswers
      ).value.flatMap {
        case Left(error) =>
          logger.info("[SubmitEmailController][onSubmit] Error retrieving email verification status with status: " +
            s"${error.status} and message: ${error.message}")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Right(emailVerificationDetails) =>
            emailVerificationService.submitVerifiedEmail(
              emailVerificationDetails.emailAddress,
              emailVerificationDetails.isVerified,
              submitPreferencesConnector,
              auditService
            )
      }
  }
}
