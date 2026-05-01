/*
 * Copyright 2025 HM Revenue & Customs
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

import base.{SpecBase, UnitSpec}
import models.returns.ReturnsUserAnswers
import pages.returns.EnterDutyAmountPage
import play.api.libs.json.Json
import utils.ReturnsDateUtils

import java.time.Instant


class ConfirmationViewModelSpec extends SpecBase with UnitSpec {
  
  "ConfirmationViewModel" - {

    val monthMessage = ReturnsDateUtils.getCurrentMonthMessage(ReturnsDateUtils.month)
    
    "must return the email address" in {
      val ua = ReturnsUserAnswers("id", Json.obj(), Instant.now(), Instant.now())
      val vm = ConfirmationViewModel(ua, emailAddress, vpdRef.get, btaLink)

      vm.email mustBe emailAddress
    }

    "must return the current date" in {
      val ua = ReturnsUserAnswers("id", Json.obj(), Instant.now(), Instant.now())
      val vm = ConfirmationViewModel(ua, emailAddress, vpdRef.get, btaLink)

      val expectedResult = s"${ReturnsDateUtils.getCurrentDay} $monthMessage ${ReturnsDateUtils.getYear}"

      vm.date mustBe expectedResult
    }
    
    "must return the current month from messages" in {
      val ua = ReturnsUserAnswers("id", Json.obj(), Instant.now(), Instant.now())
        .set(EnterDutyAmountPage, 1000).success.value

      val vm = ConfirmationViewModel(ua, emailAddress, vpdRef.get, btaLink)

      vm.currentMonth mustBe monthMessage
    }
  }
}
