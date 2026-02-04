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

import config.FrontendAppConfig
import connectors.SubmitPreferencesConnector
import controllers.actions.*
import models.BtaLink
import models.emailverification.PaperlessPreferenceSubmission
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.contactPreference.PostalConfirmationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PostalConfirmationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: ApprovedVapingManufacturerAuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: PostalConfirmationView,
                                       submitPreferencesConnector: SubmitPreferencesConnector,
                                       config: FrontendAppConfig
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging with BtaLink {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {

    implicit request =>




      val email = request.userAnswers.subscriptionSummary.emailAddress
      val verification = request.userAnswers.subscriptionSummary.emailVerification
      val bounced = request.userAnswers.subscriptionSummary.bouncedEmail

      val preferenceSubmission = PaperlessPreferenceSubmission(false, email, verification, bounced)

      if (request.userAnswers.subscriptionSummary.paperlessPreference) {
        // This submission needs to be on a user action, not page load.  Added here for testing, iterations will change this behaviour.
        submitPreferencesConnector.submitContactPreferences(preferenceSubmission, request.vpdId).map {
          case Left(error) =>
            logger.info(s"[PostalConfirmationController][onPageLoad] Error submitting preference: $error")
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case Right(response) =>
            Ok(view(btaLink))
        }
      } else {
        // Still render the confirmation page with no submission as current flow
        Future.successful(Ok(view(btaLink)))
      }
  }

  override def btaLink: String = config.continueToBta
}
