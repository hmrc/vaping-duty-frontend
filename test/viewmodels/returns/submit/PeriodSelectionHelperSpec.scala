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
import models.identifiers.PeriodKey
import models.obligations.ObligationsResponse
import models.returns.ReturnsConstants
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.tasklist.TaskListItemTitle
import utils.ReturnsDateUtils

class PeriodSelectionHelperSpec extends SpecBase {

  implicit val messages: Messages = messages(applicationBuilder(None).build())

  "filterFulfilledWithinThreeYears" - {

    "must filter to only fulfilled obligations" in {
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(
        openObligation(october2027),
        fulfilledObligation(december2027)
      )))

      val result = PeriodSelectionHelper.filterFulfilledWithinThreeYears(obligationsResponse)

      result.size mustBe 1
      result.head.obligationDetails.periodKey mustBe december2027.value
    }

    "must filter to only obligations within three years" in {
      val recentPeriod = PeriodKey("26AJ") // October 2026
      val oldPeriod = PeriodKey("23AJ")    // October 2023 (within 3 years from Feb 2026)
      
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(
        fulfilledObligation(recentPeriod),
        fulfilledObligation(oldPeriod)
      )))

      val result = PeriodSelectionHelper.filterFulfilledWithinThreeYears(obligationsResponse)

      result.size mustBe 2
      result.map(_.obligationDetails.periodKey) must contain allOf(recentPeriod.value, oldPeriod.value)
    }

    "must return empty sequence when no obligations match criteria" in {
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(
        openObligation(october2027)
      )))

      val result = PeriodSelectionHelper.filterFulfilledWithinThreeYears(obligationsResponse)

      result mustBe empty
    }
  }

  "extractAvailableYears" - {

    "must extract distinct years sorted in descending order" in {
      val period2024 = PeriodKey("24AC") // March 2024
      val period2025 = PeriodKey("25AF") // June 2025
      val period2024b = PeriodKey("24AI") // September 2024
      val period2023 = PeriodKey("23AL") // December 2023

      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(
        fulfilledObligation(period2024),
        fulfilledObligation(period2025),
        fulfilledObligation(period2024b),
        fulfilledObligation(period2023)
      )))

      val filtered = PeriodSelectionHelper.filterFulfilledWithinThreeYears(obligationsResponse)
      val result = PeriodSelectionHelper.extractAvailableYears(filtered)

      result mustBe Seq(2025, 2024, 2023)
    }

    "must return empty sequence for empty obligations" in {
      val result = PeriodSelectionHelper.extractAvailableYears(Seq.empty)

      result mustBe empty
    }

    "must handle single year" in {
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(
        fulfilledObligation(PeriodKey("24AC")), // March 2024
        fulfilledObligation(PeriodKey("24AF"))  // June 2024
      )))

      val filtered = PeriodSelectionHelper.filterFulfilledWithinThreeYears(obligationsResponse)
      val result = PeriodSelectionHelper.extractAvailableYears(filtered)

      result mustBe Seq(2024)
    }
  }

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

  "filterObligationsByYear" - {

    "must filter obligations by year" in {
      val period2024 = PeriodKey("24AC") // March 2024
      val period2025 = PeriodKey("25AF") // June 2025

      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(
        fulfilledObligation(period2024),
        fulfilledObligation(period2025)
      )))

      val filtered = PeriodSelectionHelper.filterFulfilledWithinThreeYears(obligationsResponse)
      val result = PeriodSelectionHelper.filterObligationsByYear(filtered, 2024)

      result.size mustBe 1
      result.head.obligationDetails.periodKey mustBe period2024.value
    }

    "must sort obligations by month in descending order" in {
      val marchPeriod = PeriodKey("24AC")     // March 2024
      val junePeriod = PeriodKey("24AF")      // June 2024
      val septemberPeriod = PeriodKey("24AI") // September 2024

      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(
        fulfilledObligation(marchPeriod),
        fulfilledObligation(septemberPeriod),
        fulfilledObligation(junePeriod)
      )))

      val filtered = PeriodSelectionHelper.filterFulfilledWithinThreeYears(obligationsResponse)
      val result = PeriodSelectionHelper.filterObligationsByYear(filtered, 2024)

      result.size mustBe 3
      result.head.obligationDetails.periodKey mustBe septemberPeriod.value
      result(1).obligationDetails.periodKey mustBe junePeriod.value
      result(2).obligationDetails.periodKey mustBe marchPeriod.value
    }

    "must return empty sequence when no obligations match year" in {
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(
        fulfilledObligation(PeriodKey("24AC")) // March 2024
      )))

      val filtered = PeriodSelectionHelper.filterFulfilledWithinThreeYears(obligationsResponse)
      val result = PeriodSelectionHelper.filterObligationsByYear(filtered, 2025)

      result mustBe empty
    }
  }

  "buildTaskListItems" - {

    "must build TaskListItems with correct month labels and hrefs" in {
      val marchPeriod = PeriodKey("24AC") // March 2024
      val junePeriod = PeriodKey("24AF")  // June 2024

      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(
        fulfilledObligation(marchPeriod),
        fulfilledObligation(junePeriod)
      )))

      val filtered = PeriodSelectionHelper.filterFulfilledWithinThreeYears(obligationsResponse)
      val hrefBuilder = (periodKey: String) => s"/test-url?period=$periodKey"

      val result = PeriodSelectionHelper.buildTaskListItems(filtered, hrefBuilder, returnsDateUtils)

      result.size mustBe 2
      result.head.title mustBe TaskListItemTitle(content = Text(messages("month.mar")))
      result.head.href mustBe Some(s"/test-url?period=${marchPeriod.value}")
      result(1).title mustBe TaskListItemTitle(content = Text(messages("month.jun")))
      result(1).href mustBe Some(s"/test-url?period=${junePeriod.value}")
    }

    "must return empty sequence for empty obligations" in {
      val hrefBuilder = (periodKey: String) => s"/test-url?period=$periodKey"

      val result = PeriodSelectionHelper.buildTaskListItems(Seq.empty, hrefBuilder, returnsDateUtils)

      result mustBe empty
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
