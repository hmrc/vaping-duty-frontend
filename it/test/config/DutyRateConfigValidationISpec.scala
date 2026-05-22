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

package config

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.inject.guice.GuiceApplicationBuilder

import java.time.LocalDate

class DutyRateConfigValidationISpec extends AnyFreeSpec with Matchers {

  "DutyRateConfig loaded from application.conf" - {

    "must load successfully and pass all validation rules" in {
      val app = new GuiceApplicationBuilder().build()
      val dutyRateConfig = app.injector.instanceOf[DutyRateConfig]
      
      // Must have at least one rate
      dutyRateConfig.rates must not be empty
      
      // All rates must be positive
      dutyRateConfig.rates.foreach { rate =>
        rate.ratePencePerMl must be > 0
      }
      
      // All end dates must be after or equal to start dates
      dutyRateConfig.rates.foreach { rate =>
        rate.endDate.isAfter(rate.startDate) || rate.endDate.isEqual(rate.startDate) mustBe true
      }
      
      // Rates must be sorted by start date
      val sortedRates = dutyRateConfig.rates.sortBy(_.startDate)
      dutyRateConfig.rates mustBe sortedRates
      
      // No gaps or overlaps between periods
      dutyRateConfig.rates.sliding(2).foreach {
        case Seq(current, next) =>
          val dayAfterCurrentEnd = current.endDate.plusDays(1)
          dayAfterCurrentEnd mustBe next.startDate
        case _ => // Single element, no validation needed
      }
      
      // Current date must be covered
      val today = LocalDate.now()
      val isTodayCovered = dutyRateConfig.rates.exists(_.isValidFor(today))
      isTodayCovered mustBe true
      
      app.stop()
    }

    "must have rates configured for a reasonable future period" in {
      val app = new GuiceApplicationBuilder().build()
      val dutyRateConfig = app.injector.instanceOf[DutyRateConfig]
      
      // Check that rates extend at least 1 year into the future
      val oneYearFromNow = LocalDate.now().plusYears(1)
      val isFutureCovered = dutyRateConfig.rates.exists(_.isValidFor(oneYearFromNow))
      
      isFutureCovered mustBe true
      
      app.stop()
    }

    "must have all dates in ISO format (YYYY-MM-DD)" in {
      val app = new GuiceApplicationBuilder().build()
      val dutyRateConfig = app.injector.instanceOf[DutyRateConfig]
      
      // If we got here, dates parsed successfully (LocalDate.parse would have thrown otherwise)
      dutyRateConfig.rates.foreach { rate =>
        rate.startDate must not be null
        rate.endDate must not be null
      }
      
      app.stop()
    }
  }
}