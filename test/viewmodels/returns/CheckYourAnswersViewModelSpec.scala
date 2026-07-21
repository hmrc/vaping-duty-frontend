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
import models.returns.DutyRate
import pages.returns.{DeclareDutyPage, DeclareDutySuspensePage, DeclareSpoiltProductsPage, EnterDutyAmountPage}
import utils.ReturnsDateUtils
import viewmodels.returns.submit.CheckYourAnswersViewModel

class CheckYourAnswersViewModelSpec extends SpecBase {

  private val returnsDateUtils = new ReturnsDateUtils(clock)

  "CheckYourAnswersViewModel" - {

    val dutyRates = Map(periodKey -> DutyRate(315))
    
    "must create view model with correct duty due" in {
      val userAnswers = returnsUserAnswers.set(EnterDutyAmountPage, BigDecimal(1000)).success.value
      
      val vm = CheckYourAnswersViewModel(userAnswers, dutyRates, periodKey, returnsDateUtils)

      vm.dutyDue mustBe "£315"
      vm.dutyCalculationParagraph.toString must include("£3.15")
      vm.dutyRateParagraph.toString mustBe ""
    }

    "must create view model with zero duty when no amount entered" in {
      val dutyRates = Map(periodKey -> DutyRate(315))
      val vm = CheckYourAnswersViewModel(returnsUserAnswers, dutyRates, periodKey, returnsDateUtils)

      vm.dutyDue mustBe "£0"
      vm.dutyCalculationParagraph.toString must include("£3.15")
      vm.dutyRateParagraph.toString must include("You declared no vaping products had been released for consumption in the UK for this period, so no duty is due.")
    }

    "must set nilReturn to true when all declaration pages are false" in {
      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, false).success.value
        .set(DeclareSpoiltProductsPage, false).success.value
        .set(DeclareDutySuspensePage, false).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, dutyRates, periodKey, returnsDateUtils)

      vm.nilReturn mustBe true
    }

    "must set nilReturn to false when DeclareDutyPage is true" in {
      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, true).success.value
        .set(DeclareSpoiltProductsPage, false).success.value
        .set(DeclareDutySuspensePage, false).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, dutyRates, periodKey, returnsDateUtils)

      vm.nilReturn mustBe false
    }

    "must set nilReturn to false when DeclareSpoiltProductsPage is true" in {
      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, false).success.value
        .set(DeclareSpoiltProductsPage, true).success.value
        .set(DeclareDutySuspensePage, false).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, dutyRates, periodKey, returnsDateUtils)

      vm.nilReturn mustBe false
    }
  }
}
