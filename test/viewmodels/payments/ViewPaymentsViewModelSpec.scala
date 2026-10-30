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
import uk.gov.hmrc.vapingdutyfinance.models.{MainTransactionType, PaymentStatus}
import utils.ReturnsDateUtils

import java.time.LocalDate

class ViewPaymentsViewModelSpec extends SpecBase {

  private val returnsDateUtils = new ReturnsDateUtils(clock)
  private val repaymentGuidanceUrl = "repaymentGuidanceUrl"

  private val testPaymentDue = OutstandingPayment(
    chargeReference = "XA123456789012",
    amountDue = BigDecimal("330000.00"),
    dueDate = LocalDate.of(2025, 3, 25),
    status = PaymentStatus.Due,
    mainTransaction = MainTransactionType.Return
  )

  private val testPaymentOverdue = OutstandingPayment(
    chargeReference = "XB123456789012",
    amountDue = BigDecimal("167000.80"),
    dueDate = LocalDate.of(2025, 2, 25),
    status = PaymentStatus.Overdue,
    mainTransaction = MainTransactionType.Return
  )

  private val testPaymentNothingToPay = OutstandingPayment(
    chargeReference = "XC123456789012",
    amountDue = BigDecimal("0.00"),
    dueDate = LocalDate.of(2025, 4, 25),
    status = PaymentStatus.NothingToPay,
    mainTransaction = MainTransactionType.Return
  )

  private val testPaymentOverdueInterest = OutstandingPayment(
    chargeReference = "XE123456789012",
    amountDue = BigDecimal("123.45"),
    dueDate = LocalDate.of(2025, 2, 25),
    status = PaymentStatus.Overdue,
    mainTransaction = MainTransactionType.LatePaymentInterest
  )

  private val testPaymentRefund = OutstandingPayment(
    chargeReference = "XF123456789012",
    amountDue = BigDecimal("-2577.44"),
    dueDate = LocalDate.of(2025, 2, 25),
    status = PaymentStatus.Overdue,
    mainTransaction = MainTransactionType.Return
  )

  private val testUnallocatedPayment = PaymentOnAccount(
    amount = BigDecimal("5000.00"),
    paymentDate = Some(LocalDate.of(2025, 1, 15))
  )

