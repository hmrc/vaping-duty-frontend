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

package viewmodels.payments

import base.SpecBase

class ViewPaymentsViewModelSpec extends SpecBase {

  "ViewPaymentsViewModel" - {
    "when payment exists" - {
      "must format total owed correctly" in {
        val vm = ViewPaymentsViewModel(Seq(testPaymentDue))
        vm.totalOwed mustBe "£330,000"
      }

      "must build a table row for each payment status" in {
        ViewPaymentsViewModel(Seq(testPaymentDue)).paymentRows must have size 1
        ViewPaymentsViewModel(Seq(testPaymentOverdue)).paymentRows must have size 1
        ViewPaymentsViewModel(Seq(testPaymentNothingToPay)).paymentRows must have size 1
      }

      "must sum multiple payments correctly" in {
        val vm = ViewPaymentsViewModel(Seq(testPaymentDue, testPaymentOverdue))
        vm.totalOwed mustBe "£497,000.80"
        vm.paymentRows must have size 2
      }
    }

    "when no payment exists" - {
      "must show £0 as total owed" in {
        val vm = ViewPaymentsViewModel(Seq.empty)
        vm.totalOwed mustBe "£0"
      }

      "must have no table rows" in {
        val vm = ViewPaymentsViewModel(Seq.empty)
        vm.paymentRows mustBe Seq.empty
      }
    }
  }
}
