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
import models.returns.AdjustmentList
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.PaginationItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.tasklist.{TaskListItem, TaskListItemTitle}
import utils.ReturnsDateUtils

import java.time.LocalDate

case class SelectAdjustmentPeriodViewModel(
  periods: Seq[TaskListItem],
  paginationItems: Seq[PaginationItem],
  currentYear: Int
)

object SelectAdjustmentPeriodViewModel {

  private val STATUS_FULFILLED = "F"
  private val YEARS_TO_SHOW = 3

  def apply(
    obligationsResponse: ObligationsResponse,
    selectedYear: Option[Int],
    currentReturnPeriod: PeriodKey,
    adjustmentList: Option[AdjustmentList]
  )(implicit messages: Messages): SelectAdjustmentPeriodViewModel = {

    val currentDate = LocalDate.now()
    val threeYearsAgo = currentDate.minusYears(YEARS_TO_SHOW)

    val fulfilledObligations = obligationsResponse.obligation
      .filter(_.obligationDetails.openOrFulfilledStatus == STATUS_FULFILLED)
      .filter(_.obligationDetails.iCFromDate.isAfter(threeYearsAgo))
      .filter(_.obligationDetails.periodKey != currentReturnPeriod.toString)

    // Filter out periods already in adjustment list
    val existingAdjustmentPeriods = adjustmentList
      .map(_.adjustments.map(_.period.toString).toSet)
      .getOrElse(Set.empty)

    val availableObligations = fulfilledObligations
      .filterNot(ob => existingAdjustmentPeriods.contains(ob.obligationDetails.periodKey))

    val availableYears = availableObligations
      .map(_.obligationDetails.iCFromDate.getYear)
      .distinct
      .sorted(Ordering[Int].reverse)

    val currentYear = selectedYear.getOrElse(
      availableYears.headOption.getOrElse(currentDate.getYear)
    )

    val obligationsForYear = availableObligations
      .filter(_.obligationDetails.iCFromDate.getYear == currentYear)
      .sortBy(_.obligationDetails.iCFromDate.getMonthValue)(Ordering[Int].reverse)

    val taskListItems = obligationsForYear.map { obligation =>
      val month = obligation.obligationDetails.iCFromDate.getMonthValue
      val monthKey = ReturnsDateUtils.getMonthMessageKey(month)
      val periodKey = obligation.obligationDetails.periodKey

      TaskListItem(
        title = TaskListItemTitle(content = Text(messages(monthKey))),
        href = Some(s"${controllers.returns.submit.routes.AdjustmentVolumeWithTypeController.onPageLoad(models.NormalMode).url}?period=${currentReturnPeriod.value}&adjustmentPeriod=$periodKey")
      )
    }

    val paginationYears = availableYears.take(YEARS_TO_SHOW)

    val paginationItems = paginationYears.map { year =>
      PaginationItem(
        number = Some(year.toString),
        current = Some(year == currentYear),
        href = s"${controllers.returns.submit.routes.SelectAdjustmentPeriodController.onPageLoad(Some(year)).url}&period=${currentReturnPeriod.value}"
      )
    }

    SelectAdjustmentPeriodViewModel(
      periods = taskListItems,
      paginationItems = paginationItems,
      currentYear = currentYear
    )
  }
}
