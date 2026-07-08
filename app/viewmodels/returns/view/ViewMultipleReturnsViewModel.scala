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

package viewmodels.returns.view

import models.identifiers.PeriodKey
import models.obligations.{ObligationDetails, ObligationStatus, ObligationsResponse}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.PaginationItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.tasklist.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.tag.Tag
import utils.ReturnsDateUtils

import java.time.{LocalDate, Month}

final case class ViewMultipleReturnsViewModel(
                                               outstandingReturnsSection: OutstandingReturnsSection,
                                               completedReturnsSection: CompletedReturnsSection,
                                               paginationViewModel: Option[PaginationViewModel],
                                               shouldShowPagination: Boolean
                                             )

object ViewMultipleReturnsViewModel {

  private val TAG_CLASS_BLUE = "govuk-tag--blue"
  private val TAG_CLASS_RED = "govuk-tag--red"

  def apply(obligationsResponse: ObligationsResponse, currentYear: Int, now: LocalDate, returnsDateUtils: ReturnsDateUtils)
           (implicit messages: Messages): ViewMultipleReturnsViewModel = {

    val (outstandingObligations, fulfilledObligations) =
      obligationsResponse.obligation.partition(_.obligationDetails.openOrFulfilledStatus == ObligationStatus.O.toString)

    val outstandingItems =
      outstandingObligations
        .sortBy(_.obligationDetails.iCFromDate)(Ordering[LocalDate].reverse)
        .map(item => createOutstandingTaskListItem(item.obligationDetails, now, returnsDateUtils))

    val outstandingSection = OutstandingReturnsSection(
      items = outstandingItems,
      showEmptyMessage = outstandingItems.isEmpty,
      shouldShowSection = outstandingItems.nonEmpty
    )

    val completedByYear: Map[Int, Seq[ObligationDetails]] =
      fulfilledObligations
        .map(_.obligationDetails)
        .groupBy(_.iCFromDate.getYear)

    val allYears = completedByYear.keys.toSeq.sorted.reverse

    val paginationViewModel = if (allYears.sizeIs > 1) {
      val paginationItems = allYears.map { year =>
        PaginationItem(
          number = Some(year.toString),
          href = controllers.returns.view.routes.ViewMultipleReturnsController.onPageLoad(Some(year)).url,
          current = Some(year == currentYear)
        )
      }
      Some(PaginationViewModel(paginationItems))
    } else None

    val completedSection = CompletedReturnsSection(
      year = currentYear.toString,
      items = completedByYear.get(currentYear)
        .map(_.sortBy(_.iCFromDate)(Ordering[LocalDate].reverse).map(createCompletedTaskListItem(_, returnsDateUtils)))
        .getOrElse(Seq.empty),
      showEmptyMessage = completedByYear.get(currentYear).isEmpty,
      shouldShowItems = completedByYear.get(currentYear).nonEmpty
    )

    ViewMultipleReturnsViewModel(
      outstandingReturnsSection = outstandingSection,
      completedReturnsSection = completedSection,
      paginationViewModel = paginationViewModel,
      shouldShowPagination = paginationViewModel.isDefined
    )
  }

  private def createOutstandingTaskListItem(details: ObligationDetails, now: LocalDate, returnsDateUtils: ReturnsDateUtils)
                                           (implicit messages: Messages): TaskListItem = {

    val month = Month.of(details.iCFromDate.getMonthValue)
    val year = details.iCFromDate.getYear
    val monthDisplay = s"${returnsDateUtils.getMonthMessage(month)} $year"
    val isOverdue = details.iCDueDate.isBefore(now)

    val status = if (isOverdue) {
      messages("returns.overview.outstanding.status.overdue")
    } else {
      messages("returns.overview.outstanding.status.due")
    }

    val statusClass = if (isOverdue) TAG_CLASS_RED else TAG_CLASS_BLUE

    TaskListItem(
      title = TaskListItemTitle(content = Text(monthDisplay)),
      href = Some(s"${controllers.returns.submit.routes.BeforeYouStartController.onPageLoad().url}?period=${details.periodKey}"),
      status = TaskListItemStatus(
        tag = Some(Tag(
          content = Text(status),
          classes = statusClass
        ))
      )
    )
  }

  private def createCompletedTaskListItem(details: ObligationDetails, returnsDateUtils: ReturnsDateUtils)
                                         (implicit messages: Messages): TaskListItem = {

    val month = Month.of(details.iCFromDate.getMonthValue)
    val monthDisplay = returnsDateUtils.getMonthMessage(month)

    TaskListItem(
      title = TaskListItemTitle(content = Text(monthDisplay)),
      href = Some(controllers.returns.view.routes.ViewIndividualReturnController.onPageLoad(PeriodKey(details.periodKey)).url),
      status = TaskListItemStatus(
        content = Text(messages("returns.overview.completed.status"))
      )
    )
  }
}
