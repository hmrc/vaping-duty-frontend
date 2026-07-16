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

package controllers.returns.submit.adjustments

import controllers.actions.ApprovedVapingManufacturerAuthAction
import controllers.actions.returns.{ReturnsDataRequiredAction, ReturnsDataRetrievalAction, ReturnsEnabledAction}
import controllers.returns.PeriodKeyExtraction
import forms.returns.adjustments.RemoveAdjustmentFormProvider
import models.identifiers.PeriodKey
import models.returns.ReturnsConstants
import models.returns.ReturnsUserAnswers
import models.returns.adjustments.AdjustmentList
import pages.returns.adjustments.{AdjustmentListPage, DeclareAdjustmentPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.{AdjustmentCheckYourAnswersService, ReturnsUserAnswersService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.returns.submit.adjustments.RemoveAdjustmentView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class RemoveAdjustmentController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            sessionRepository: ReturnsUserAnswersService,
                                            identify: ApprovedVapingManufacturerAuthAction,
                                            getData: ReturnsDataRetrievalAction,
                                            requireData: ReturnsDataRequiredAction,
                                            formProvider: RemoveAdjustmentFormProvider,
                                            returnsEnabledAction: ReturnsEnabledAction,
                                            adjustmentCheckYourAnswersService: AdjustmentCheckYourAnswersService,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: RemoveAdjustmentView
                                          )(using ExecutionContext) extends FrontendBaseController with I18nSupport with PeriodKeyExtraction {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>
      withPeriodKey(ReturnsConstants.QUERY_PARAM_ADJUSTMENT_PERIOD) { adjustmentPeriod =>
        adjustmentCheckYourAnswersService
          .buildRemoveViewModel(request.userAnswers.get(AdjustmentListPage), adjustmentPeriod, request.enrolmentVpdId)
          .map {
            case Some(vm) => Ok(view(request.periodKey, adjustmentPeriod, form, vm))
            case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>
      withPeriodKey(ReturnsConstants.QUERY_PARAM_ADJUSTMENT_PERIOD) { adjustmentPeriod =>
        adjustmentCheckYourAnswersService
          .buildRemoveViewModel(request.userAnswers.get(AdjustmentListPage), adjustmentPeriod, request.enrolmentVpdId)
          .flatMap {
            case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            case Some(vm) =>
              form.bindFromRequest().fold(
                formWithErrors =>
                  Future.successful(BadRequest(view(request.periodKey, adjustmentPeriod, formWithErrors, vm))),

                confirmed =>
                  val resultAnswers =
                    if (confirmed) {
                      for {
                        updatedAnswers <- Future.fromTry(removeEntry(request.userAnswers, adjustmentPeriod))
                        _              <- sessionRepository.set(updatedAnswers)
                      } yield updatedAnswers
                    } else {
                      Future.successful(request.userAnswers)
                    }

                  resultAnswers.map { _ =>
                    Redirect(withPeriod(
                      controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad().url,
                      request.periodKey
                    ))
                  }
              )
          }
      }
  }

  private def removeEntry(userAnswers: ReturnsUserAnswers, adjustmentPeriod: PeriodKey): Try[ReturnsUserAnswers] = {
    val updatedAdjustments = userAnswers.get(AdjustmentListPage).map(_.adjustments).getOrElse(Seq.empty).filterNot(_.period == adjustmentPeriod)

    if (updatedAdjustments.isEmpty) userAnswers.set(DeclareAdjustmentPage, false)
    else userAnswers.set(AdjustmentListPage, AdjustmentList(updatedAdjustments))
  }

  private def withPeriod(url: String, periodKey: PeriodKey): String = s"$url?period=${periodKey.value}"
}
