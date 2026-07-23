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

package services.returns

import com.google.inject.{Inject, Singleton}
import models.identifiers.PeriodKey
import models.obligations.ObligationDetails
import models.returns.adjustments.*
import models.returns.{ReturnsConstants, ReturnsUserAnswers}
import pages.returns.adjustments.{AdjustmentListPage, AdjustmentReasonPage}
import play.api.data.{Form, FormError}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdjustmentVolumeService @Inject()(
                                         dutyRateService: DutyRateService
                                       )(using ExecutionContext) {

  def cleanFormData(rawData: Map[String, Seq[String]]): Map[String, String] = {
    val flattenedData = rawData.view.mapValues(_.head).toMap
    flattenedData.get(ReturnsConstants.ADJUSTMENT_TYPE_FIELD) match {
      case Some(AdjustmentType.UnderDeclared.toString) =>
        flattenedData - ReturnsConstants.OVER_DECLARED_VOLUME_FIELD
      case Some(AdjustmentType.OverDeclared.toString) =>
        flattenedData - ReturnsConstants.UNDER_DECLARED_VOLUME_FIELD
      case _ => flattenedData
    }
  }

  private def clearNonSelectedField(form: Form[AdjustmentVolumeWithTypeFormData]): Form[AdjustmentVolumeWithTypeFormData] = {
    form.data.get(ReturnsConstants.ADJUSTMENT_TYPE_FIELD) match {
      case Some(AdjustmentType.UnderDeclared.toString) =>
        form.copy(
          data = form.data - ReturnsConstants.OVER_DECLARED_VOLUME_FIELD,
          errors = form.errors.filterNot(_.key == ReturnsConstants.OVER_DECLARED_VOLUME_FIELD)
        )
      case Some(AdjustmentType.OverDeclared.toString) =>
        form.copy(
          data = form.data - ReturnsConstants.UNDER_DECLARED_VOLUME_FIELD,
          errors = form.errors.filterNot(_.key == ReturnsConstants.UNDER_DECLARED_VOLUME_FIELD)
        )
      case _ =>
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

  def prepareFormForDisplay(form: Form[AdjustmentVolumeWithTypeFormData]): Form[AdjustmentVolumeWithTypeFormData] = {
    val cleanedForm = clearNonSelectedField(form)
    moveFormLevelErrorsToField(cleanedForm, ReturnsConstants.ADJUSTMENT_TYPE_FIELD)
  }

  def findExistingAdjustment(userAnswers: ReturnsUserAnswers, periodKey: PeriodKey): Option[AdjustmentEntry] = {
    userAnswers.get(AdjustmentListPage)
      .flatMap(_.adjustments.find(_.period == periodKey))
  }

  def buildFormData(adjustment: AdjustmentEntry): AdjustmentVolumeWithTypeFormData = {
    AdjustmentVolumeWithTypeFormData(
      adjustmentType = adjustment.adjustmentType,
      underDeclaredVolume = if (adjustment.adjustmentType == AdjustmentType.UnderDeclared) Some(adjustment.volumeInMl) else None,
      overDeclaredVolume = if (adjustment.adjustmentType == AdjustmentType.OverDeclared) Some(adjustment.volumeInMl) else None
    )
  }

  def prepareFormWithData(
                           form: Form[AdjustmentVolumeWithTypeFormData],
                           existingAdjustment: Option[AdjustmentEntry]
                         ): Form[AdjustmentVolumeWithTypeFormData] = {
    existingAdjustment match {
      case Some(adjustment) => form.fill(buildFormData(adjustment))
      case None => form
    }
  }

  def updateAdjustmentList(
                            userAnswers: ReturnsUserAnswers,
                            adjustmentPeriodKey: PeriodKey,
                            formData: AdjustmentVolumeWithTypeFormData,
                            obligationDetails: Seq[ObligationDetails]
                          )(using HeaderCarrier): Future[AdjustmentUpdateResult] = {
    val existingList = userAnswers.get(AdjustmentListPage).getOrElse(AdjustmentList.empty)

    val newEntry = AdjustmentEntry(
      period = adjustmentPeriodKey,
      adjustmentType = formData.adjustmentType,
      volumeInMl = formData.getVolume
    )

    val updatedAdjustments = existingList.adjustments.filterNot(_.period == adjustmentPeriodKey) :+ newEntry
    val updatedList = AdjustmentList(updatedAdjustments)

    val dutyRates = dutyRateService.getDutyRatesForPeriods(
      updatedList.adjustments.map(_.period).distinct,
      obligationDetails
    )

    val reasonRequiredBefore = AdjustmentDutyCalculator.totals(
      existingList.adjustments,
      dutyRates
    ).reasonMandatory

    val reasonRequiredAfter = AdjustmentDutyCalculator.totals(
      updatedList.adjustments,
      dutyRates
    ).reasonMandatory

    for {
      updatedAnswers <- Future.fromTry(userAnswers.set(AdjustmentListPage, updatedList))
      finalAnswers <- {
        if (!reasonRequiredAfter && updatedAnswers.get(AdjustmentReasonPage).isDefined) {
          Future.fromTry(updatedAnswers.remove(AdjustmentReasonPage))
        } else {
          Future.successful(updatedAnswers)
        }
      }
    } yield AdjustmentUpdateResult(finalAnswers, reasonRequiredBefore, reasonRequiredAfter)
  }
}
