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
import pages.returns.EnterDutyAmountPage
import viewmodels.returns.submit.CheckYourAnswersViewModel

class CheckYourAnswersViewModelSpec extends SpecBase {

  "CheckYourAnswersViewModel" - {

    "must create view model with correct duty due" in {
      val userAnswers = returnsUserAnswers.set(EnterDutyAmountPage, BigDecimal(1000)).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, testDutyRate, periodKey)

      vm.dutyDue mustBe "£3,150"
      vm.dutyRate mustBe "£3.15"
    }

    "must create view model with zero duty when no amount entered" in {
      val vm = CheckYourAnswersViewModel(returnsUserAnswers, testDutyRate, periodKey)

      vm.dutyDue mustBe "£0"
      vm.dutyRate mustBe "£3.15"
    }

    "must create view model with correct summary lists" in {
      val userAnswers = returnsUserAnswers.set(EnterDutyAmountPage, BigDecimal(500)).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, testDutyRate, periodKey)

      vm.finalDutySummaryList.rows must not be empty
      vm.dutySuspendedSummaryList.rows must not be empty
      vm.dutyDue mustBe "£1,575"
    }
  }
}
