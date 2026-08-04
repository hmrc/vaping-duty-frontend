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

import controllers.payments.routes
import models.payments.{ClearedPayment, OutstandingPayment, PaymentOnAccount, PaymentsResponse}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, TableRow, Tag, Text}
import uk.gov.hmrc.govukfrontend.views.html.components.GovukTag
import uk.gov.hmrc.vapingdutyfinance.models.PaymentStatus
import utils.{CssConstants, CurrencyFormatter, ReturnsDateUtils}

import java.time.LocalDate

final case class ViewPaymentsViewModel(
  totalOwed: String,
  outstandingRows: Seq[Seq[TableRow]],
  paymentOnAccountRows: Seq[Seq[TableRow]],
  clearedRows: Seq[Seq[TableRow]],
  clearedPaymentsYear: String
)

object ViewPaymentsViewModel {
  private val govukTag = GovukTag()

  def apply(payments: PaymentsResponse, returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): ViewPaymentsViewModel = {
    val totalOwed = payments.totalAccountBalance.getOrElse(BigDecimal(0))
    val clearedYear = returnsDateUtils.getYear.toString

    ViewPaymentsViewModel(
      totalOwed = CurrencyFormatter.currencyFormat(totalOwed),
      outstandingRows = payments.outstanding.map(buildOutstandingRow(_, returnsDateUtils)),
      paymentOnAccountRows = payments.paymentOnAccount.map(buildPaymentOnAccountRow(_, returnsDateUtils)),
      clearedRows = payments.cleared.map(buildClearedRow(_, returnsDateUtils)),
      clearedPaymentsYear = clearedYear
    )
  }

  private def buildOutstandingRow(payment: OutstandingPayment, returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): Seq[TableRow] =
    Seq(
      TableRow(
        content = Text(formatDateWithTranslatedMonth(Some(payment.dueDate), returnsDateUtils)),
        classes = CssConstants.tableHeader
      ),
      TableRow(
        content = HtmlContent(
          s"""${messages("payments.viewPayments.table.description.text")}<br>${messages("payments.viewPayments.table.chargeReference")}: ${payment.chargeReference}"""
        )
      ),
      TableRow(
        content = Text(CurrencyFormatter.currencyFormat(payment.amountDue)),
        classes = CssConstants.tableCellNumeric
      ),
      TableRow(
        content = HtmlContent(
          govukTag(Tag(
            content = Text(messages(statusMessageKey(payment.status))),
            classes = statusTagStyle(payment.status)
          ))
        )
      ),
      TableRow(
        content = HtmlContent(
          s"""<a href="${routes.StartPaymentController.startPayment(payment.chargeReference)}" class="${CssConstants.link}">${messages("payments.viewPayments.table.payNow")}</a>"""
        )
      )
    )

  private def buildPaymentOnAccountRow(payment: PaymentOnAccount, returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): Seq[TableRow] =
    Seq(
      TableRow(
        content = Text(formatDateWithTranslatedMonth(payment.paymentDate, returnsDateUtils)),
        classes = CssConstants.tableHeader
      ),
      TableRow(content = Text(messages("payments.viewPayments.unallocated.description.placeholder"))),
      TableRow(
        content = Text(CurrencyFormatter.currencyFormat(payment.amount)),
        classes = CssConstants.tableCellNumeric
      ),
      TableRow(
        content = HtmlContent(
          s"""<a href="#" class="${CssConstants.link}">${messages("payments.viewPayments.unallocated.action.claimRepayment")}</a>"""
        )
      )
    )

  private def buildClearedRow(payment: ClearedPayment, returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): Seq[TableRow] =
    Seq(
      TableRow(
        content = Text(formatMonthOnly(payment.clearedDate, returnsDateUtils)),
        classes = CssConstants.tableHeader
      ),
      TableRow(
        content = HtmlContent(
          s"""${messages("payments.viewPayments.cleared.description.text")}<br>${messages("payments.viewPayments.table.chargeReference")}: ${payment.chargeReference}"""
        )
      ),
      TableRow(
        content = Text(CurrencyFormatter.currencyFormat(payment.amountPaid)),
        classes = CssConstants.tableCellNumeric
      )
    )

  private def formatDateWithTranslatedMonth(dateOpt: Option[LocalDate], returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): String =
    dateOpt.fold(messages("payments.viewPayments.notAvailable")) { date =>
      val monthName = returnsDateUtils.getMonthMessage(date.getMonth)
      s"${date.getDayOfMonth} $monthName ${date.getYear}"
    }

  private def formatMonthOnly(dateOpt: Option[LocalDate], returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): String =
    dateOpt.fold(messages("payments.viewPayments.notAvailable")) { date =>
      returnsDateUtils.getMonthMessage(date.getMonth)
    }

  private def statusMessageKey(status: PaymentStatus): String = status match {
    case PaymentStatus.Due => "payments.viewPayments.status.due"
    case PaymentStatus.Overdue => "payments.viewPayments.status.overdue"
    case PaymentStatus.NothingToPay => "payments.viewPayments.status.nothingToPay"
  }

  private def statusTagStyle(status: PaymentStatus): String = status match {
    case PaymentStatus.Due => CssConstants.tagLightBlue
    case PaymentStatus.Overdue => CssConstants.tagRed
    case PaymentStatus.NothingToPay => CssConstants.tagGreen
  }
}
