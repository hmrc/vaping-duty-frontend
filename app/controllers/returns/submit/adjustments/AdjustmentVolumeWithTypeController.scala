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
import forms.returns.adjustments.{AdjustmentVolumeWithTypeFormData, AdjustmentVolumeWithTypeFormProvider}
import models.Mode
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import navigation.ReturnsNavigator
import pages.returns.adjustments.AdjustmentListPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.{ObligationService, ReturnsUserAnswersService}
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
                                                    returnsDateUtils: ReturnsDateUtils,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    view: AdjustmentVolumeWithTypeView
                                                  )(using ExecutionContext) extends FrontendBaseController with I18nSupport with PeriodKeyExtraction {

  val form: Form[AdjustmentVolumeWithTypeFormData] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>

      withPeriodKey("adjustmentPeriod") { adjustmentPeriodKey =>
        obligationService.getObligations(request.enrolmentVpdId).map { obligations =>

          val existingAdjustment = request.userAnswers.get(AdjustmentListPage)
            .flatMap(_.adjustments.find(_.period == adjustmentPeriodKey))

          val preparedForm = existingAdjustment match {
            case Some(adjustment) =>
              val formData = AdjustmentVolumeWithTypeFormData(
                adjustmentType = adjustment.adjustmentType,
                underDeclaredVolume = if (adjustment.adjustmentType == AdjustmentType.UnderDeclared) Some(adjustment.volumeInMl) else None,
                overDeclaredVolume = if (adjustment.adjustmentType == AdjustmentType.OverDeclared) Some(adjustment.volumeInMl) else None
              )
              form.fill(formData)
            case None => form
          }

          val vm = AdjustmentVolumeWithTypeViewModel(obligations, adjustmentPeriodKey, returnsDateUtils)
          Ok(view(request.periodKey, adjustmentPeriodKey, preparedForm, mode, vm))
        }
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>

      withPeriodKey("adjustmentPeriod") { adjustmentPeriodKey =>
        obligationService.getObligations(request.enrolmentVpdId).flatMap { obligations =>
          form.bindFromRequest().fold(
            formWithErrors => {
              val vm = AdjustmentVolumeWithTypeViewModel(obligations, adjustmentPeriodKey, returnsDateUtils)
              Future.successful(BadRequest(view(request.periodKey, adjustmentPeriodKey, formWithErrors, mode, vm)))
            },

            formData => {
              val volume = formData.getVolume
              val newEntry = AdjustmentEntry(
                period = adjustmentPeriodKey,
                adjustmentType = formData.adjustmentType,
                volumeInMl = volume
              )

              val existingList = request.userAnswers.get(AdjustmentListPage).getOrElse(AdjustmentList.empty)

              val updatedAdjustments = existingList.adjustments.filterNot(_.period == adjustmentPeriodKey) :+ newEntry
              val updatedList = AdjustmentList(updatedAdjustments)

              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AdjustmentListPage, updatedList))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(AdjustmentListPage, mode, updatedAnswers))
            }
          )
        }
      }
  }
}