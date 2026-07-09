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
import models.obligations.{ObligationDetails, ObligationItem, ObligationStatus}
import utils.ReturnsDateUtils

import java.time.LocalDate

class TaskListPageViewModelSpec extends SpecBase {

  private val app = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()
  private implicit val returnsDateUtils: ReturnsDateUtils = app.injector.instanceOf[ReturnsDateUtils]

  private val testPeriodKey = PeriodKey("26AF")
  private val differentPeriodKey = PeriodKey("26AG")
  private val testFromDate = LocalDate.of(2026, 6, 1)
  private val testToDate = LocalDate.of(2026, 6, 30)
  private val testDueDate = LocalDate.of(2026, 7, 31)

  private def createObligation(periodKeyValue: String, status: ObligationStatus = ObligationStatus.O): ObligationItem =
    ObligationItem(
      identification = None,
      obligationDetails = ObligationDetails(
        openOrFulfilledStatus = status.toString,
        iCFromDate = testFromDate,
        iCToDate = testToDate,
        iCDateReceived = None,
        iCDueDate = testDueDate,
        periodKey = periodKeyValue
      )
    )

  "TaskListPageViewModel" - {

    "must create a view model successfully when matching obligation exists" in {
      val obligation = createObligation(testPeriodKey.value)
      val obligations = Seq(obligation)

      val result = TaskListPageViewModel(returnsUserAnswers, obligations, testPeriodKey, returnsDateUtils)

      result.returnPeriod must not be empty
      result.year must not be empty
      result.dueDate must not be empty
    }

    "must create a view model successfully when multiple obligations exist with matching period key" in {
      val matchingObligation = createObligation(testPeriodKey.value)
      val otherObligation = createObligation(differentPeriodKey.value)
      val obligations = Seq(otherObligation, matchingObligation)

      val result = TaskListPageViewModel(returnsUserAnswers, obligations, testPeriodKey, returnsDateUtils)

      result.returnPeriod must not be empty
      result.year must not be empty
      result.dueDate must not be empty
    }

    "must throw IllegalStateException when no matching obligation exists" in {
      val obligation = createObligation(differentPeriodKey.value)
      val obligations = Seq(obligation)

      val exception = intercept[IllegalStateException] {
        TaskListPageViewModel(returnsUserAnswers, obligations, testPeriodKey, returnsDateUtils)
      }

      exception.getMessage must include(s"No obligation found for period key: ${testPeriodKey.value}")
    }

    "must throw IllegalStateException when obligations list is empty" in {
      val obligations = Seq.empty[ObligationItem]

      val exception = intercept[IllegalStateException] {
        TaskListPageViewModel(returnsUserAnswers, obligations, testPeriodKey, returnsDateUtils)
      }

      exception.getMessage must include(s"No obligation found for period key: ${testPeriodKey.value}")
    }
  }
}