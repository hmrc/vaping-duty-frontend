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
import models.returns.{DutyRate, SpoiltVolumeByPeriod}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.test.Helpers.*
import utils.ReturnsDateUtils

import scala.concurrent.Future

class SpoiltCheckYourAnswersServiceSpec extends SpecBase with MockitoSugar with ObligationsBuilders {

  private val mockObligationService: ObligationService = mock[ObligationService]
  private val mockDutyRateService: DutyRateService = mock[DutyRateService]
  private val mockReturnsDateUtils: ReturnsDateUtils = mock[ReturnsDateUtils]
  private val mockSessionRepository: ReturnsUserAnswersService = mock[ReturnsUserAnswersService]

  private val service = new SpoiltCheckYourAnswersService(
    mockObligationService,
    mockDutyRateService,
    mockReturnsDateUtils,
    mockSessionRepository
  )

  private val vpdId = VpdId("XMVPD0000000123")
  private val periodKey = PeriodKey("24AA")
  private val spoiltPeriodKey = PeriodKey("24AB")

  private val obligationDetails = Seq(
    openObligation(periodKey)
  )

  private val TEN_POUNDS_PER_10ML = DutyRate(1000)

  implicit val messages: Messages = stubMessages()

  "buildViewModel" - {

    "must successfully build view model with spoilt products" in {
      val spoiltEntry = SpoiltVolumeByPeriod(volume = BigDecimal(1000), periodKey = spoiltPeriodKey)
      val obligationForSpoilt = openObligation(spoiltPeriodKey)

      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq(obligationForSpoilt)))
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(spoiltPeriodKey -> TEN_POUNDS_PER_10ML))

      val result = service.buildViewModel(
        declareSpoiltProducts = Some(true),
        spoiltList = Some(List(spoiltEntry)),
        periodKey = periodKey,
        vpdId = vpdId
      ).futureValue

      result.hasSpoiltProducts mustBe true
      result.summaryCards.size mustBe 1
      result.totalSpoiltDuty mustBe BigDecimal(1000.00)
    }

    "must successfully build view model with no spoilt products when declareSpoiltProducts is false" in {
      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(obligationDetails))

      val result = service.buildViewModel(
        declareSpoiltProducts = Some(false),
        spoiltList = None,
        periodKey = periodKey,
        vpdId = vpdId
      ).futureValue

      result.hasSpoiltProducts mustBe false
      result.summaryCards.size mustBe 1
      result.totalSpoiltDuty mustBe BigDecimal(0)
    }

    "must correctly calculate duty rates for multiple periods" in {
      val spoiltPeriodKey2 = PeriodKey("24AC")

      val obligationForSpoilt1 = openObligation(spoiltPeriodKey)
      val obligationForSpoilt2 = openObligation(spoiltPeriodKey2)

      val spoiltEntry1 = SpoiltVolumeByPeriod(volume = BigDecimal(1000), periodKey = spoiltPeriodKey)
      val spoiltEntry2 = SpoiltVolumeByPeriod(volume = BigDecimal(500), periodKey = spoiltPeriodKey2)

      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq(obligationForSpoilt1, obligationForSpoilt2)))
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(
          spoiltPeriodKey -> TEN_POUNDS_PER_10ML,
          spoiltPeriodKey2 -> TEN_POUNDS_PER_10ML
        ))

      val result = service.buildViewModel(
        declareSpoiltProducts = Some(true),
        spoiltList = Some(List(spoiltEntry1, spoiltEntry2)),
        periodKey = periodKey,
        vpdId = vpdId
      ).futureValue

      result.hasSpoiltProducts mustBe true
      result.summaryCards.size mustBe 2
      result.totalSpoiltDuty mustBe BigDecimal(1500.00)
    }

    "must handle empty spoilt list" in {
      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(obligationDetails))

      val result = service.buildViewModel(
        declareSpoiltProducts = Some(true),
        spoiltList = Some(List.empty),
        periodKey = periodKey,
        vpdId = vpdId
      ).futureValue

      result.hasSpoiltProducts mustBe false
      result.summaryCards.size mustBe 0
      result.totalSpoiltDuty mustBe BigDecimal(0)
    }

    "must throw RuntimeException when obligation not found for spoilt period" in {
      val nonExistentPeriodKey = PeriodKey("24ZZ")
      val spoiltEntry = SpoiltVolumeByPeriod(volume = BigDecimal(1000), periodKey = nonExistentPeriodKey)

      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(obligationDetails))
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map.empty[PeriodKey, DutyRate])
      when(mockReturnsDateUtils.formatPeriodDisplay(eqTo(nonExistentPeriodKey), eqTo(obligationDetails))(using any()))
        .thenThrow(new RuntimeException("No obligation found for period 24ZZ"))

      val exception = intercept[RuntimeException] {
        service.buildViewModel(
          declareSpoiltProducts = Some(true),
          spoiltList = Some(List(spoiltEntry)),
          periodKey = periodKey,
          vpdId = vpdId
        ).futureValue
      }

      exception.getMessage must include("No obligation found for period 24ZZ")
    }
  }

  "buildRemoveViewModel" - {

    "must return Some with the details rows when the entry and its obligation are found" in {
      val spoiltEntry = SpoiltVolumeByPeriod(volume = BigDecimal(1000), periodKey = spoiltPeriodKey)
      val obligationForSpoilt = openObligation(spoiltPeriodKey)

      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq(obligationForSpoilt)))
      when(mockDutyRateService.getDutyRateForDate(any()))
        .thenReturn(TEN_POUNDS_PER_10ML)
      when(mockReturnsDateUtils.formatPeriodDisplay(eqTo(spoiltPeriodKey), any[Seq[models.obligations.ObligationDetails]])(using any()))
        .thenReturn("December 2026")

      val result = service.buildRemoveViewModel(
        spoiltList = Some(List(spoiltEntry)),
        spoiltPeriod = spoiltPeriodKey,
        vpdId = vpdId
      ).futureValue

      result.value.rows.size mustBe 3
    }

    "must return None when the entry is not in the spoilt list" in {
      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(obligationDetails))

      val result = service.buildRemoveViewModel(
        spoiltList = Some(List.empty),
        spoiltPeriod = spoiltPeriodKey,
        vpdId = vpdId
      ).futureValue

      result mustBe None
    }

    "must return None when no obligation exists for the entry's period" in {
      val spoiltEntry = SpoiltVolumeByPeriod(volume = BigDecimal(1000), periodKey = spoiltPeriodKey)

      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq.empty))

      val result = service.buildRemoveViewModel(
        spoiltList = Some(List(spoiltEntry)),
        spoiltPeriod = spoiltPeriodKey,
        vpdId = vpdId
      ).futureValue

      result mustBe None
    }
  }

  "hasAvailablePeriodsToAdd" - {

    "must return true when a fulfilled period has not yet been declared as spoilt" in {
      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq(fulfilledObligation(spoiltPeriodKey))))

      val result = service.hasAvailablePeriodsToAdd(
        spoiltList = None,
        periodKey = periodKey,
        vpdId = vpdId
      ).futureValue

      result mustBe true
    }

    "must return false when every fulfilled period has already been declared as spoilt" in {
      when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(Seq(fulfilledObligation(spoiltPeriodKey))))

      val result = service.hasAvailablePeriodsToAdd(
        spoiltList = Some(List(SpoiltVolumeByPeriod(volume = BigDecimal(1000), periodKey = spoiltPeriodKey))),
        periodKey = periodKey,
        vpdId = vpdId
      ).futureValue

      result mustBe false
    }
  }
}
