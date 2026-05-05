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

import models.returns.{ObligationDetails, ObligationStatus, ObligationsResponse}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.TableRow

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

case class ViewMultipleReturnsViewModel(
  outstandingReturns: Seq[Seq[TableRow]],
  completedReturns: Seq[Seq[TableRow]]
)

object ViewMultipleReturnsViewModel {

  private val STATUS_OPEN = ObligationStatus.O
  private val STATUS_FULFILLED = ObligationStatus.F
  private val PLACEHOLDER_LINK = "#"
  private val TAG_CLASS_BLUE = "govuk-tag--blue"
  private val TAG_CLASS_RED = "govuk-tag--red"

  def apply(obligationsResponse: ObligationsResponse)(implicit messages: Messages): ViewMultipleReturnsViewModel = {

    val outstandingObligations = obligationsResponse.obligation
      .filter(_.obligationDetails.openOrFulfilledStatus == STATUS_OPEN.toString)
      .map(item => createOutstandingRow(item.obligationDetails))

    val completedObligations = obligationsResponse.obligation
      .filter(_.obligationDetails.openOrFulfilledStatus == STATUS_FULFILLED.toString)
      .map(item => createCompletedRow(item.obligationDetails))

    ViewMultipleReturnsViewModel(
      outstandingReturns = outstandingObligations.map(_.toTableRows),
      completedReturns = completedObligations.map(_.toTableRows)
    )
  }

  private def createOutstandingRow(details: ObligationDetails)(implicit messages: Messages): OutstandingReturnRow = {
    val monthDisplay = formatMonthYear(details.iCFromDate)
    val isOverdue = details.iCDueDate.isBefore(LocalDate.now())

    val status = if (isOverdue) {
      messages("returns.overview.outstanding.status.overdue")
    } else {
      messages("returns.overview.outstanding.status.due")
    }

    val statusClass = if (isOverdue) TAG_CLASS_RED else TAG_CLASS_BLUE

    OutstandingReturnRow(
      monthDisplay = monthDisplay,
      status = status,
      statusClass = statusClass,
      submitLink = PLACEHOLDER_LINK
    )
  }

  private def createCompletedRow(details: ObligationDetails): CompletedReturnRow = {
    CompletedReturnRow(
      monthDisplay = formatMonthYear(details.iCFromDate),
      viewLink = PLACEHOLDER_LINK
    )
  }

  private def formatMonthYear(date: LocalDate): String = {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    date.format(formatter)
  }
}
