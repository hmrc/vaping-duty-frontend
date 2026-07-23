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
import forms.returns.adjustments.AdjustmentVolumeWithTypeFormProvider
import models.Mode
import models.returns.ReturnsConstants
import navigation.ReturnsNavigator
import pages.returns.adjustments.AdjustmentListPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.{AdjustmentVolumeService, ObligationService, ReturnsUserAnswersService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ReturnsDateUtils
import viewmodels.returns.submit.adjustments.AdjustmentVolumeWithTypeViewModel
import views.html.returns.submit.adjustments.AdjustmentVolumeWithTypeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AdjustmentVolumeWithTypeController @Inject()(
                                                    override val messagesApi: MessagesApi,
                                                    sessionRepository: ReturnsUserAnswersService,
                                                    navigator: ReturnsNavigator,
                                                    identify: ApprovedVapingManufacturerAuthAction,
                                                    getData: ReturnsDataRetrievalAction,
                                                    requireData: ReturnsDataRequiredAction,
                                                    formProvider: AdjustmentVolumeWithTypeFormProvider,
                                                    returnsEnabledAction: ReturnsEnabledAction,
                                                    obligationService: ObligationService,
                                                    adjustmentVolumeService: AdjustmentVolumeService,
                                                    returnsDateUtils: ReturnsDateUtils,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    view: AdjustmentVolumeWithTypeView
                                                  )(using ExecutionContext) extends FrontendBaseController with I18nSupport with PeriodKeyExtraction {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>

      withPeriodKey(ReturnsConstants.QUERY_PARAM_ADJUSTMENT_PERIOD) { adjustmentPeriodKey =>
        for {
          obligationDetails <- obligationService.getObligationsDirectly(request.enrolmentVpdId)
          form              <- formProvider(request.periodKey, request.enrolmentVpdId)
        } yield {
          val existingAdjustment = adjustmentVolumeService.findExistingAdjustment(
            request.userAnswers,
            adjustmentPeriodKey
          )
          val preparedForm = adjustmentVolumeService.prepareFormWithData(form, existingAdjustment)
          val vm = AdjustmentVolumeWithTypeViewModel(obligationDetails, adjustmentPeriodKey, returnsDateUtils)
          Ok(view(request.periodKey, adjustmentPeriodKey, preparedForm, mode, vm))
        }
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>

      withPeriodKey(ReturnsConstants.QUERY_PARAM_ADJUSTMENT_PERIOD) { adjustmentPeriodKey =>
        for {
          obligationDetails <- obligationService.getObligationsDirectly(request.enrolmentVpdId)
          form              <- formProvider(request.periodKey, request.enrolmentVpdId)
          result            <- {
            val rawData = request.body.asFormUrlEncoded.getOrElse(Map.empty)
            val cleanedData = adjustmentVolumeService.cleanFormData(rawData)

            form.bind(cleanedData).fold(
              formWithErrors => {
                val updatedForm = adjustmentVolumeService.prepareFormForDisplay(formWithErrors)
                val vm = AdjustmentVolumeWithTypeViewModel(obligationDetails, adjustmentPeriodKey, returnsDateUtils)
                Future.successful(BadRequest(view(request.periodKey, adjustmentPeriodKey, updatedForm, mode, vm)))
              },

              formData => {
                for {
                  updateResult <- adjustmentVolumeService.updateAdjustmentList(
                    request.userAnswers,
                    adjustmentPeriodKey,
                    formData,
                    obligationDetails
                  )
                  _ <- sessionRepository.set(updateResult.userAnswers)
                } yield {
                  Redirect(navigator.nextPage(AdjustmentListPage, mode, updateResult.userAnswers))
                }
              }
            )
          }
        } yield result
      }
  }
}
