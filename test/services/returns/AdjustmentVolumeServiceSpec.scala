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

import base.SpecBase
import forms.mappings.Mappings
import models.identifiers.PeriodKey
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType, AdjustmentVolumeWithTypeFormData}
import models.returns.{DutyRate, ReturnsConstants}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.adjustments.{AdjustmentListPage, AdjustmentReasonPage}
import play.api.data.Form
import play.api.data.Forms.{bigDecimal, mapping, optional}

class AdjustmentVolumeServiceSpec extends SpecBase with MockitoSugar with Mappings {

  val mockSessionRepository: ReturnsUserAnswersService = mock[ReturnsUserAnswersService]

  val service = new AdjustmentVolumeService(mockDutyRateService)

  val adjustmentPeriodKey: PeriodKey = october2027

  def createTestForm: Form[AdjustmentVolumeWithTypeFormData] = Form(
    mapping(
      "adjustmentType" -> enumerable[AdjustmentType]("error.required"),
      "underDeclaredVolume" -> optional(bigDecimal),
      "overDeclaredVolume" -> optional(bigDecimal)
    )((adjustmentType, underDeclaredVolume, overDeclaredVolume) =>
      AdjustmentVolumeWithTypeFormData(adjustmentType, underDeclaredVolume, overDeclaredVolume)
    )(data =>
      Some((data.adjustmentType, data.underDeclaredVolume, data.overDeclaredVolume))
    )
  )

  "cleanFormData" - {
    "must remove overDeclaredVolume when adjustmentType is underDeclared" in {
      val rawData = Map(
        ReturnsConstants.ADJUSTMENT_TYPE_FIELD -> Seq("underDeclared"),
        ReturnsConstants.UNDER_DECLARED_VOLUME_FIELD -> Seq("100.5"),
        ReturnsConstants.OVER_DECLARED_VOLUME_FIELD -> Seq("200.7")
      )

      val result = service.cleanFormData(rawData)

      result must contain(ReturnsConstants.ADJUSTMENT_TYPE_FIELD -> "underDeclared")
      result must contain(ReturnsConstants.UNDER_DECLARED_VOLUME_FIELD -> "100.5")
      result must not contain key(ReturnsConstants.OVER_DECLARED_VOLUME_FIELD)
    }

    "must remove underDeclaredVolume when adjustmentType is overDeclared" in {
      val rawData = Map(
        ReturnsConstants.ADJUSTMENT_TYPE_FIELD -> Seq("overDeclared"),
        ReturnsConstants.UNDER_DECLARED_VOLUME_FIELD -> Seq("100.5"),
        ReturnsConstants.OVER_DECLARED_VOLUME_FIELD -> Seq("200.7")
      )

      val result = service.cleanFormData(rawData)

      result must contain(ReturnsConstants.ADJUSTMENT_TYPE_FIELD -> "overDeclared")
      result must contain(ReturnsConstants.OVER_DECLARED_VOLUME_FIELD -> "200.7")
      result must not contain key(ReturnsConstants.UNDER_DECLARED_VOLUME_FIELD)
    }

    "must return all fields when adjustmentType is not present" in {
      val rawData = Map(
        ReturnsConstants.UNDER_DECLARED_VOLUME_FIELD -> Seq("100.5"),
        ReturnsConstants.OVER_DECLARED_VOLUME_FIELD -> Seq("200.7")
      )

      val result = service.cleanFormData(rawData)

      result must contain(ReturnsConstants.UNDER_DECLARED_VOLUME_FIELD -> "100.5")
      result must contain(ReturnsConstants.OVER_DECLARED_VOLUME_FIELD -> "200.7")
    }
  }

  "findExistingAdjustment" - {
    "must return adjustment when it exists for the period" in {
      val adjustment = AdjustmentEntry(
        period = adjustmentPeriodKey,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal("100.5")
      )
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, AdjustmentList(Seq(adjustment))).success.value

      val result = service.findExistingAdjustment(userAnswers, adjustmentPeriodKey)

      result mustBe Some(adjustment)
    }

