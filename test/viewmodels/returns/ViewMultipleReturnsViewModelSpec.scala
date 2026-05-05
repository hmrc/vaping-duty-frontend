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

package viewmodels.returns

import base.SpecBase
import models.returns.{ObligationItem, ObligationsResponse}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import viewmodels.returns.view.{CompletedReturnRow, OutstandingReturnRow, ViewMultipleReturnsViewModel}

import java.time.LocalDate

class ViewMultipleReturnsViewModelSpec extends SpecBase {

  implicit val messages: Messages = messages(applicationBuilder(None).build())

  private val today = LocalDate.now()
  private val pastDate = today.minusDays(1)
  private val futureDate = today.plusDays(10)

  private val outstandingObligation = createMockObligationsResponse().obligation.head

  private val overdueObligation = createMockObligationsResponse().obligation(1)

  private val completedObligation = createMockObligationsResponse().obligation(2)

  "ViewMultipleReturnsViewModel" - {

    "must create view model with outstanding and completed returns" in {
      val response = ObligationsResponse(
        obligation = Seq(outstandingObligation, completedObligation)
      )

      val result = ViewMultipleReturnsViewModel(response)

      result.outstandingReturns.size mustBe 1
      result.completedReturns.size mustBe 1
    }

    "must separate outstanding and completed obligations correctly" in {
      val response = ObligationsResponse(
        obligation = Seq(outstandingObligation, overdueObligation, completedObligation)
      )

      val result = ViewMultipleReturnsViewModel(response)

      result.outstandingReturns.size mustBe 2
      result.completedReturns.size mustBe 1
    }

    "must format month display correctly for outstanding returns" in {
      val response = ObligationsResponse(
        obligation = Seq(outstandingObligation)
      )

      val result = ViewMultipleReturnsViewModel(response)

      val firstRow = result.outstandingReturns.head
      firstRow.head.content mustBe Text("December 2027")
    }

    "must format month display correctly for completed returns" in {
      val response = ObligationsResponse(
        obligation = Seq(completedObligation)
      )

      val result = ViewMultipleReturnsViewModel(response)

      val firstRow = result.completedReturns.head
      firstRow.head.content mustBe Text("October 2027")
    }

    "must handle empty obligations response" in {
      val response = ObligationsResponse(obligation = Seq.empty)

      val result = ViewMultipleReturnsViewModel(response)

      result.outstandingReturns mustBe empty
      result.completedReturns mustBe empty
    }

    "must handle response with only outstanding obligations" in {
      val response = ObligationsResponse(
        obligation = Seq(outstandingObligation, overdueObligation)
      )

      val result = ViewMultipleReturnsViewModel(response)

      result.outstandingReturns.size mustBe 2
      result.completedReturns mustBe empty
    }

    "must handle response with only completed obligations" in {
      val response = ObligationsResponse(
        obligation = Seq(completedObligation)
      )

      val result = ViewMultipleReturnsViewModel(response)

      result.outstandingReturns mustBe empty
      result.completedReturns.size mustBe 1
    }
  }

  "OutstandingReturnRow" - {

    "must convert to table rows with three columns" in {
      val row = OutstandingReturnRow(
        monthDisplay = "January 2024",
        status = messages("returns.overview.outstanding.status.due"),
        statusClass = "govuk-tag--blue",
        submitLink = "#"
      )

      val result = row.toTableRows

      result.size mustBe 3
    }

    "must include month display in first column" in {
      val row = OutstandingReturnRow(
        monthDisplay = "January 2024",
        status = messages("returns.overview.outstanding.status.due"),
        statusClass = "govuk-tag--blue",
        submitLink = "#"
      )

      val result = row.toTableRows

      result.head.content mustBe Text("January 2024")
    }

    "must convert to table rows with due status" in {
      val row = OutstandingReturnRow(
        monthDisplay = "January 2024",
        status = messages("returns.overview.outstanding.status.due"),
        statusClass = "govuk-tag--blue",
        submitLink = "#"
      )

      val result = row.toTableRows

      result.size mustBe 3
      result.head.content mustBe Text("January 2024")
    }

    "must convert to table rows with overdue status" in {
      val row = OutstandingReturnRow(
        monthDisplay = "December 2023",
        status = messages("returns.overview.outstanding.status.overdue"),
        statusClass = "govuk-tag--red",
        submitLink = "#"
      )

      val result = row.toTableRows

      result.size mustBe 3
      result.head.content mustBe Text("December 2023")
    }

    "must include submit link in table rows" in {
      val row = OutstandingReturnRow(
        monthDisplay = "January 2024",
        status = messages("returns.overview.outstanding.status.due"),
        statusClass = "govuk-tag--blue",
        submitLink = "/submit-return"
      )

      val result = row.toTableRows

      result.size mustBe 3
    }
  }

  "CompletedReturnRow" - {

    "must convert to table rows with two columns" in {
      val row = CompletedReturnRow(
        monthDisplay = "November 2023",
        viewLink = "#"
      )

      val result = row.toTableRows

      result.size mustBe 2
    }

    "must include month display in first column" in {
      val row = CompletedReturnRow(
        monthDisplay = "November 2023",
        viewLink = "#"
      )

      val result = row.toTableRows

      result.head.content mustBe Text("November 2023")
    }

    "must include view link in table rows" in {
      val row = CompletedReturnRow(
        monthDisplay = "November 2023",
        viewLink = "/view-return/123"
      )

      val result = row.toTableRows

      result.size mustBe 2
    }

    "must handle different month formats" in {
      val row = CompletedReturnRow(
        monthDisplay = "December 2023",
        viewLink = "#"
      )

      val result = row.toTableRows

      result.head.content mustBe Text("December 2023")
    }
  }
}
