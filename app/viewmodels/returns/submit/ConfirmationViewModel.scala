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

package viewmodels.returns.submit

import models.identifiers.PeriodKey
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.{GovukInsetText, GovukWarningText}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import uk.gov.hmrc.govukfrontend.views.viewmodels.warningtext.WarningText
import utils.{CurrencyFormatter, DateTimeFormats}
import views.html.components.{Heading2, Link, ListWithLinks, Paragraph}

import java.time.{Instant, LocalDate, ZoneId}
import java.time.format.DateTimeFormatter

case class ConfirmationViewModel(
  email: String,
  date: String,
  submissionDate: String,
  periodMonthYear: String,
  totalDutyAmount: BigDecimal,
  totalDutyFormatted: String,
  paymentDueDate: String,
  chargeReference: Option[String],
  content: Html,
  vpdRef: String,
  btaLink: String,
  periodKey: PeriodKey,
  viewReturnUrl: String,
  showEmailConfirmation: Boolean,
  showViewReturnLink: Boolean,
  showWhatYouMustDoNext: Boolean,
  showBtaButton: Boolean
)

object ConfirmationViewModel extends CurrencyFormatter {

  private val SUBMISSION_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy")
  private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy")
  private val PAYMENT_DUE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy")
  private val ZONE_ID = ZoneId.of("Europe/London")

  def apply(
    dutyDue: BigDecimal,
    email: String,
    chargeReference: Option[String],
    submissionDate: Instant,
    periodFrom: LocalDate,
    periodTo: LocalDate,
    paymentDueDate: LocalDate,
    vpdRef: String,
    btaLink: String,
    periodKey: PeriodKey,
    viewReturnUrl: String
  )(implicit messages: Messages): ConfirmationViewModel = {

    val submissionDateFormatted = LocalDate.ofInstant(submissionDate, ZONE_ID).format(SUBMISSION_DATE_FORMATTER)
    val periodMonthYearFormatted = periodFrom.format(MONTH_YEAR_FORMATTER)
    val paymentDueDateFormatted = paymentDueDate.format(PAYMENT_DUE_FORMATTER)
    val currentDateString = submissionDateFormatted
    val isNilReturn = dutyDue == 0

    new ConfirmationViewModel(
      email = email,
      date = currentDateString,
      submissionDate = submissionDateFormatted,
      periodMonthYear = periodMonthYearFormatted,
      totalDutyAmount = dutyDue,
      totalDutyFormatted = currencyFormat(dutyDue.abs),
      paymentDueDate = paymentDueDateFormatted,
      chargeReference = chargeReference,
      content = getContent(dutyDue, paymentDueDateFormatted),
      vpdRef = vpdRef,
      btaLink = btaLink,
      periodKey = periodKey,
      viewReturnUrl = viewReturnUrl,
      showEmailConfirmation = !isNilReturn,
      showViewReturnLink = !isNilReturn,
      showWhatYouMustDoNext = !isNilReturn,
      showBtaButton = !isNilReturn
    )
  }

  private def getContent(dutyDue: BigDecimal, paymentDueDate: String)(implicit messages: Messages): Html = {
    if (dutyDue > 0) {
      getPositiveContent(dutyDue, paymentDueDate)
    } else {
      getZeroContent()
    }
  }

  private def getPositiveContent(dutyDue: BigDecimal, paymentDueDate: String)(implicit messages: Messages): Html = {
    val warning = GovukWarningText()
    val p = Paragraph()
    val h2 = Heading2()
    val list = ListWithLinks()

    val warningSection = warning(WarningText(
      iconFallbackText = Some(messages("site.warning")),
      content = Text(messages("returns.confirmation.warning.youMust", currencyFormat(dutyDue), paymentDueDate))
    ))

    val directDebitParagraph = p(Seq(Text(messages("returns.confirmation.p.directDebit", paymentDueDate))))

    val whatNextHeading = h2(Text(messages("returns.confirmation.h2.whatNext")))

    val bulletList = list(Seq(
      Html(messages("returns.confirmation.bullet.payNow")),
      Html(messages("returns.confirmation.bullet.interest", paymentDueDate))
    ), classes = "govuk-list govuk-list--bullet")

    HtmlFormat.fill(Seq(warningSection, directDebitParagraph, whatNextHeading, bulletList))
  }

  private def getZeroContent()(implicit messages: Messages): Html = {
    val govukInsetText = GovukInsetText()

    val insetSection = govukInsetText(InsetText(
      content = Text(messages("returns.confirmation.inset.youHave"))
    ))

    insetSection
  }
}
