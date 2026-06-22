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

import models.payments.OutstandingPayment
import utils.CurrencyFormatter

import java.time.LocalDate
import java.time.format.DateTimeFormatter

final case class ViewPaymentsViewModel(
  totalOwed: String,
  payment: Option[PaymentDisplayData]
)

object ViewPaymentsViewModel {
  private val PAYMENT_STATUS_DUE = "Due"
  private val PAYMENT_STATUS_OVERDUE = "Overdue"
  private val PAYMENT_STATUS_NOTHING_TO_PAY = "Nothing to pay"
  
  private val TAG_STYLE_LIGHT_BLUE = "govuk-tag--light-blue"
  private val TAG_STYLE_RED = "govuk-tag--red"
  private val TAG_STYLE_GREEN = "govuk-tag--green"
  
  private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy")

  def apply(paymentOption: Option[OutstandingPayment]): ViewPaymentsViewModel = {
    val totalOwed = paymentOption
      .map(p => CurrencyFormatter.currencyFormat(p.amountDue))
      .getOrElse("£0")

    val paymentData = paymentOption.map { payment =>
      PaymentDisplayData(
        dueDate = formatDate(payment.dueDate),
        chargeReference = payment.chargeReference,
        period = payment.period,
        amountDue = CurrencyFormatter.currencyFormat(payment.amountDue),
        status = payment.status,
        statusTagStyle = getStatusTagStyle(payment.status)
      )
    }

    ViewPaymentsViewModel(totalOwed, paymentData)
  }

  private def formatDate(dateString: String): String = {
    try {
      val date = LocalDate.parse(dateString)
      date.format(DATE_FORMATTER)
    } catch {
      case _: Exception => dateString
    }
  }

  private def getStatusTagStyle(status: String): String = status match {
    case PAYMENT_STATUS_DUE => TAG_STYLE_LIGHT_BLUE
    case PAYMENT_STATUS_OVERDUE => TAG_STYLE_RED
    case PAYMENT_STATUS_NOTHING_TO_PAY => TAG_STYLE_GREEN
    case _ => ""
  }
}

final case class PaymentDisplayData(
  dueDate: String,
  chargeReference: String,
  period: String,
  amountDue: String,
  status: String,
  statusTagStyle: String
)