    "must return None when no adjustment exists for the period" in {
      val result = service.findExistingAdjustment(returnsUserAnswers, adjustmentPeriodKey)

      result mustBe None
    }
  }

  "buildFormData" - {
    "must build form data for underDeclared adjustment" in {
      val adjustment = AdjustmentEntry(
        period = adjustmentPeriodKey,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal("100.5")
      )

      val result = service.buildFormData(adjustment)

      result.adjustmentType mustBe AdjustmentType.UnderDeclared
      result.underDeclaredVolume mustBe Some(BigDecimal("100.5"))
      result.overDeclaredVolume mustBe None
    }

    "must build form data for overDeclared adjustment" in {
      val adjustment = AdjustmentEntry(
        period = adjustmentPeriodKey,
        adjustmentType = AdjustmentType.OverDeclared,
        volumeInMl = BigDecimal("200.7")
      )

      val result = service.buildFormData(adjustment)

      result.adjustmentType mustBe AdjustmentType.OverDeclared
      result.underDeclaredVolume mustBe None
      result.overDeclaredVolume mustBe Some(BigDecimal("200.7"))
    }
  }

  "prepareFormWithData" - {
    "must fill form with existing adjustment data" in {
      val form = createTestForm
      val adjustment = AdjustmentEntry(
        period = adjustmentPeriodKey,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal("100.5")
      )

      val result = service.prepareFormWithData(form, Some(adjustment))

      result.value.value.adjustmentType mustBe AdjustmentType.UnderDeclared
      result.value.value.underDeclaredVolume mustBe Some(BigDecimal("100.5"))
    }

    "must return empty form when no existing adjustment" in {
      val form = createTestForm

      val result = service.prepareFormWithData(form, None)

      result.value mustBe None
    }
  }

  "updateAdjustmentList" - {
    val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

    "must add new adjustment to empty list" in {
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(adjustmentPeriodKey -> testDutyRate))

      val formData = AdjustmentVolumeWithTypeFormData(
        adjustmentType = AdjustmentType.UnderDeclared,
        underDeclaredVolume = Some(BigDecimal("100.5")),
        overDeclaredVolume = None
      )

      val result = service.updateAdjustmentList(
        returnsUserAnswers,
        adjustmentPeriodKey,
        formData,
        obligationDetails
      ).futureValue

      result.userAnswers.get(AdjustmentListPage).value.adjustments must have size 1
      result.userAnswers.get(AdjustmentListPage).value.adjustments.head.period mustBe adjustmentPeriodKey
      result.userAnswers.get(AdjustmentListPage).value.adjustments.head.volumeInMl mustBe BigDecimal("100.5")
    }

    "must replace existing adjustment for the same period" in {
      val existingAdjustment = AdjustmentEntry(
        period = adjustmentPeriodKey,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal("50")
      )
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, AdjustmentList(Seq(existingAdjustment))).success.value

      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(adjustmentPeriodKey -> testDutyRate))

      val formData = AdjustmentVolumeWithTypeFormData(
        adjustmentType = AdjustmentType.OverDeclared,
        underDeclaredVolume = None,
        overDeclaredVolume = Some(BigDecimal("200.7"))
      )

      val result = service.updateAdjustmentList(
        userAnswers,
        adjustmentPeriodKey,
        formData,
        obligationDetails
      ).futureValue

      result.userAnswers.get(AdjustmentListPage).value.adjustments must have size 1
      result.userAnswers.get(AdjustmentListPage).value.adjustments.head.adjustmentType mustBe AdjustmentType.OverDeclared
      result.userAnswers.get(AdjustmentListPage).value.adjustments.head.volumeInMl mustBe BigDecimal("200.7")
    }

    "must remove reason when duty drops below threshold" in {
      val existingAdjustment = AdjustmentEntry(
        period = adjustmentPeriodKey,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal(5000)
      )
      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(existingAdjustment))).success.value
        .set(AdjustmentReasonPage, "existing reason").success.value

      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(adjustmentPeriodKey -> testDutyRate))

      val formData = AdjustmentVolumeWithTypeFormData(
        adjustmentType = AdjustmentType.UnderDeclared,
        underDeclaredVolume = Some(BigDecimal("10")),
        overDeclaredVolume = None
      )

      val result = service.updateAdjustmentList(
        userAnswers,
        adjustmentPeriodKey,
        formData,
        obligationDetails
      ).futureValue

      result.userAnswers.get(AdjustmentReasonPage) mustBe None
    }
  }
}
