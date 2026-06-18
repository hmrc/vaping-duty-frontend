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
import models.obligations.ObligationDetails
import models.returns.view.ReturnDisplayResponse
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.{GovukInsetText, GovukWarningText}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import uk.gov.hmrc.govukfrontend.views.viewmodels.warningtext.WarningText
import utils.CurrencyFormatter
import views.html.components.{Heading2, Link, ListWithLinks, Paragraph}

import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter

case class ConfirmationViewModel(
  submissionDate: String,
  periodMonthYear: String,
  totalDutyAmount: BigDecimal,
  totalDutyFormatted: String,
  paymentDueDate: String,
  chargeReference: Option[String],
  content: Html,
  btaLink: String,
  periodKey: PeriodKey,
  showWhatYouMustDoNext: Boolean
)

object ConfirmationViewModel extends CurrencyFormatter {

  private val SUBMISSION_DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy")
  private val MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy")
  private val PAYMENT_DUE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy")
  private val ZONE_ID = ZoneId.of("Europe/London")

  def apply(
    returnsResponse: ReturnDisplayResponse,
    obligation: ObligationDetails,
    btaLink: String
  )(implicit messages: Messages): ConfirmationViewModel = {

    val totalDutyDue = returnsResponse.success.totalDutyDue
      .map(_.totalDutyDue)
      .getOrElse(
        // scalafix:off DisableSyntax.throw
        throw new RuntimeException("Total duty due not found")
      )

    val chargeReference = returnsResponse.success.chargeDetails
      .flatMap(_.chargeReference)
      .map(_.toUpperCase)

    val submissionDateFormatted = LocalDate.ofInstant(
      returnsResponse.success.processingDate,
      ZONE_ID
    ).format(SUBMISSION_DATE_FORMATTER)

    val periodMonthYearFormatted = obligation.iCFromDate.format(MONTH_YEAR_FORMATTER)
    val paymentDueDateFormatted = obligation.iCDueDate.format(PAYMENT_DUE_FORMATTER)
    val isNilReturn = totalDutyDue == 0

    new ConfirmationViewModel(
      submissionDate = submissionDateFormatted,
      periodMonthYear = periodMonthYearFormatted,
      totalDutyAmount = totalDutyDue,
      totalDutyFormatted = currencyFormat(totalDutyDue.abs),
      paymentDueDate = paymentDueDateFormatted,
      chargeReference = chargeReference,
      content = getContent(totalDutyDue, paymentDueDateFormatted, btaLink),
      btaLink = btaLink,
      periodKey = PeriodKey(obligation.periodKey),
      showWhatYouMustDoNext = !isNilReturn
    )
  }

  private def getContent(dutyDue: BigDecimal, paymentDueDate: String, btaLink: String)
                        (implicit messages: Messages): Html = {
    if (dutyDue > 0) {
      getPositiveContent(dutyDue, paymentDueDate, btaLink)
    } else {
      getZeroContent()
    }
  }

  private def getPositiveContent(dutyDue: BigDecimal, paymentDueDate: String, btaLink: String)
                                (implicit messages: Messages): Html = {
    val warning = GovukWarningText()
    val p = Paragraph()
    val h2 = Heading2()
    val list = ListWithLinks()
    val link = Link()

    val warningSection = warning(WarningText(
      iconFallbackText = Some(messages("site.warning")),
      content = Text(messages("returns.confirmation.warning.youMust", currencyFormat(dutyDue), paymentDueDate))
    ))

    val directDebitParagraph = p(Seq(Text(messages("returns.confirmation.p.directDebit", paymentDueDate))))

    val whatNextHeading = h2(Text(messages("returns.confirmation.h2.whatNext")))

    val payNowBulletWithLink = Html(
      messages("returns.confirmation.bullet.bta.prefix") + " " +
      link(id = "bta-link", href = btaLink, text = messages("returns.confirmation.bullet.bta.linkText"))
    )

    val bulletList = list(Seq(
      payNowBulletWithLink,
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
