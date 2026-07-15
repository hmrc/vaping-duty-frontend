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
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType, AdjustmentVolumeWithTypeFormData}
import navigation.ReturnsNavigator
import pages.returns.adjustments.AdjustmentListPage
import play.api.data.{Form, FormError}
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

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>

      withPeriodKey(ReturnsConstants.QUERY_PARAM_ADJUSTMENT_PERIOD) { adjustmentPeriodKey =>
        for {
          obligationDetails <- obligationService.getObligationsDirectly(request.enrolmentVpdId)
          form              <- formProvider(request.periodKey, request.enrolmentVpdId)
        } yield {
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
            val cleanedData = cleanFormData(rawData)

            form.bind(cleanedData).fold(
              formWithErrors => {
                val updatedForm = prepareFormForDisplay(formWithErrors)
                val vm = AdjustmentVolumeWithTypeViewModel(obligationDetails, adjustmentPeriodKey, returnsDateUtils)
                Future.successful(BadRequest(view(request.periodKey, adjustmentPeriodKey, updatedForm, mode, vm)))
              },

              formData => {
                val newEntry = AdjustmentEntry(
                  period = adjustmentPeriodKey,
                  adjustmentType = formData.adjustmentType,
                  volumeInMl = formData.getVolume
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
        } yield result
      }
  }

  private def cleanFormData(rawData: Map[String, Seq[String]]): Map[String, String] = {
    val flattenedData = rawData.view.mapValues(_.head).toMap
    flattenedData.get(ReturnsConstants.ADJUSTMENT_TYPE_FIELD) match {
      case Some(AdjustmentType.UnderDeclared.toString) =>
        flattenedData - ReturnsConstants.OVER_DECLARED_VOLUME_FIELD
      case Some(AdjustmentType.OverDeclared.toString) =>
        flattenedData - ReturnsConstants.UNDER_DECLARED_VOLUME_FIELD
      case _ => flattenedData
    }
  }

  private def prepareFormForDisplay(form: Form[AdjustmentVolumeWithTypeFormData]): Form[AdjustmentVolumeWithTypeFormData] = {
    val cleanedForm = clearNonSelectedField(form)
    moveFormLevelErrorsToField(cleanedForm, ReturnsConstants.ADJUSTMENT_TYPE_FIELD)
  }

  private def clearNonSelectedField(form: Form[AdjustmentVolumeWithTypeFormData]): Form[AdjustmentVolumeWithTypeFormData] = {
    // Get the adjustment type from the form data
    form.data.get(ReturnsConstants.ADJUSTMENT_TYPE_FIELD) match {
      case Some(AdjustmentType.UnderDeclared.toString) =>
        // Clear overDeclaredVolume from the form data and remove any errors for that field
        form.copy(
          data = form.data - ReturnsConstants.OVER_DECLARED_VOLUME_FIELD,
          errors = form.errors.filterNot(_.key == ReturnsConstants.OVER_DECLARED_VOLUME_FIELD)
        )
      case Some(AdjustmentType.OverDeclared.toString) =>
        // Clear underDeclaredVolume from the form data and remove any errors for that field
        form.copy(
          data = form.data - ReturnsConstants.UNDER_DECLARED_VOLUME_FIELD,
          errors = form.errors.filterNot(_.key == ReturnsConstants.UNDER_DECLARED_VOLUME_FIELD)
        )
      case _ =>
        // If no adjustment type is selected, return form as-is
        form
    }
  }

  private def moveFormLevelErrorsToField[T](form: Form[T], fieldName: String): Form[T] = {
    val formLevelErrors = form.errors.filter(_.key.isEmpty)
    if (formLevelErrors.nonEmpty) {
      val fieldErrors = formLevelErrors.map(e => FormError(fieldName, e.message, e.args))
      form.copy(errors = form.errors.filterNot(_.key.isEmpty) ++ fieldErrors)
    } else {
      form
    }
  }
}
