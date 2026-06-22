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

import base.SpecBase
import models.obligations.{ObligationDetails, ObligationItem, ObligationsResponse}
import play.api.i18n.Messages

import java.time.LocalDate

class ViewMultipleReturnsViewModelSpec extends SpecBase {

  implicit val messages: Messages = messagesApi.preferred(fakeRequest)

  private val periodKey2024 = "24AA"
  private val periodKey2023 = "23AL"
  private val openStatus = "O"
  private val fulfilledStatus = "F"

  private val outstandingObligation = ObligationItem(
    ObligationDetails(
      openOrFulfilledStatus = openStatus,
      iCFromDate = LocalDate.of(2024, 1, 1),
      iCToDate = LocalDate.of(2024, 1, 31),
      iCDateReceived = None,
      iCDueDate = LocalDate.of(2024, 3, 7),
      periodKey = periodKey2024
    )
  )

  private val completedObligation2024 = ObligationItem(
    ObligationDetails(
      openOrFulfilledStatus = fulfilledStatus,
      iCFromDate = LocalDate.of(2024, 2, 1),
      iCToDate = LocalDate.of(2024, 2, 29),
      iCDateReceived = Some(LocalDate.of(2024, 4, 5)),
      iCDueDate = LocalDate.of(2024, 4, 7),
      periodKey = "24AB"
    )
  )

  private val completedObligation2023 = ObligationItem(
    ObligationDetails(
      openOrFulfilledStatus = fulfilledStatus,
      iCFromDate = LocalDate.of(2023, 12, 1),
      iCToDate = LocalDate.of(2023, 12, 31),
      iCDateReceived = Some(LocalDate.of(2024, 2, 5)),
      iCDueDate = LocalDate.of(2024, 2, 7),
      periodKey = periodKey2023
    )
  )

  "ViewMultipleReturnsViewModel" - {

    "must create view model with outstanding and completed returns for current year" in {
      val obligationsResponse = ObligationsResponse(Seq(outstandingObligation, completedObligation2024))

      val result = ViewMultipleReturnsViewModel(obligationsResponse, 2024)

      result.outstandingReturnsSection.items.length mustBe 1
      result.outstandingReturnsSection.showEmptyMessage mustBe false
      result.completedReturnsSections.length mustBe 1
      result.completedReturnsSections.head.items.length mustBe 1
      result.completedReturnsSections.head.year mustBe 2024
      result.paginationViewModel mustBe None
      result.shouldShowPagination mustBe false
    }

    "must create view model with only outstanding returns" in {
      val obligationsResponse = ObligationsResponse(Seq(outstandingObligation))

      val result = ViewMultipleReturnsViewModel(obligationsResponse, 2024)

      result.outstandingReturnsSection.items.length mustBe 1
      result.outstandingReturnsSection.showEmptyMessage mustBe false
      result.completedReturnsSections.length mustBe 1
      result.completedReturnsSections.head.showEmptyMessage mustBe true
    }

    "must create view model with only completed returns for specified year" in {
      val obligationsResponse = ObligationsResponse(Seq(completedObligation2023))

      val result = ViewMultipleReturnsViewModel(obligationsResponse, 2023)

      result.outstandingReturnsSection.items.length mustBe 0
      result.outstandingReturnsSection.showEmptyMessage mustBe true
      result.completedReturnsSections.length mustBe 1
      result.completedReturnsSections.head.year mustBe 2023
      result.completedReturnsSections.head.items.length mustBe 1
    }

    "must create view model with no returns" in {
      val obligationsResponse = ObligationsResponse(Seq.empty)

      val result = ViewMultipleReturnsViewModel(obligationsResponse, 2024)

      result.outstandingReturnsSection.items.length mustBe 0
      result.outstandingReturnsSection.showEmptyMessage mustBe true
      result.completedReturnsSections.length mustBe 1
      result.completedReturnsSections.head.showEmptyMessage mustBe true
    }

    "must show pagination when multiple years of completed returns exist" in {
      val obligationsResponse = ObligationsResponse(Seq(completedObligation2024, completedObligation2023))

      val result = ViewMultipleReturnsViewModel(obligationsResponse, 2024)

      result.paginationViewModel mustBe defined
      result.paginationViewModel.get.paginationItems.length mustBe 2
      result.paginationViewModel.get.paginationItems.head.number mustBe Some("2024")
      result.paginationViewModel.get.paginationItems.head.current mustBe Some(true)
      result.paginationViewModel.get.paginationItems(1).number mustBe Some("2023")
      result.paginationViewModel.get.paginationItems(1).current mustBe Some(false)
    }

    "must not show pagination when only one year of completed returns exists" in {
      val obligationsResponse = ObligationsResponse(Seq(completedObligation2024))

      val result = ViewMultipleReturnsViewModel(obligationsResponse, 2024)

      result.paginationViewModel mustBe None
    }

    "must show correct pagination items for middle year" in {
      val completedObligation2022 = ObligationItem(
        ObligationDetails(
          openOrFulfilledStatus = fulfilledStatus,
          iCFromDate = LocalDate.of(2022, 12, 1),
          iCToDate = LocalDate.of(2022, 12, 31),
          iCDateReceived = Some(LocalDate.of(2023, 2, 5)),
          iCDueDate = LocalDate.of(2023, 2, 7),
          periodKey = "22AL"
        )
      )

      val obligationsResponse = ObligationsResponse(Seq(completedObligation2024, completedObligation2023, completedObligation2022))

      val result = ViewMultipleReturnsViewModel(obligationsResponse, 2023)

      result.paginationViewModel mustBe defined
      result.paginationViewModel.get.paginationItems.length mustBe 3
      result.paginationViewModel.get.paginationItems(0).number mustBe Some("2024")
      result.paginationViewModel.get.paginationItems(0).current mustBe Some(false)
      result.paginationViewModel.get.paginationItems(1).number mustBe Some("2023")
      result.paginationViewModel.get.paginationItems(1).current mustBe Some(true)
      result.paginationViewModel.get.paginationItems(2).number mustBe Some("2022")
      result.paginationViewModel.get.paginationItems(2).current mustBe Some(false)
    }

    "must show empty message for completed returns when year has no completed returns" in {
      val obligationsResponse = ObligationsResponse(Seq(completedObligation2023))

      val result = ViewMultipleReturnsViewModel(obligationsResponse, 2024)

      result.completedReturnsSections.length mustBe 1
      result.completedReturnsSections.head.showEmptyMessage mustBe true
      result.completedReturnsSections.head.items.length mustBe 0
    }

    "must format outstanding returns with month and year" in {
      val obligationsResponse = ObligationsResponse(Seq(outstandingObligation))

      val result = ViewMultipleReturnsViewModel(obligationsResponse, 2024)

      val taskListItem = result.outstandingReturnsSection.items.head
      taskListItem.title.content.asHtml.body must include("January 2024")
    }

    "must format completed returns with month only" in {
      val obligationsResponse = ObligationsResponse(Seq(completedObligation2024))

      val result = ViewMultipleReturnsViewModel(obligationsResponse, 2024)

      val taskListItem = result.completedReturnsSections.head.items.head
      taskListItem.title.content.asHtml.body must include("February")
      taskListItem.title.content.asHtml.body mustNot include("2024")
    }
  }
}