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
import models.returns.DutyRate
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when

import java.time.LocalDate

class DutyRateServiceSpec extends SpecBase with MockitoSugar {

  private val mockDutyRateConfig = mock[DutyRateConfig]
  
  private val testRates = Seq(
    DutyRate(
      startDate = LocalDate.of(2026, 1, 1),
      endDate = LocalDate.of(2026, 12, 31),
      ratePencePerMl = 22
    ),
    DutyRate(
      startDate = LocalDate.of(2027, 1, 1),
      endDate = LocalDate.of(2027, 12, 31),
      ratePencePerMl = 30
    ),
    DutyRate(
      startDate = LocalDate.of(2028, 1, 1),
      endDate = LocalDate.of(9999, 12, 31),
      ratePencePerMl = 40
    )
  )

  "DutyRateService" - {

    "getRateForDate" - {

      "must return the correct rate for a date within the first period" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        val service = new DutyRateService(mockDutyRateConfig)
        
        val date = LocalDate.of(2026, 6, 15)
        val result = service.getRateForDate(date)
        
        result mustBe 22
      }

      "must return the correct rate for a date within the second period" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        val service = new DutyRateService(mockDutyRateConfig)
        
        val date = LocalDate.of(2027, 8, 20)
        val result = service.getRateForDate(date)
        
        result mustBe 30
      }

      "must return the correct rate for a date within the third period" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        val service = new DutyRateService(mockDutyRateConfig)
        
        val date = LocalDate.of(2028, 3, 10)
        val result = service.getRateForDate(date)
        
        result mustBe 40
      }

      "must return the correct rate for a date on the start boundary" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        val service = new DutyRateService(mockDutyRateConfig)
        
        val date = LocalDate.of(2027, 1, 1)
        val result = service.getRateForDate(date)
        
        result mustBe 30
      }

      "must return the correct rate for a date on the end boundary" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        val service = new DutyRateService(mockDutyRateConfig)
        
        val date = LocalDate.of(2026, 12, 31)
        val result = service.getRateForDate(date)
        
        result mustBe 22
      }

      "must return the correct rate for a far future date" in {
        when(mockDutyRateConfig.rates).thenReturn(testRates)
        val service = new DutyRateService(mockDutyRateConfig)
        
        val date = LocalDate.of(3000, 1, 1)
        val result = service.getRateForDate(date)
        
        result mustBe 40
      }
    }
  }
}