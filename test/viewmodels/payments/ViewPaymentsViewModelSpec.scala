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
import models.payments.{ClearedPayment, OutstandingPayment, PaymentOnAccount, PaymentsResponse}
import uk.gov.hmrc.vapingdutyfinance.models.PaymentStatus

import java.time.LocalDate

class ViewPaymentsViewModelSpec extends SpecBase {

  private val testPaymentDue = OutstandingPayment(
    chargeReference = "XA123456789012",
    amountDue = BigDecimal("330000.00"),
    dueDate = LocalDate.of(2025, 3, 25),
    status = PaymentStatus.Due
  )

  private val testPaymentOverdue = OutstandingPayment(
    chargeReference = "XB123456789012",
    amountDue = BigDecimal("167000.80"),
    dueDate = LocalDate.of(2025, 2, 25),
    status = PaymentStatus.Overdue
  )

  private val testPaymentNothingToPay = OutstandingPayment(
    chargeReference = "XC123456789012",
    amountDue = BigDecimal("0.00"),
    dueDate = LocalDate.of(2025, 4, 25),
    status = PaymentStatus.NothingToPay
  )

  private val testUnallocatedPayment = PaymentOnAccount(
    paymentReference = Some("PAY123456"),
    amount = BigDecimal("5000.00"),
    paymentDate = Some(LocalDate.of(2025, 1, 15))
  )

  private val testClearedPayment = ClearedPayment(
    chargeReference = "XD123456789012",
    periodFromDate = Some(LocalDate.of(2024, 12, 1)),
    periodToDate = Some(LocalDate.of(2024, 12, 31)),
    amountPaid = BigDecimal("10000.00"),
    clearedDate = Some(LocalDate.of(2025, 1, 10))
  )

  private val testPaymentsResponse = PaymentsResponse(
    Seq(testPaymentDue),
    Seq(testUnallocatedPayment),
    Seq(testClearedPayment),
    None
  )

  "ViewPaymentsViewModel" - {
    "when payments exist" - {
      "must format total owed correctly from outstanding payments only" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentDue), Seq.empty, Seq.empty, Some(BigDecimal("330000.00"))))
        vm.totalOwed mustBe "£330,000"
      }

      "must build a table row for each outstanding payment status" in {
        ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentDue), Seq.empty, Seq.empty, None)).outstandingRows must have size 1
        ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentOverdue), Seq.empty, Seq.empty, None)).outstandingRows must have size 1
        ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentNothingToPay), Seq.empty, Seq.empty, None)).outstandingRows must have size 1
      }

      "must sum multiple outstanding payments correctly" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentDue, testPaymentOverdue), Seq.empty, Seq.empty, Some(BigDecimal("497000.80"))))
        vm.totalOwed mustBe "£497,000.80"
        vm.outstandingRows must have size 2
      }

      "must build a table row for each payment on account" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq.empty, Seq(testUnallocatedPayment), Seq.empty, None))
        vm.paymentOnAccountRows must have size 1
      }

      "must build a table row for each cleared payment" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq.empty, Seq.empty, Seq(testClearedPayment), None))
        vm.clearedRows must have size 1
      }

      "must build rows for all three sections independently when all are populated" in {
        val vm = ViewPaymentsViewModel(testPaymentsResponse)
        vm.outstandingRows must have size 1
        vm.paymentOnAccountRows must have size 1
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
        vm.paymentOnAccountRows mustBe Seq.empty
        vm.clearedRows mustBe Seq.empty
      }
    }

    "when only some sections have data" - {
      "must independently reflect emptiness per section" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentDue), Seq.empty, Seq(testClearedPayment), None))
        vm.outstandingRows must have size 1
        vm.paymentOnAccountRows mustBe Seq.empty
        vm.clearedRows must have size 1
      }
    }
  }
}
