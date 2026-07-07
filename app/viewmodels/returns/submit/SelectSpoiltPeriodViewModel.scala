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
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.PaginationItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.tasklist.TaskListItem

case class SelectSpoiltPeriodViewModel(
  periods: Seq[TaskListItem],
  paginationItems: Seq[PaginationItem],
  currentYear: Int
)

object SelectSpoiltPeriodViewModel {

  def apply(
    obligationsResponse: ObligationsResponse,
    selectedYear: Option[Int],
    currentReturnPeriod: PeriodKey
  )(implicit messages: Messages): SelectSpoiltPeriodViewModel = {

    val fulfilledObligations = PeriodSelectionHelper.filterFulfilledWithinThreeYears(obligationsResponse)

    val availableYears = PeriodSelectionHelper.extractAvailableYears(fulfilledObligations)

    val currentYear = PeriodSelectionHelper.selectCurrentYear(availableYears, selectedYear)

    val obligationsForYear = PeriodSelectionHelper.filterObligationsByYear(fulfilledObligations, currentYear)

    val taskListItemHrefBuilder = (periodKey: String) =>
      s"${controllers.returns.submit.spoilt.routes.SpoiltVolumeByPeriodController.onPageLoad().url}?period=${currentReturnPeriod.value}&spoiltPeriod=$periodKey"

    val taskListItems = PeriodSelectionHelper.buildTaskListItems(obligationsForYear, taskListItemHrefBuilder)

    val paginationItemHrefBuilder = (year: Int) =>
      s"${controllers.returns.submit.spoilt.routes.SelectSpoiltPeriodController.onPageLoad(Some(year)).url}&period=${currentReturnPeriod.value}"

    val paginationItems = PeriodSelectionHelper.buildPaginationItems(availableYears, currentYear, paginationItemHrefBuilder)

    SelectSpoiltPeriodViewModel(
      periods = taskListItems,
      paginationItems = paginationItems,
      currentYear = currentYear
    )
  }
}
