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
import models.payments.OutstandingPayment
import uk.gov.hmrc.govukfrontend.views.Aliases.Text

class ViewPaymentsViewModelSpec extends SpecBase {

  val testPaymentDue = OutstandingPayment(
    chargeReference = "VPD38270541977",
    period = "December 2026",
    amountDue = BigDecimal("330000.00"),
    dueDate = "2026-12-15",
    status = "Due"
  )

  val testPaymentOverdue = OutstandingPayment(
    chargeReference = "VPD38270541978",
    period = "November 2026",
    amountDue = BigDecimal("167000.80"),
    dueDate = "2026-11-15",
    status = "Overdue"
  )

  val testPaymentNothingToPay = OutstandingPayment(
    chargeReference = "VPD38270541979",
    period = "October 2026",
    amountDue = BigDecimal("0.00"),
    dueDate = "2026-10-15",
    status = "Nothing to pay"
  )

  "ViewPaymentsViewModel" - {
    "when payment exists" - {
      "must format total owed correctly" in {
        val vm = ViewPaymentsViewModel(Some(testPaymentDue))
        vm.totalOwed mustBe "£330,000"
      }

      "must create payment display data" in {
        val vm = ViewPaymentsViewModel(Some(testPaymentDue))
        vm.payment mustBe defined
        vm.payment.get.chargeReference mustBe "VPD38270541977"
        vm.payment.get.amountDue mustBe "£330,000"
        vm.payment.get.status mustBe "Due"
      }

      "must format date correctly" in {
        val vm = ViewPaymentsViewModel(Some(testPaymentDue))
        vm.payment.get.dueDate mustBe "15 December 2026"
      }

      "must apply correct tag style for Due status" in {
        val vm = ViewPaymentsViewModel(Some(testPaymentDue))
        vm.payment.get.statusTagStyle mustBe "govuk-tag--light-blue"
      }

      "must apply correct tag style for Overdue status" in {
        val vm = ViewPaymentsViewModel(Some(testPaymentOverdue))
        vm.payment.get.statusTagStyle mustBe "govuk-tag--red"
      }

      "must apply correct tag style for Nothing to pay status" in {
        val vm = ViewPaymentsViewModel(Some(testPaymentNothingToPay))
        vm.payment.get.statusTagStyle mustBe "govuk-tag--green"
      }

      "must handle unknown status" in {
        val unknownStatusPayment = testPaymentDue.copy(status = "Unknown")
        val vm = ViewPaymentsViewModel(Some(unknownStatusPayment))
        vm.payment.get.statusTagStyle mustBe ""
      }

      "must build a single table row group containing the formatted payment details" in {
        val vm = ViewPaymentsViewModel(Some(testPaymentDue))
        vm.paymentRows must have size 1
        val row = vm.paymentRows.head
        row must have size 5
        row.head.content mustBe Text("15 December 2026")
      }
    }

    "when no payment exists" - {
      "must show £0 as total owed" in {
        val vm = ViewPaymentsViewModel(None)
        vm.totalOwed mustBe "£0"
      }

      "must have no payment data" in {
        val vm = ViewPaymentsViewModel(None)
        vm.payment mustBe None
      }

      "must have no table rows" in {
        val vm = ViewPaymentsViewModel(None)
        vm.paymentRows mustBe Seq.empty
      }
    }

    "must handle invalid date format gracefully" in {
      val invalidDatePayment = testPaymentDue.copy(dueDate = "invalid-date")
      val vm = ViewPaymentsViewModel(Some(invalidDatePayment))
      vm.payment.get.dueDate mustBe "invalid-date"
    }
  }
}