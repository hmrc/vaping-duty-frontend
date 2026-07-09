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

package viewmodels.returns.submit

import base.SpecBase
import models.returns.ReturnsConstants
import play.api.i18n.Messages

class PeriodSelectionHelperSpec extends SpecBase {

  implicit val messages: Messages = messages(applicationBuilder(None).build())

  "selectCurrentYear" - {

    "must return selected year when provided" in {
      val availableYears = Seq(2025, 2024, 2023)
      val selectedYear = Some(2024)

      val result = PeriodSelectionHelper.selectCurrentYear(availableYears, selectedYear)

      result mustBe 2024
    }

    "must return most recent available year when no year selected" in {
      val availableYears = Seq(2025, 2024, 2023)

      val result = PeriodSelectionHelper.selectCurrentYear(availableYears, None)

      result mustBe 2025
    }

    "must return current year when no available years and no selection" in {
      val result = PeriodSelectionHelper.selectCurrentYear(Seq.empty, None)

      result mustBe java.time.LocalDate.now().getYear
    }
  }

  "buildPaginationItems" - {

    "must build pagination items with correct year numbers and hrefs" in {
      val years = Seq(2025, 2024, 2023)
      val currentYear = 2024
      val hrefBuilder = (year: Int) => s"/test-url?year=$year"

      val result = PeriodSelectionHelper.buildPaginationItems(years, currentYear, hrefBuilder)

      result.size mustBe 3
      result.head.number mustBe Some("2025")
      result.head.current mustBe Some(false)
      result.head.href mustBe "/test-url?year=2025"
      result(1).number mustBe Some("2024")
      result(1).current mustBe Some(true)
      result(1).href mustBe "/test-url?year=2024"
      result(2).number mustBe Some("2023")
      result(2).current mustBe Some(false)
      result(2).href mustBe "/test-url?year=2023"
    }

    "must limit pagination items to YEARS_TO_SHOW" in {
      val years = Seq(2025, 2024, 2023, 2022, 2021)
      val currentYear = 2024
      val hrefBuilder = (year: Int) => s"/test-url?year=$year"

      val result = PeriodSelectionHelper.buildPaginationItems(years, currentYear, hrefBuilder)

      result.size mustBe ReturnsConstants.YEARS_TO_SHOW
    }

    "must return empty sequence for empty years" in {
      val hrefBuilder = (year: Int) => s"/test-url?year=$year"

      val result = PeriodSelectionHelper.buildPaginationItems(Seq.empty, 2024, hrefBuilder)

      result mustBe empty
    }
  }
}
