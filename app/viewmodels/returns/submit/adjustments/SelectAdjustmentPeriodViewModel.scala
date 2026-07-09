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

package viewmodels.returns.submit.adjustments

import models.identifiers.PeriodKey
import models.obligations.ObligationDetails
import models.returns.adjustments.AdjustmentList
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.PaginationItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.tasklist.{TaskListItem, TaskListItemTitle}
import utils.ReturnsDateUtils
import viewmodels.returns.submit.PeriodSelectionHelper

case class SelectAdjustmentPeriodViewModel(
                                            periods: Seq[TaskListItem],
                                            paginationItems: Seq[PaginationItem],
                                            currentYear: Int
                                          )

object SelectAdjustmentPeriodViewModel {

  def apply(
             obligationDetails: Seq[ObligationDetails],
             selectedYear: Option[Int],
             currentReturnPeriod: PeriodKey,
             adjustmentList: Option[AdjustmentList],
             returnsDateUtils: ReturnsDateUtils
           )(implicit messages: Messages): SelectAdjustmentPeriodViewModel = {

    val fulfilledObligations = PeriodSelectionHelper.filterFulfilledWithinThreeYears(obligationDetails)
      .filter(_.periodKey != currentReturnPeriod.toString)

    val existingAdjustmentPeriods = adjustmentList
      .map(_.adjustments.map(_.period.toString).toSet)
      .getOrElse(Set.empty)

    val availableObligations = fulfilledObligations
      .filterNot(ob => existingAdjustmentPeriods.contains(ob.periodKey))

    val availableYears = PeriodSelectionHelper.extractAvailableYearsFromDetails(availableObligations)

    val currentYear = PeriodSelectionHelper.selectCurrentYear(availableYears, selectedYear)

    val obligationsForYear = PeriodSelectionHelper.filterObligationsByYearFromDetails(availableObligations, currentYear)

    val taskListItems = obligationsForYear.map { obligation =>
      val month = obligation.iCFromDate.getMonthValue
      val monthKey = returnsDateUtils.getMonthMessageKey(month)
      val periodKey = obligation.periodKey
      val href = s"${controllers.returns.submit.adjustments.routes.AdjustmentVolumeWithTypeController.onPageLoad(models.NormalMode).url}?period=${currentReturnPeriod.value}&adjustmentPeriod=$periodKey"

      TaskListItem(
        title = TaskListItemTitle(content = Text(messages(monthKey))),
        href = Some(href)
      )
    }

    val paginationItemHrefBuilder = (year: Int) =>
      s"${controllers.returns.submit.adjustments.routes.SelectAdjustmentPeriodController.onPageLoad(Some(year)).url}&period=${currentReturnPeriod.value}"

    val paginationItems = PeriodSelectionHelper.buildPaginationItems(availableYears, currentYear, paginationItemHrefBuilder)

    SelectAdjustmentPeriodViewModel(
      periods = taskListItems,
      paginationItems = paginationItems,
      currentYear = currentYear
    )
  }
}
