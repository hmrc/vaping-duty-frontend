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
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers.*

import scala.concurrent.Future

class AdjustmentCheckYourAnswersServiceSpec extends SpecBase with MockitoSugar with ObligationsBuilders {

  private val mockObligationService: ObligationService = mock[ObligationService]
  private val mockDutyRateService: DutyRateService = mock[DutyRateService]

  private val service = new AdjustmentCheckYourAnswersService(
    mockObligationService,
    mockDutyRateService
  )

  private val vpdId = VpdId("XMVPD0000000123")
  private val periodKey = PeriodKey("24AA")
  private val adjustmentPeriodKey = PeriodKey("24AB")

  private val obligationDetails = Seq(
    openObligation(periodKey)
  )

  private val DUTY_RATE_PENCE = 100

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
      when(mockDutyRateService.getRateForDate(any()))
        .thenReturn(DUTY_RATE_PENCE)

      val result = service.buildViewModel(
        declareAdjustment = Some(true),
        adjustmentList = Some(adjustmentList),
        periodKey = periodKey,
        vpdId = vpdId
      ).futureValue

      result.hasAdjustments mustBe true
      result.summaryCards.size mustBe 1
      result.totalAdjustment mustBe BigDecimal(1000.00)
    }

    "must successfully build view model with no adjustments when declareAdjustment is false" in {
      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(obligationDetails))

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
      when(mockDutyRateService.getRateForDate(any()))
        .thenReturn(DUTY_RATE_PENCE)

      val result = service.buildViewModel(
        declareAdjustment = Some(true),
        adjustmentList = Some(adjustmentList),
        periodKey = periodKey,
        vpdId = vpdId
      ).futureValue

      result.hasAdjustments mustBe true
      result.summaryCards.size mustBe 2
      result.totalAdjustment mustBe BigDecimal(500.00) // 1000 - 500
    }

    "must handle empty adjustment list" in {
      val adjustmentList = AdjustmentList(Seq.empty)

      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(obligationDetails))

      val result = service.buildViewModel(
        declareAdjustment = Some(true),
        adjustmentList = Some(adjustmentList),
        periodKey = periodKey,
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
        .thenReturn(Future.successful(obligationDetails))

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
}