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
import models.payments.PaymentsResponse

class ViewPaymentsViewModelSpec extends SpecBase {

  "ViewPaymentsViewModel" - {
    "when payments exist" - {
      "must format total owed correctly from outstanding payments only" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentDue), Seq.empty, Seq.empty))
        vm.totalOwed mustBe "£330,000"
      }

      "must build a table row for each outstanding payment status" in {
        ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentDue), Seq.empty, Seq.empty)).outstandingRows must have size 1
        ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentOverdue), Seq.empty, Seq.empty)).outstandingRows must have size 1
        ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentNothingToPay), Seq.empty, Seq.empty)).outstandingRows must have size 1
      }

      "must sum multiple outstanding payments correctly" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentDue, testPaymentOverdue), Seq.empty, Seq.empty))
        vm.totalOwed mustBe "£497,000.80"
        vm.outstandingRows must have size 2
      }

      "must build a table row for each unallocated payment" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq.empty, Seq(testUnallocatedPayment), Seq.empty))
        vm.unallocatedRows must have size 1
      }

      "must build a table row for each cleared payment" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq.empty, Seq.empty, Seq(testClearedPayment)))
        vm.clearedRows must have size 1
      }

      "must build rows for all three sections independently when all are populated" in {
        val vm = ViewPaymentsViewModel(testPaymentsResponse)
        vm.outstandingRows must have size 1
        vm.unallocatedRows must have size 1
        vm.clearedRows must have size 1
      }
    }

    "when no payments exist" - {
      "must show £0 as total owed" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse.empty)
        vm.totalOwed mustBe "£0"
      }

      "must have no rows in any section" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse.empty)
        vm.outstandingRows mustBe Seq.empty
        vm.unallocatedRows mustBe Seq.empty
        vm.clearedRows mustBe Seq.empty
      }
    }

    "when only some sections have data" - {
      "must independently reflect emptiness per section" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentDue), Seq.empty, Seq(testClearedPayment)))
        vm.outstandingRows must have size 1
        vm.unallocatedRows mustBe Seq.empty
        vm.clearedRows must have size 1
      }
    }
  }
}
