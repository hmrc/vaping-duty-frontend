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
import models.NormalMode
import pages.returns.EnterDutyAmountPage

class DeclareDutyCheckAnswersViewModelSpec extends SpecBase {

  private val dutyRate = BigDecimal("2.20")
  private val volumeInMl = 1000

  "DeclareDutyCheckAnswersViewModel" - {

    "must calculate duty correctly" in {
      val ua = returnsUserAnswers.set(EnterDutyAmountPage, volumeInMl).success.value
      val vm = DeclareDutyCheckAnswersViewModel(ua, dutyRate, periodKey)

      vm.dutyDue mustBe "£2,200"
    }

    "must handle zero volume" in {
      val ua = returnsUserAnswers.set(EnterDutyAmountPage, 0).success.value
      val vm = DeclareDutyCheckAnswersViewModel(ua, dutyRate, periodKey)

      vm.volumeFormatted mustBe "0 ml"
      vm.dutyDue mustBe "£0"
    }

    "must handle missing volume" in {
      val vm = DeclareDutyCheckAnswersViewModel(returnsUserAnswers, dutyRate, periodKey)

      vm.volumeFormatted mustBe "0 ml"
      vm.dutyDue mustBe "£0"
    }

    "must create summary list with two rows" in {
      val ua = returnsUserAnswers.set(EnterDutyAmountPage, volumeInMl).success.value
      val vm = DeclareDutyCheckAnswersViewModel(ua, dutyRate, periodKey)

      vm.summaryList.rows.size mustBe 2
    }

    "must have volume row with Change link in NormalMode" in {
      val ua = returnsUserAnswers.set(EnterDutyAmountPage, volumeInMl).success.value
      val vm = DeclareDutyCheckAnswersViewModel(ua, dutyRate, periodKey)

      val volumeRow = vm.summaryList.rows.head
      volumeRow.key.content.asHtml.toString must include("Volume")
      volumeRow.value.content.asHtml.toString must include("1000 ml")
      volumeRow.actions.value.items.size mustBe 1
      
      val changeLink = volumeRow.actions.value.items.head
      changeLink.href must include(controllers.returns.submit.routes.EnterDutyAmountController.onPageLoad(NormalMode).url)
      changeLink.href must include(s"period=${periodKey.value}")
    }
  }
}