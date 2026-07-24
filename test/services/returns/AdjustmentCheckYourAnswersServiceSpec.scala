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
import builders.ObligationsBuilders
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.ObligationDetails
import models.returns.DutyRate
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers.*
import utils.ReturnsDateUtils

import scala.concurrent.Future

class AdjustmentCheckYourAnswersServiceSpec extends SpecBase with MockitoSugar with ObligationsBuilders {

  private val mockObligationService: ObligationService = mock[ObligationService]
  private val mockDutyRateService: DutyRateService = mock[DutyRateService]
  private val mockReturnsDateUtils: ReturnsDateUtils = mock[ReturnsDateUtils]
  private val mockSessionRepository: ReturnsUserAnswersService = mock[ReturnsUserAnswersService]

  private val service = new AdjustmentCheckYourAnswersService(
    mockObligationService,
    mockDutyRateService,
    mockReturnsDateUtils,
    mockSessionRepository
  )

  private val vpdId = VpdId("XMVPD0000000123")
  private val periodKey = PeriodKey("24AA")
  private val adjustmentPeriodKey = PeriodKey("24AB")

  private val TEN_POUNDS_PER_10ML = DutyRate(1000)

  implicit val messages: Messages = stubMessages()

  "buildViewModel" - {

    "must successfully build view model with adjustments" in {
      val adjustmentEntry = AdjustmentEntry(
        period = adjustmentPeriodKey,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal(1000)
      )
      val adjustmentList = AdjustmentList(Seq(adjustmentEntry))

      val obligationForAdjustment = openObligation(adjustmentPeriodKey)

      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq(obligationForAdjustment)))
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(adjustmentPeriodKey -> TEN_POUNDS_PER_10ML))

      val result = service.buildViewModel(
        declareAdjustment = Some(true),
        adjustmentList = Some(AdjustmentList(Seq(
          underDeclaredAdjustment(february2024, ml(1000))
        ))),
        periodKey = january2024,
        vpdId = vpdId
      ).futureValue

      result.hasAdjustments mustBe true
      result.summaryCards.size mustBe 1
      result.totalAdjustment mustBe BigDecimal(1000.00)
    }

    "must successfully build view model with no adjustments when declareAdjustment is false" in {
      stubObligations(Seq(openObligation(january2024)))

      val result = service.buildViewModel(
        declareAdjustment = Some(false),
        adjustmentList = None,
        periodKey = periodKey,
        vpdId = vpdId
      ).futureValue

      result.hasAdjustments mustBe false
      result.summaryCards.size mustBe 1
      result.totalAdjustment mustBe BigDecimal(0)
    }

    "must correctly calculate duty rates for multiple periods" in {
      val adjustmentPeriodKey2 = PeriodKey("24AC")
      
      val obligationForAdjustment1 = openObligation(adjustmentPeriodKey)
      val obligationForAdjustment2 = openObligation(adjustmentPeriodKey2)

      val adjustmentEntry1 = AdjustmentEntry(
        period = adjustmentPeriodKey,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal(1000)
      )
      val adjustmentEntry2 = AdjustmentEntry(
        period = adjustmentPeriodKey2,
        adjustmentType = AdjustmentType.OverDeclared,
        volumeInMl = BigDecimal(500)
      )
      val adjustmentList = AdjustmentList(Seq(adjustmentEntry1, adjustmentEntry2))

      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq(obligationForAdjustment1, obligationForAdjustment2)))
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(
          adjustmentPeriodKey -> TEN_POUNDS_PER_10ML,
          adjustmentPeriodKey2 -> TEN_POUNDS_PER_10ML
        ))

      val result = service.buildViewModel(
        declareAdjustment = Some(true),
        adjustmentList = Some(AdjustmentList(Seq(
          underDeclaredAdjustment(february2024, ml(1000)),
          overDeclaredAdjustment(march2024, ml(500))
        ))),
        periodKey = january2024,
        vpdId = vpdId
      ).futureValue

      result.hasAdjustments mustBe true
      result.summaryCards.size mustBe 2
      result.totalAdjustment mustBe BigDecimal(500.00) // 1000 - 500
    }

    "must handle empty adjustment list" in {
      stubObligations(Seq(openObligation(january2024)))

      val result = service.buildViewModel(
        declareAdjustment = Some(true),
        adjustmentList = Some(AdjustmentList(Seq.empty)),
        periodKey = january2024,
        vpdId = vpdId
      ).futureValue

      result.hasAdjustments mustBe false
      result.summaryCards.size mustBe 0
      result.totalAdjustment mustBe BigDecimal(0)
    }

    "must throw RuntimeException when obligation not found for adjustment period" in {
      val nonExistentPeriodKey = PeriodKey("24ZZ")
      val adjustmentEntry = AdjustmentEntry(
        period = nonExistentPeriodKey,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal(1000)
      )
      val adjustmentList = AdjustmentList(Seq(adjustmentEntry))

      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenThrow(new RuntimeException("No obligation found for period 24ZZ"))

      val exception = intercept[RuntimeException] {
        service.buildViewModel(
          declareAdjustment = Some(true),
          adjustmentList = Some(adjustmentList),
          periodKey = periodKey,
          vpdId = vpdId
        ).futureValue
      }

      exception.getMessage must include("No obligation found for period 24ZZ")
    }
  }

  "buildRemoveViewModel" - {

    "must return Some with the details rows when the entry and its obligation are found" in {
      stubObligations(Seq(openObligation(february2024)))
      stubDutyRate(TEN_POUNDS_PER_10ML)

      val result = service.buildRemoveViewModel(
        adjustmentList = Some(AdjustmentList(Seq(
          overDeclaredAdjustment(february2024, ml(1000))
        ))),
        adjustmentPeriod = february2024,
        vpdId = vpdId
      ).futureValue

      result.value.rows.size mustBe 4
    }

    "must return None when the entry is not in the adjustment list" in {
      stubObligations(Seq(openObligation(january2024)))

      val result = service.buildRemoveViewModel(
        adjustmentList = Some(AdjustmentList(Seq.empty)),
        adjustmentPeriod = february2024,
        vpdId = vpdId
      ).futureValue

      result mustBe None
    }

    "must return None when no obligation exists for the entry's period" in {
      stubObligations(Seq.empty)

      val result = service.buildRemoveViewModel(
        adjustmentList = Some(AdjustmentList(Seq(underDeclaredAdjustment(february2024, ml(1000))))),
        adjustmentPeriod = february2024,
        vpdId = vpdId
      ).futureValue

      result mustBe None
    }
  }

  private def stubObligations(allObligations: Seq[ObligationDetails]): Unit =
    when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
      .thenReturn(Future.successful(allObligations))

  private def stubDutyRate(dutyRate: DutyRate): Unit =
    when(mockDutyRateService.getDutyRateForDate(any()))
      .thenReturn(dutyRate)

  private def overDeclaredAdjustment(period: PeriodKey, volumeInMl: BigDecimal): AdjustmentEntry =
    AdjustmentEntry(
      period = period,
      adjustmentType = AdjustmentType.OverDeclared,
      volumeInMl = volumeInMl
    )

  private def underDeclaredAdjustment(period: PeriodKey, volumeInMl: BigDecimal): AdjustmentEntry =
    AdjustmentEntry(
      period = period,
      adjustmentType = AdjustmentType.UnderDeclared,
      volumeInMl = volumeInMl
    )

  private def ml(volume: Int): BigDecimal = BigDecimal(volume)

  "isReasonMandatory" - {

    "must return true when under-declaration duty exceeds threshold" in {
      val adjustmentList = AdjustmentList(Seq(
        underDeclaredAdjustment(february2024, ml(50000))
      ))
      val userAnswers = returnsUserAnswers.set(pages.returns.adjustments.AdjustmentListPage, adjustmentList).success.value

      stubObligations(Seq(openObligation(february2024)))
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(february2024 -> TEN_POUNDS_PER_10ML))

      val result = service.isReasonMandatory(userAnswers, vpdId).futureValue

      result mustBe true
    }

    "must return true when over-declaration duty exceeds threshold" in {
      val adjustmentList = AdjustmentList(Seq(
        overDeclaredAdjustment(february2024, ml(50000))
      ))
      val userAnswers = returnsUserAnswers.set(pages.returns.adjustments.AdjustmentListPage, adjustmentList).success.value

      stubObligations(Seq(openObligation(february2024)))
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(february2024 -> TEN_POUNDS_PER_10ML))

      val result = service.isReasonMandatory(userAnswers, vpdId).futureValue

      result mustBe true
    }

    "must return false when duty is below threshold" in {
      val adjustmentList = AdjustmentList(Seq(
        underDeclaredAdjustment(february2024, ml(100))
      ))
      val userAnswers = returnsUserAnswers.set(pages.returns.adjustments.AdjustmentListPage, adjustmentList).success.value

      stubObligations(Seq(openObligation(february2024)))
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(february2024 -> TEN_POUNDS_PER_10ML))

      val result = service.isReasonMandatory(userAnswers, vpdId).futureValue

      result mustBe false
    }

    "must return false when no adjustments exist" in {
      val userAnswers = returnsUserAnswers

      val result = service.isReasonMandatory(userAnswers, vpdId).futureValue

      result mustBe false
    }
  }

  "shouldRedirectToReasonPage" - {

    "must return true when adjustmentReasonMandatory is true" in {
      val userAnswers = returnsUserAnswers

      val result = service.shouldRedirectToReasonPage(userAnswers, adjustmentReasonMandatory = true)

      result mustBe true
    }

    "must return true when adjustmentReasonMandatory is true even if reason already exists" in {
      val userAnswers = returnsUserAnswers
        .set(pages.returns.adjustments.AdjustmentReasonPage, "Test reason").success.value

      val result = service.shouldRedirectToReasonPage(userAnswers, adjustmentReasonMandatory = true)

      result mustBe true
    }

    "must return false when adjustmentReasonMandatory is false" in {
      val userAnswers = returnsUserAnswers

      val result = service.shouldRedirectToReasonPage(userAnswers, adjustmentReasonMandatory = false)

      result mustBe false
    }
  }

  "cleanupReasonIfNotRequired" - {

    "must remove reason when not mandatory and present" in {
      val userAnswers = returnsUserAnswers
        .set(pages.returns.adjustments.AdjustmentReasonPage, "Test reason").success.value

      val result = service.cleanupReasonIfNotRequired(userAnswers, adjustmentReasonMandatory = false).success.value

      result.get(pages.returns.adjustments.AdjustmentReasonPage) mustBe None
    }

    "must keep reason when mandatory and present" in {
      val userAnswers = returnsUserAnswers
        .set(pages.returns.adjustments.AdjustmentReasonPage, "Test reason").success.value

      val result = service.cleanupReasonIfNotRequired(userAnswers, adjustmentReasonMandatory = true).success.value

      result.get(pages.returns.adjustments.AdjustmentReasonPage) mustBe Some("Test reason")
    }

    "must not change user answers when reason not present and not mandatory" in {
      val userAnswers = returnsUserAnswers

      val result = service.cleanupReasonIfNotRequired(userAnswers, adjustmentReasonMandatory = false).success.value

      result mustBe userAnswers
    }

    "must not change user answers when reason not present and mandatory" in {
      val userAnswers = returnsUserAnswers

      val result = service.cleanupReasonIfNotRequired(userAnswers, adjustmentReasonMandatory = true).success.value

      result mustBe userAnswers
    }
  }
}
