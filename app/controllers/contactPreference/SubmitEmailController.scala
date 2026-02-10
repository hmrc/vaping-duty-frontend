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
import models.emailverification.{PaperlessPreferenceSubmission, VerificationDetails}
import models.requests.DataRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmailVerificationService
import uk.gov.hmrc.http.HeaderCarrier
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
                                       view: SubmitEmailView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      Ok(view())
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val email = request.userAnswers.emailAddress.getOrElse("")

      // Checking new email is now verified
      emailVerificationService.retrieveAddressStatus(
        VerificationDetails(request.credId),
        email,
        request.userAnswers
      ).value.flatMap {
        case Left(error) =>
          logger.info("[EnterEmailController][onSubmit] Error retrieving email verification status with status: " +
            s"${error.status} and message: ${error.message}")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Right(response) =>
          submitPreferences(response.emailAddress, response.isVerified)
      }
  }

  private def submitPreferences(email: String, verified: Boolean)
                               (implicit hc: HeaderCarrier, request: DataRequest[_]) = {

    if (verified) {
      val preferenceSubmission = PaperlessPreferenceSubmission(true, Some(email), Some(verified), None)

      submitPreferencesConnector.submitContactPreferences(preferenceSubmission, request.vpdId).map {
        case Left(error) =>
          logger.info("[EnterEmailController][submitPreferences] Error submitting contact preference with status: " +
            s"${error.status} and message: ${error.message}")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Right(response) =>
          logger.info(s"[EnterEmailController][submitPreferences] Email preference updated ${response.processingDate}")
          Redirect(controllers.contactPreference.routes.EmailConfirmationController.onPageLoad())
      }
    } else {
      // Should never enter this case
      logger.warn("[EmailConfirmationController][submitPreferences] Unverified email attempted to submit")
      Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