  private val testClearedPayment = ClearedPayment(
    chargeReference = "XD123456789012",
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
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentDue), Seq.empty, Seq.empty, Some(BigDecimal("330000.00"))), returnsDateUtils, repaymentGuidanceUrl)
        vm.totalOwed mustBe "£330,000"
      }

      "must build a table row for each outstanding payment status" in {
        ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentDue), Seq.empty, Seq.empty, None), returnsDateUtils, repaymentGuidanceUrl).outstandingRows must have size 1
        ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentOverdue), Seq.empty, Seq.empty, None), returnsDateUtils, repaymentGuidanceUrl).outstandingRows must have size 1
        ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentNothingToPay), Seq.empty, Seq.empty, None), returnsDateUtils, repaymentGuidanceUrl).outstandingRows must have size 1
      }

      "must sum multiple outstanding payments correctly" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentDue, testPaymentOverdue), Seq.empty, Seq.empty, Some(BigDecimal("497000.80"))), returnsDateUtils, repaymentGuidanceUrl)
        vm.totalOwed mustBe "£497,000.80"
        vm.outstandingRows must have size 2
      }

      "must build a table row for each payment on account" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq.empty, Seq(testUnallocatedPayment), Seq.empty, None), returnsDateUtils, repaymentGuidanceUrl)
        vm.paymentOnAccountRows must have size 1
      }

      "must build a table row for each cleared payment" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq.empty, Seq.empty, Seq(testClearedPayment), None), returnsDateUtils, repaymentGuidanceUrl)
        vm.clearedRows must have size 1
      }

      "must build rows for all three sections independently when all are populated" in {
        val vm = ViewPaymentsViewModel(testPaymentsResponse, returnsDateUtils, repaymentGuidanceUrl)
        vm.outstandingRows must have size 1
        vm.paymentOnAccountRows must have size 1
        vm.clearedRows must have size 1
      }

      "must build unallocated payment rows with 4 columns (date, description, amount, action)" in {
        val vm = ViewPaymentsViewModel(testPaymentsResponse, returnsDateUtils, repaymentGuidanceUrl)
        vm.paymentOnAccountRows.head must have size 4
      }

      "must build cleared payment rows with 3 columns (period, description, amount)" in {
        val vm = ViewPaymentsViewModel(testPaymentsResponse, returnsDateUtils, repaymentGuidanceUrl)
        vm.clearedRows.head must have size 3
      }

      "must display the full date for cleared payment period" in {
        val vm = ViewPaymentsViewModel(testPaymentsResponse, returnsDateUtils, repaymentGuidanceUrl)
        val periodCell = vm.clearedRows.head.head
        periodCell.content.asHtml.body mustBe "10 January 2025"
      }

      "must show a bold 'Cleared payment' heading for a cleared payment" in {
        val vm = ViewPaymentsViewModel(testPaymentsResponse, returnsDateUtils, repaymentGuidanceUrl)
        val descriptionCell = vm.clearedRows.head(1)
        descriptionCell.content.asHtml.body must include("<strong>Cleared payment</strong>")
      }

      "must indicate an outstanding balance when totalAccountBalance is positive" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentDue), Seq.empty, Seq.empty, Some(BigDecimal("330000.00"))), returnsDateUtils, repaymentGuidanceUrl)
        vm.hasOutstandingBalance mustBe true
      }

      "must show a bold 'Late payment interest' heading for an outstanding payment with MainTransactionType.LatePaymentInterest" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentOverdueInterest), Seq.empty, Seq.empty, None), returnsDateUtils, repaymentGuidanceUrl)
        val descriptionCell = vm.outstandingRows.head(1)
        descriptionCell.content.asHtml.body must include("<strong>Late payment interest</strong>")
      }

      "must show the normal bold payment heading for an outstanding payment with MainTransactionType.Return" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentOverdue), Seq.empty, Seq.empty, None), returnsDateUtils, repaymentGuidanceUrl)
        val descriptionCell = vm.outstandingRows.head(1)
        descriptionCell.content.asHtml.body must include("<strong>Payment</strong>")
        descriptionCell.content.asHtml.body must not include "Late payment interest"
      }

      "must build a refund row for an outstanding payment with a negative amount" in {
        val vm  = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentRefund), Seq.empty, Seq.empty, None), returnsDateUtils, repaymentGuidanceUrl)
        val row = vm.outstandingRows.head

        row.head.content.asHtml.body mustBe "Not applicable"
        row(1).content.asHtml.body must include("<strong>Repayment</strong>")
        row(1).content.asHtml.body must include("XF123456789012")
        row(2).content.asHtml.body mustBe "-£2,577.44"
        row(2).classes must include("app-table__cell--nowrap")
        row(3).content.asHtml.body must include("Nothing to pay")
        row(3).content.asHtml.body must include("govuk-tag--grey")
        row(4).content.asHtml.body must include("Claim repayment")
        row(4).content.asHtml.body must include(s"""href="$repaymentGuidanceUrl"""")
        row(4).content.asHtml.body must not include "Pay now"
      }

      "must keep payment description and pay now link for an outstanding payment with a positive amount" in {
        val vm  = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentOverdue), Seq.empty, Seq.empty, None), returnsDateUtils, repaymentGuidanceUrl)
        val row = vm.outstandingRows.head

        row.head.content.asHtml.body mustBe "25 February 2025"
        row(1).content.asHtml.body must include("<strong>Payment</strong>")
        row(2).content.asHtml.body mustBe "£167,000.80"
        row(3).content.asHtml.body must include("Overdue")
        row(4).content.asHtml.body must include("Pay now")
        row(4).content.asHtml.body must not include "Claim refund"
      }
    }

    "when no payments exist" - {
      "must show £0 as total owed" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse.empty, returnsDateUtils, repaymentGuidanceUrl)
        vm.totalOwed mustBe "£0"
      }

      "must indicate no outstanding balance" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse.empty, returnsDateUtils, repaymentGuidanceUrl)
        vm.hasOutstandingBalance mustBe false
      }

      "must have no rows in any section" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse.empty, returnsDateUtils, repaymentGuidanceUrl)
        vm.outstandingRows mustBe Seq.empty
        vm.paymentOnAccountRows mustBe Seq.empty
        vm.clearedRows mustBe Seq.empty
      }

      "must use current year when no cleared payments exist" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse.empty, returnsDateUtils, repaymentGuidanceUrl)
        vm.clearedPaymentsYear mustBe LocalDate.now(clock).getYear.toString
      }
    }

    "when only some sections have data" - {
      "must independently reflect emptiness per section" in {
        val vm = ViewPaymentsViewModel(PaymentsResponse(Seq(testPaymentDue), Seq.empty, Seq(testClearedPayment), None), returnsDateUtils, repaymentGuidanceUrl)
        vm.outstandingRows must have size 1
        vm.paymentOnAccountRows mustBe Seq.empty
        vm.clearedRows must have size 1
      }
    }
  }
}
