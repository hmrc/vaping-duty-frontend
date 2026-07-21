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

  private val TEN_POUNDS_PER_10ML = DutyRate(1000)

  implicit val messages: Messages = stubMessages()

  "buildViewModel" - {

    "must successfully build view model with adjustments" in {
      stubObligations(Seq(openObligation(february2024)))
      stubDutyRate(TEN_POUNDS_PER_10ML)

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
        periodKey = january2024,
        vpdId = vpdId
      ).futureValue

      result.hasAdjustments mustBe false
      result.summaryCards.size mustBe 1
      result.totalAdjustment mustBe BigDecimal(0)
    }

    "must correctly calculate duty rates for multiple periods" in {
      stubObligations(Seq(openObligation(february2024), openObligation(march2024)))
      stubDutyRate(TEN_POUNDS_PER_10ML)

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

      stubObligations(Seq(openObligation(january2024)))

      val exception = intercept[RuntimeException] {
        service.buildViewModel(
          declareAdjustment = Some(true),
          adjustmentList = Some(AdjustmentList(Seq(underDeclaredAdjustment(nonExistentPeriodKey, ml(1000))))),
          periodKey = january2024,
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
}
