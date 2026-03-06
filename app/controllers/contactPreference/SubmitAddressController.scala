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
import models.contactPreference
import models.contactPreference.PaperlessPreference.{Email, Post, toValue}
import models.contactPreference.{Failure, PaperlessPreference, PerformSubmission, ResponseStatus, Success}
import models.emailverification.PaperlessPreferenceSubmission
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AuditService, UserAnswersService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.contactPreference.SubmitAddressViewModel
import views.html.contactPreference.SubmitAddressView

import javax.inject.Inject
import scala.concurrent.impl.Promise
import scala.concurrent.{ExecutionContext, Future}

class SubmitAddressController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          identify: ApprovedVapingManufacturerAuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          submitPreferencesConnector: SubmitPreferencesConnector,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: SubmitAddressView,
                                          config: FrontendAppConfig,
                                          auditService: AuditService
                                        )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val vm = SubmitAddressViewModel(config, request.userAnswers.subscriptionSummary.correspondenceAddress)

    Ok(view(vm))
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val currentPreference = PaperlessPreference(request.userAnswers.subscriptionSummary.paperlessPreference)

    currentPreference match {
      case Email =>
        PerformSubmission(
          submitPreferencesConnector,
          PaperlessPreferenceSubmission(
            paperlessPreference = toValue(Post),
            emailAddress = request.userAnswers.subscriptionSummary.emailAddress,
            emailVerification = request.userAnswers.subscriptionSummary.emailVerification,
            bouncedEmail = request.userAnswers.subscriptionSummary.bouncedEmail
          ),
          auditService
        ).map {
          case _: Failure => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case _: Success => Redirect(controllers.contactPreference.routes.ConfirmationController.onPageLoad())
        }
      case Post  =>
        Future.successful(Redirect(controllers.contactPreference.routes.ChangeAddressController.onPageLoad()))
    }
  }
}
