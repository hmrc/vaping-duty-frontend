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

package controllers.returns.submit.spoilt

import controllers.actions.ApprovedVapingManufacturerAuthAction
import controllers.actions.returns.{ReturnsDataRequiredAction, ReturnsDataRetrievalAction, ReturnsEnabledAction}
import controllers.returns.PeriodKeyExtraction
import forms.returns.RemoveSpoiltAdjustmentFormProvider
import models.identifiers.PeriodKey
import pages.returns.SpoiltVolumeByPeriodPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.SpoiltCheckYourAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.returns.submit.spoilt.RemoveSpoiltAdjustmentView
import models.returns.ReturnsConstants

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class RemoveSpoiltAdjustmentController @Inject()(
                                                  override val messagesApi: MessagesApi,
                                                  identify: ApprovedVapingManufacturerAuthAction,
                                                  getData: ReturnsDataRetrievalAction,
                                                  requireData: ReturnsDataRequiredAction,
                                                  formProvider: RemoveSpoiltAdjustmentFormProvider,
                                                  returnsEnabledAction: ReturnsEnabledAction,
                                                  spoiltCheckYourAnswersService: SpoiltCheckYourAnswersService,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  view: RemoveSpoiltAdjustmentView
                                                )(using ExecutionContext) extends FrontendBaseController with I18nSupport with PeriodKeyExtraction {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>
      withPeriodKey(ReturnsConstants.QUERY_PARAM_SPOILT_PERIOD) { spoiltPeriod =>
        spoiltCheckYourAnswersService
          .buildRemoveViewModel(request.userAnswers.get(SpoiltVolumeByPeriodPage), spoiltPeriod, request.enrolmentVpdId)
          .map {
            case Some(vm) => Ok(view(request.periodKey, spoiltPeriod, form, vm))
            case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>
      withPeriodKey(ReturnsConstants.QUERY_PARAM_SPOILT_PERIOD) { spoiltPeriod =>
        form.bindFromRequest().fold(
          formWithErrors =>
            spoiltCheckYourAnswersService
              .buildRemoveViewModel(request.userAnswers.get(SpoiltVolumeByPeriodPage), spoiltPeriod, request.enrolmentVpdId)
              .map {
                case Some(vm) => BadRequest(view(request.periodKey, spoiltPeriod, formWithErrors, vm))
                case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              },

          confirmed =>
            spoiltCheckYourAnswersService
              .handleRemoval(request.userAnswers, spoiltPeriod, confirmed)
              .map { _ =>
                Redirect(withPeriod(
                  controllers.returns.submit.spoilt.routes.SpoiltCheckYourAnswersController.onPageLoad().url,
                  request.periodKey
                ))
              }
        )
      }
  }

  private def withPeriod(url: String, periodKey: PeriodKey): String = s"$url?period=${periodKey.value}"
}
