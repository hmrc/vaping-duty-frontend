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
import models.obligations.ObligationDetails
import models.returns.SpoiltVolumeByPeriod
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.PaginationItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.tasklist.{TaskListItem, TaskListItemTitle}
import utils.ReturnsDateUtils

case class SelectSpoiltPeriodViewModel(
  periods: Seq[TaskListItem],
  paginationItems: Seq[PaginationItem],
  currentYear: Int
)

object SelectSpoiltPeriodViewModel {

  def apply(
    obligationDetails: Seq[ObligationDetails],
    selectedYear: Option[Int],
    currentReturnPeriod: PeriodKey,
    spoiltList: Option[List[SpoiltVolumeByPeriod]],
    returnsDateUtils: ReturnsDateUtils
  )(implicit messages: Messages): SelectSpoiltPeriodViewModel = {

    val fulfilledObligations = PeriodSelectionHelper.filterFulfilledWithinThreeYears(obligationDetails)
      .filter(_.periodKey != currentReturnPeriod.toString)

    val existingSpoiltPeriods = spoiltList
      .map(_.map(_.periodKey.toString).toSet)
      .getOrElse(Set.empty)

    val availableObligations = fulfilledObligations
      .filterNot(ob => existingSpoiltPeriods.contains(ob.periodKey))

    val availableYears = PeriodSelectionHelper.extractAvailableYearsFromDetails(availableObligations)

    val currentYear = PeriodSelectionHelper.selectCurrentYear(availableYears, selectedYear)

    val obligationsForYear = PeriodSelectionHelper.filterObligationsByYearFromDetails(availableObligations, currentYear)

    val taskListItems = obligationsForYear.map { obligation =>
      val month = obligation.iCFromDate.getMonthValue
      val monthKey = returnsDateUtils.getMonthMessageKey(month)
      val periodKey = obligation.periodKey
      val href = s"${controllers.returns.submit.spoilt.routes.SpoiltVolumeByPeriodController.onPageLoad().url}?period=${currentReturnPeriod.value}&spoiltPeriod=$periodKey"

      TaskListItem(
        title = TaskListItemTitle(content = Text(messages(monthKey))),
        href = Some(href)
      )
    }

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
