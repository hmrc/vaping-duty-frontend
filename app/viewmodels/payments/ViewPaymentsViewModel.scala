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

import models.payments.{ClearedPayment, OutstandingPayment, PaymentOnAccount, PaymentsResponse}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, TableRow, Tag, Text}
import uk.gov.hmrc.govukfrontend.views.html.components.GovukTag
import uk.gov.hmrc.vapingdutyfinance.models.PaymentStatus
import utils.{CurrencyFormatter, ReturnsDateUtils}

import java.time.LocalDate

final case class ViewPaymentsViewModel(
  totalOwed: String,
  outstandingRows: Seq[Seq[TableRow]],
  paymentOnAccountRows: Seq[Seq[TableRow]],
  clearedRows: Seq[Seq[TableRow]],
  clearedPaymentsYear: String
)

object ViewPaymentsViewModel {
  private val TAG_STYLE_LIGHT_BLUE = "govuk-tag--light-blue"
  private val TAG_STYLE_RED = "govuk-tag--red"
  private val TAG_STYLE_GREEN = "govuk-tag--green"
  private val NOT_AVAILABLE = "N/A"

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
        classes = "govuk-table__header"
      ),
      TableRow(
        content = HtmlContent(
          s"""${messages("payments.viewPayments.table.description.text")}<br>${messages("payments.viewPayments.table.chargeReference")}: ${payment.chargeReference}"""
        )
      ),
      TableRow(
        content = Text(CurrencyFormatter.currencyFormat(payment.amountDue)),
        classes = "govuk-table__cell--numeric"
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
          s"""<a href="#" class="govuk-link no-wrap-link">${messages("payments.viewPayments.table.payNow")}</a>"""
        )
      )
    )

  private def buildPaymentOnAccountRow(payment: PaymentOnAccount, returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): Seq[TableRow] =
    Seq(
      TableRow(
        content = Text(formatDateWithTranslatedMonth(payment.paymentDate, returnsDateUtils)),
        classes = "govuk-table__header"
      ),
      TableRow(content = Text(messages("payments.viewPayments.unallocated.description.placeholder"))),
      TableRow(
        content = Text(CurrencyFormatter.currencyFormat(payment.amount)),
        classes = "govuk-table__cell--numeric"
      ),
      TableRow(
        content = HtmlContent(
          s"""<a href="#" class="govuk-link">${messages("payments.viewPayments.unallocated.action.claimRepayment")}</a>"""
        )
      )
    )

  private def buildClearedRow(payment: ClearedPayment, returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): Seq[TableRow] =
    Seq(
      TableRow(
        content = Text(formatMonthOnly(payment.clearedDate, returnsDateUtils)),
        classes = "govuk-table__header"
      ),
      TableRow(
        content = HtmlContent(
          s"""${messages("payments.viewPayments.cleared.description.text")}<br>${messages("payments.viewPayments.table.chargeReference")}: ${payment.chargeReference}"""
        )
      ),
      TableRow(
        content = Text(CurrencyFormatter.currencyFormat(payment.amountPaid)),
        classes = "govuk-table__cell--numeric"
      )
    )

  private def formatDateWithTranslatedMonth(date: LocalDate, returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): String = {
    val month = date.getMonth
    val monthName = returnsDateUtils.getMonthMessage(month)
    s"${date.getDayOfMonth} $monthName ${date.getYear}"
  }

  private def formatDateWithTranslatedMonth(dateOpt: Option[LocalDate], returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): String =
    dateOpt.map(date => formatDateWithTranslatedMonth(date, returnsDateUtils)).getOrElse(NOT_AVAILABLE)

  private def formatMonthOnly(date: LocalDate, returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): String = {
    val month = date.getMonth
    returnsDateUtils.getMonthMessage(month)
  }

  private def formatMonthOnly(dateOpt: Option[LocalDate], returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): String =
    dateOpt.map(date => formatMonthOnly(date, returnsDateUtils)).getOrElse(NOT_AVAILABLE)

  private def statusMessageKey(status: PaymentStatus): String = status match {
    case PaymentStatus.Due => "payments.viewPayments.status.due"
    case PaymentStatus.Overdue => "payments.viewPayments.status.overdue"
    case PaymentStatus.NothingToPay => "payments.viewPayments.status.nothingToPay"
  }

  private def statusTagStyle(status: PaymentStatus): String = status match {
    case PaymentStatus.Due => TAG_STYLE_LIGHT_BLUE
    case PaymentStatus.Overdue => TAG_STYLE_RED
    case PaymentStatus.NothingToPay => TAG_STYLE_GREEN
  }
}
