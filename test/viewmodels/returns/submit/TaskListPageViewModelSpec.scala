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
import models.obligations.ObligationDetails
import utils.ReturnsDateUtils

class TaskListPageViewModelSpec extends SpecBase {

  private val app = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()
  private implicit val returnsDateUtils: ReturnsDateUtils = app.injector.instanceOf[ReturnsDateUtils]

  "TaskListPageViewModel" - {

    "must create a view model successfully when matching obligation exists" in {
      val obligations = Seq(openObligation(june2026))

      val result = TaskListPageViewModel(returnsUserAnswers, obligations, june2026, returnsDateUtils)

      result.returnPeriod must not be empty
      result.year must not be empty
      result.dueDate must not be empty
    }

    "must create a view model successfully when multiple obligations exist with matching period key" in {
      val obligations = Seq(openObligation(june2026), openObligation(july2026))

      val result = TaskListPageViewModel(returnsUserAnswers, obligations, june2026, returnsDateUtils)

      result.returnPeriod must not be empty
      result.year must not be empty
      result.dueDate must not be empty
    }

    "must throw IllegalStateException when no matching obligation exists" in {
      val obligations = Seq(openObligation(july2026))

      val exception = intercept[IllegalStateException] {
        TaskListPageViewModel(returnsUserAnswers, obligations, june2026, returnsDateUtils)
      }

      exception.getMessage must include(s"No obligation found for period key: ${june2026.value}")
    }

    "must throw IllegalStateException when obligations list is empty" in {
      val obligations = Seq.empty[ObligationDetails]

      val exception = intercept[IllegalStateException] {
        TaskListPageViewModel(returnsUserAnswers, obligations, june2026, returnsDateUtils)
      }

      exception.getMessage must include(s"No obligation found for period key: ${june2026.value}")
    }
  }
}