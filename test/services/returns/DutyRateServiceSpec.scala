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
import config.DutyRateConfig
import models.identifiers.PeriodKey
import models.obligations.{ObligationDetails, ObligationStatus}
import models.returns.DutyRate
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import java.time.LocalDate
import scala.concurrent.Future

class DutyRateServiceSpec extends SpecBase with MockitoSugar {

  private val mockDutyRateConfig: DutyRateConfig = mock[DutyRateConfig]
  private val mockObligationService: ObligationService = mock[ObligationService]
  
  private val testRates = Seq(
    DutyRate(
      period = models.returns.DateRange(
        start = LocalDate.of(2026, 1, 1),
        end = LocalDate.of(2026, 12, 31)
      ),
      ratePencePer10Ml = 220
    ),
    DutyRate(
      period = models.returns.DateRange(
        start = LocalDate.of(2027, 1, 1),
        end = LocalDate.of(2027, 12, 31)
      ),
      ratePencePer10Ml = 300
    ),
    DutyRate(
      period = models.returns.DateRange(
        start = LocalDate.of(2028, 1, 1),
        end = LocalDate.of(9999, 12, 31)
      ),
      ratePencePer10Ml = 400
    )
  )

  private val testObligation = ObligationDetails(
    openOrFulfilledStatus = ObligationStatus.O.toString,
    iCFromDate = LocalDate.of(2026, 1, 1),
    iCToDate = LocalDate.of(2026, 1, 31),
    iCDateReceived = None,
    iCDueDate = LocalDate.of(2026, 2, 28),
    periodKey = "26AA"
  )

  "DutyRateService" - {

    "getRateForDate" - {

      "must return the correct rate for a date within the first period" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        val service = new DutyRateService(mockDutyRateConfig, mockObligationService)
        
        val date = LocalDate.of(2026, 6, 15)
        val result = service.getRateForDateInPencePer10ml(date)
        
        result mustBe 220
      }

      "must return the correct rate for a date within the second period" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        val service = new DutyRateService(mockDutyRateConfig, mockObligationService)
        
        val date = LocalDate.of(2027, 8, 20)
        val result = service.getRateForDateInPencePer10ml(date)
        
        result mustBe 300
      }

      "must return the correct rate for a date within the third period" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        val service = new DutyRateService(mockDutyRateConfig, mockObligationService)
        
        val date = LocalDate.of(2028, 3, 10)
        val result = service.getRateForDateInPencePer10ml(date)
        
        result mustBe 400
      }

      "must return the correct rate for a date on the start boundary" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        val service = new DutyRateService(mockDutyRateConfig, mockObligationService)
        
        val date = LocalDate.of(2027, 1, 1)
        val result = service.getRateForDateInPencePer10ml(date)
        
        result mustBe 300
      }

      "must return the correct rate for a date on the end boundary" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        val service = new DutyRateService(mockDutyRateConfig, mockObligationService)
        
        val date = LocalDate.of(2026, 12, 31)
        val result = service.getRateForDateInPencePer10ml(date)
        
        result mustBe 220
      }

      "must return the correct rate for a far future date" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        val service = new DutyRateService(mockDutyRateConfig, mockObligationService)
        
        val date = LocalDate.of(3000, 1, 1)
        val result = service.getRateForDateInPencePer10ml(date)
        
        result mustBe 400
      }
    }

    "getDutyRateForPeriod" - {

      "must return the calculated duty rate when obligation exists" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(PeriodKey("26AA")))(using any()))
          .thenReturn(Future.successful(Some(testObligation)))
        
        val service = new DutyRateService(mockDutyRateConfig, mockObligationService)
        
        whenReady(service.getDutyRateForPeriodInPoundsPerMl(vpdId, PeriodKey("26AA"))) { result =>
          result mustBe Some(BigDecimal("0.22"))
        }
      }

      "must return None when obligation does not exist" in {
        when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(PeriodKey("26XX")))(using any()))
          .thenReturn(Future.successful(None))
        
        val service = new DutyRateService(mockDutyRateConfig, mockObligationService)
        
        whenReady(service.getDutyRateForPeriodInPoundsPerMl(vpdId, PeriodKey("26XX"))) { result =>
          result mustBe None
        }
      }
    }

    "getDutyRate" - {

      "must return the duty rate when obligation exists" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(periodKey))(using any()))
          .thenReturn(Future.successful(Some(testObligation)))
        
        val service = new DutyRateService(mockDutyRateConfig, mockObligationService)
        
        whenReady(service.getDutyRateInPoundsPerMl(vpdId, periodKey)) { result =>
          result mustBe BigDecimal("0.22")
        }
      }

      "must fail with RuntimeException when obligation does not exist" in {
        
        when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(periodKey))(using any()))
          .thenReturn(Future.successful(None))
        
        val service = new DutyRateService(mockDutyRateConfig, mockObligationService)
        
        whenReady(service.getDutyRateInPoundsPerMl(vpdId, periodKey).failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustBe "No duty rate found"
        }
      }
    }
  }
}
