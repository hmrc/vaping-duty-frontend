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
import models.returns.ReturnsUserAnswers
import pages.returns.{DeclareDutyPage, EnterDutyAmountPage}
import play.api.libs.json.JsObject

import java.time.Instant

class CheckYourAnswersViewModelSpec extends SpecBase {
  
  "CheckYourAnswersViewModel" - {

    "must create a view model with both summary lists" in {
      val ua = ReturnsUserAnswers("id", JsObject.empty, Instant.now(), Instant.now())
        .set(DeclareDutyPage, true).success.value

      val vm = CheckYourAnswersViewModel(ua)

      vm.finalDutySummaryList.rows must not be empty
      // Commented until we show these rows
      //vm.dutySuspendedSummaryList.rows must not be empty
    }

    "must create a view model with empty summary lists when no data exists" in {
      val ua = ReturnsUserAnswers("id", JsObject.empty, Instant.now(), Instant.now())

      val vm = CheckYourAnswersViewModel(ua)

      // Will always show two rows at least
      vm.finalDutySummaryList.rows.size mustBe 2
      vm.dutySuspendedSummaryList.rows mustBe empty
    }

    "must filter out None values from summary rows" in {
      val ua = ReturnsUserAnswers("id", JsObject.empty, Instant.now(), Instant.now())
        .set(DeclareDutyPage, true)
        .flatMap(_.set(EnterDutyAmountPage, 1000)).success.value

      val vm = CheckYourAnswersViewModel(ua)

      // Will always show two rows at least
      vm.finalDutySummaryList.rows.size mustBe 2
      // Commented until we show these rows
      //vm.dutySuspendedSummaryList.rows.size mustBe 3
    }
  }
}
