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

import models.identifiers.PeriodKey
import models.obligations.ObligationsResponse
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.{PaginationItem, PaginationLink}
import uk.gov.hmrc.govukfrontend.views.viewmodels.tasklist.{TaskListItem, TaskListItemTitle}

import java.time.LocalDate

case class SelectSpoiltPeriodViewModel(
  periods: Seq[TaskListItem],
  paginationItems: Seq[PaginationItem],
  currentYear: Int
)

object SelectSpoiltPeriodViewModel {

  private val STATUS_FULFILLED = "F"
  private val YEARS_TO_SHOW = 3

  def apply(
    obligationsResponse: ObligationsResponse,
    selectedYear: Option[Int],
    currentReturnPeriod: PeriodKey
  )(implicit messages: Messages): SelectSpoiltPeriodViewModel = {

    val currentDate = LocalDate.now()
    val threeYearsAgo = currentDate.minusYears(YEARS_TO_SHOW)

    val fulfilledObligations = obligationsResponse.obligation
      .filter(_.obligationDetails.openOrFulfilledStatus == STATUS_FULFILLED)
      .filter(_.obligationDetails.iCFromDate.isAfter(threeYearsAgo))

    val availableYears = fulfilledObligations
      .map(_.obligationDetails.iCFromDate.getYear)
      .distinct
      .sorted(Ordering[Int].reverse)

    val currentYear = selectedYear.getOrElse(
      availableYears.headOption.getOrElse(currentDate.getYear)
    )

    val obligationsForYear = fulfilledObligations
      .filter(_.obligationDetails.iCFromDate.getYear == currentYear)
      .sortBy(_.obligationDetails.iCFromDate.getMonthValue)(Ordering[Int].reverse)

    val taskListItems = obligationsForYear.map { obligation =>
      val month = obligation.obligationDetails.iCFromDate.getMonthValue
      val monthKey = getMonthMessageKey(month)
      val periodKey = obligation.obligationDetails.periodKey

      TaskListItem(
        title = TaskListItemTitle(content = Text(messages(monthKey))),
        href = Some(s"${controllers.returns.submit.routes.SpoiltVolumeByPeriodController.onPageLoad().url}?period=${currentReturnPeriod.value}&spoiltPeriod=$periodKey")
      )
    }

    val paginationYears = availableYears.take(YEARS_TO_SHOW)

    val paginationItems = paginationYears.map { year =>
      PaginationItem(
        number = Some(year.toString),
        current = Some(year == currentYear),
        href = s"${controllers.returns.submit.routes.SelectSpoiltPeriodController.onPageLoad(Some(year)).url}&period=${currentReturnPeriod.value}"
      )
    }

    SelectSpoiltPeriodViewModel(
      periods = taskListItems,
      paginationItems = paginationItems,
      currentYear = currentYear
    )
  }

  private def getMonthMessageKey(month: Int): String = month match {
    case 1  => "month.jan"
    case 2  => "month.feb"
    case 3  => "month.mar"
    case 4  => "month.apr"
    case 5  => "month.may"
    case 6  => "month.jun"
    case 7  => "month.jul"
    case 8  => "month.aug"
    case 9  => "month.sep"
    case 10 => "month.oct"
    case 11 => "month.nov"
    case 12 => "month.dec"
  }
}