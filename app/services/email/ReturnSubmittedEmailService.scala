/*
 * Copyright 2026 HM Revenue & Customs
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

package services.email

import connectors.email.EmailConnector
import models.email.*
import models.obligations.ObligationDetails
import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import play.api.Logging
import play.api.http.Status.{ACCEPTED, BAD_REQUEST}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.CurrencyFormatter

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneId}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ReturnSubmittedEmailService @Inject()(emailConnector: EmailConnector)(using ExecutionContext)
  extends Logging with CurrencyFormatter {

  private val RETURN_PERIOD_FORMATTER   = DateTimeFormatter.ofPattern("MMMM yyyy")
  private val SUBMISSION_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy")
  private val PAYMENT_DUE_FORMATTER     = DateTimeFormatter.ofPattern("d MMMM yyyy")
  private val ZONE_ID                   = ZoneId.of("Europe/London")

  def sendReturnSubmittedEmail(
    submission: ReturnCreateRequest,
    result: ReturnSubmittedResponse,
    obligation: ObligationDetails
  )(using HeaderCarrier): Future[Unit] = {

    val recipient      = List(submission.declaration.signeesEmailAddress)
    val returnPeriod   = obligation.iCFromDate.format(RETURN_PERIOD_FORMATTER)
    val submissionDate = LocalDate.ofInstant(result.processingDate, ZONE_ID).format(SUBMISSION_DATE_FORMATTER)

    val email: Email =
      if (result.amount > 0) {
        DutyDueEmail(
          to = recipient,
          parameters = DutyDueEmailParameters(
            recipientName   = submission.declaration.fullName,
            returnPeriod    = returnPeriod,
            submissionDate  = submissionDate,
            chargeReference = result.chargeReference.getOrElse(""),
            amountDue       = currencyFormat(result.amount),
            paymentDueDate  = obligation.iCDueDate.withDayOfMonth(15).format(PAYMENT_DUE_FORMATTER)
          )
        )
      } else if (result.amount < 0) {
        CreditDueEmail(
          to = recipient,
          parameters = CreditDueEmailParameters(
            recipientName  = submission.declaration.fullName,
            returnPeriod   = returnPeriod,
            submissionDate = submissionDate,
            creditAmount   = currencyFormat(result.amount.abs)
          )
        )
      } else {
        NilReturnEmail(
          to = recipient,
          parameters = NilReturnEmailParameters(
            recipientName  = submission.declaration.fullName,
            returnPeriod   = returnPeriod,
            submissionDate = submissionDate
          )
        )
      }

    sendEmail(email, emailType = "return submitted")
  }

  private def sendEmail(email: Email, emailType: String)(using HeaderCarrier): Future[Unit] =
    emailConnector
      .postEmail(email)
      .map {
        case HttpResponse(ACCEPTED, _, _)    => ()
          logger.warn(s" HMRC email service: sent $emailType confirmation email")
        case HttpResponse(BAD_REQUEST, _, _) =>
          logger.warn(s"Error from HMRC email service: status=400 sending $emailType confirmation email")
        case HttpResponse(status, _, _) =>
          logger.warn(s"Unexpected response from HMRC email service: status=$status sending $emailType confirmation email")
      }
      .recover { case NonFatal(e) =>
        logger.warn(s"Unable to send $emailType confirmation email: ${e.getClass.getSimpleName}")
      }
}
