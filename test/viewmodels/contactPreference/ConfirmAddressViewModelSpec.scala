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

package viewmodels.contactPreference

import base.{SpecBase, UnitSpec}
import data.TestData
import org.mockito.Mockito.when


class ConfirmAddressViewModelSpec extends UnitSpec with SpecBase with TestData {

  "ConfirmAddressViewModel" - {

    "changeAddressUrl field should return the correct url from config" in {

      when(mockAppConfig.changeAddressGuidanceUrl).thenReturn("changeAddressGuidanceUrl")

      val vm = ConfirmAddressViewModel(mockAppConfig, userAnswers.subscriptionSummary.correspondenceAddress)

      val result = vm.changeAddressUrl

      result mustBe "changeAddressGuidanceUrl"
    }

    "address field should return list with the address lines in elements of the list" in {

      val vm = ConfirmAddressViewModel(mockAppConfig, userAnswers.subscriptionSummary.correspondenceAddress)

      val result = vm.address

      result mustBe Seq("Flat 123", "1 Example Road", "London", "AB1 2CD")

    }
  }
}
