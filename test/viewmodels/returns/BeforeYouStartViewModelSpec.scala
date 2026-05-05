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
import utils.ReturnsDateUtils.getYear
import viewmodels.returns.submit.BeforeYouStartViewModel

import java.time.format.TextStyle
import java.time.{LocalDate, Year}
import java.util.Locale


class BeforeYouStartViewModelSpec extends SpecBase with UnitSpec {
  
  "BeforeYouStartViewModel" - {

    val vm = BeforeYouStartViewModel()

    "return the correct year" in {
      val expectedResult = LocalDate.now().getYear

      vm.year mustBe expectedResult
    }

    "return the correct month length" in {
      val isLeapYear = Year.of(getYear).isLeap
      val expectedResult = LocalDate.now().getMonth.length(isLeapYear)

      vm.monthLength mustBe expectedResult
    }

    "return the correct month due" in {
      val expectedResult = LocalDate.now().getMonth.plus(1).getDisplayName(TextStyle.FULL, Locale.UK)

      vm.dueDate mustBe expectedResult
    }

    "return the correct return period month" in {
      val expectedResult = LocalDate.now().getMonth.getDisplayName(TextStyle.FULL, Locale.UK)

      vm.returnPeriod mustBe expectedResult
    }
  }
}
