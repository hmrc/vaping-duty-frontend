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

import models.ChargeReference
import models.identifiers.PeriodKey
import models.obligations.ObligationDetails
import models.returns.ReturnsConstants
import models.returns.view.ReturnDisplayResponse
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.html.components.{GovukInsetText, GovukWarningText}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import uk.gov.hmrc.govukfrontend.views.viewmodels.warningtext.WarningText
import utils.{CssConstants, CurrencyFormatter, ReturnsDateUtils}
import views.html.components.{Heading2, Link, ListWithLinks, Paragraph}

import java.time.{LocalDate, ZoneId}

case class ConfirmationViewModel(
                                  submissionDate: String,
                                  periodMonthYear: String,
                                  totalDutyAmount: BigDecimal,
                                  paymentDueDate: Option[String],
                                  chargeReference: Option[ChargeReference],
                                  content: Html,
                                  btaLink: String,
                                  paymentLink: String,
                                  periodKey: PeriodKey
                                )

object ConfirmationViewModel extends CurrencyFormatter {

  private val ZONE_ID = ZoneId.of("Europe/London")

  def apply(
    returnsResponse: ReturnDisplayResponse,
    obligation: ObligationDetails,
    btaLink: String,
    paymentLink: String,
    paymentDueDate: Option[LocalDate],
    returnsDateUtils: ReturnsDateUtils
  )(implicit messages: Messages): ConfirmationViewModel = {

    val totalDutyDue = returnsResponse.success.totalDutyDue.fold(BigDecimal("0"))(_.totalDue)

    val chargeReference = returnsResponse.success.chargeDetails
      .flatMap(_.chargeReference)

    val submissionDate = LocalDate.ofInstant(
      returnsResponse.success.processingDate,
      ZONE_ID
    )

    val dueDate = paymentDueDate.map(date => returnsDateUtils.formatDayMonthYear(date))

    val periodMonthYearFormatted = s"${returnsDateUtils.getMonthMessage(obligation.iCFromDate.getMonth)} ${obligation.iCFromDate.getYear}"

    new ConfirmationViewModel(
      submissionDate = returnsDateUtils.formatDayMonthYear(submissionDate),
      periodMonthYear = periodMonthYearFormatted,
      totalDutyAmount = totalDutyDue,
      paymentDueDate = dueDate,
      chargeReference = chargeReference,
      content = getContent(totalDutyDue, dueDate, btaLink, paymentLink),
      btaLink = btaLink,
      paymentLink = paymentLink,
      periodKey = PeriodKey(obligation.periodKey)
    )
  }

  private def getContent(dutyDue: BigDecimal, paymentDueDate: Option[String], btaLink: String, paymentLink: String)
                        (implicit messages: Messages): Html = {
    if (dutyDue > 0) {
      getPositiveContent(dutyDue, paymentDueDate, btaLink, paymentLink)
    } else if (dutyDue < 0) {
      getNegativeContent(dutyDue)
    } else {
      getZeroContent()
    }
  }

  private val warning = GovukWarningText()
  private val p = Paragraph()
  private val h2 = Heading2()
  private val list = ListWithLinks()
  private val link = Link()
  private val govukInsetText = GovukInsetText()

  private def getPositiveContent(dutyDue: BigDecimal, paymentDueDate: Option[String], btaLink: String, paymentLink: String)
                                (implicit messages: Messages): Html = {
    val isChapsRequired = dutyDue >= ReturnsConstants.MAX_DIRECT_DEBIT_THRESHOLD

    HtmlFormat.fill(
      topSection(dutyDue, paymentDueDate, isChapsRequired) ++
      whatsNext(btaLink, paymentLink, isChapsRequired))
  }

  private def topSection(dutyDue: BigDecimal, paymentDueDate: Option[String], isChapsRequired: Boolean)
                        (implicit messages: Messages): Seq[HtmlFormat.Appendable] = {
    if (isChapsRequired) {
      Seq(
        warningSection(dutyDue, paymentDueDate),
        h2(Text(messages("returns.confirmation.chaps.h2.title"))),
        p(Seq(Text(messages("returns.confirmation.chaps.p.cannotCollect")))),
        p(Seq(Text(messages("returns.confirmation.chaps.p.mustPayByChaps", paymentDueDate.getOrElse(""))))),
        chargeReferenceParagraph()
      )
    } else {
      val interestMessage = paymentDueDate match {
        case Some(date) => messages("returns.confirmation.p.interest", date)
        case _          => messages("returns.confirmation.p.interestNoDate")
      }

      Seq(
        warningSection(dutyDue, paymentDueDate),
        chargeReferenceParagraph(),
        p(Seq(Text(interestMessage)))
      )
    }
  }

  private def warningSection(dutyDue: BigDecimal, paymentDueDate: Option[String])(implicit messages: Messages) = {
    val warningMessage = paymentDueDate match {
      case Some(date) => messages("returns.confirmation.warning.youMust", currencyFormat(dutyDue), date)
      case None       => messages("returns.confirmation.warning.youMustNoDueDate", currencyFormat(dutyDue))
    }

    warning(WarningText(
      iconFallbackText = Some(messages("site.warning")),
      content = Text(warningMessage)
    ))
  }

  private def chargeReferenceParagraph()(implicit messages: Messages) =
    p(Seq(Text(messages("returns.confirmation.p.chargeReferenceReminder"))))

  private def whatsNext(btaLink: String, paymentLink: String, isChapsRequired: Boolean)(implicit messages: Messages) =
    Seq(
      whatsNextHeading(),
      paymentLinkList(btaLink, paymentLink, isChapsRequired)
    )

  private def whatsNextHeading()(implicit messages: Messages) =
    h2(Text(messages("returns.confirmation.h2.whatNext")))

  private def paymentLinkList(btaLink: String, paymentLink: String, isChapsRequired: Boolean)(implicit messages: Messages) =
    list(Seq(
      paymentLinks(isChapsRequired, paymentLink),
      businessTaxAccountLink(btaLink, isChapsRequired))
    )

  private def paymentLinks(isChapsRequired: Boolean, paymentLink: String)(implicit messages: Messages) = {
    if (isChapsRequired) {
      p(Seq(HtmlContent(
        link(
          id = "chaps-payment-link",
          href = paymentLink,
          text = messages("returns.confirmation.chaps.bullet.linkText"),
          newTabText = Some(messages("site.newTab"))
        )
      )))
    } else {
      p(Seq(HtmlContent(
        link(
          id = "direct-debit-link",
          href = controllers.payments.routes.StartDirectDebitController.startDirectDebit().url,
          text = messages("returns.confirmation.bullet.directDebit.linkText")
        )
      )))
    }
  }

  private def businessTaxAccountLink(btaLink: String, isChapsRequired: Boolean)(implicit messages: Messages) = {
    val businessTaxAccountLinkPrefixKey =
      if (isChapsRequired) "returns.confirmation.chaps.bullet.bta.prefix"
      else "returns.confirmation.bullet.bta.prefix"

    val businessTaxAccountLink = Paragraph()(
      Seq(HtmlContent(
        link(id = "bta-link", href = btaLink, text = s"${messages(businessTaxAccountLinkPrefixKey)} ${messages("returns.confirmation.bullet.bta.linkText")}")
      )),
      classes = s"govuk-body ${CssConstants.paddingBottom2}"
    )
    businessTaxAccountLink
  }

  private def getNegativeContent(dutyDue: BigDecimal)(implicit messages: Messages): Html = {
    val prefix = messages("returns.confirmation.inset.negative.prefix", currencyFormat(dutyDue.abs))
    val repaymentLink = link(
      id = "repayment-link",
      href = "#",
      text = messages("returns.confirmation.inset.negative.linkText")
    )
    val suffix = messages("returns.confirmation.inset.negative.suffix")

    val content = HtmlFormat.fill(Seq(Html(prefix), repaymentLink, Html(suffix)))

    val insetSection = govukInsetText(InsetText(
      content = HtmlContent(content)
    ))

    insetSection
  }

  private def getZeroContent()(implicit messages: Messages): Html = {
    val insetSection = govukInsetText(InsetText(
      content = Text(messages("returns.confirmation.inset.youHave"))
    ))

    insetSection
  }
}
