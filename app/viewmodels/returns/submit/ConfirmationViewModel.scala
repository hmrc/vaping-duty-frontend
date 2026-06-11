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
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import uk.gov.hmrc.govukfrontend.views.viewmodels.warningtext.WarningText
import utils.{CurrencyFormatter, ReturnsDateUtils}
import views.html.components.{Heading2, Link, ListWithLinks, Paragraph}

case class ConfirmationViewModel(email: String,
                                 date: String,
                                 currentMonth: String,
                                 content: Html,
                                 vpdRef: String,
                                 btaLink: String,
                                 periodKey: PeriodKey,
                                 viewReturnUrl: String)

object ConfirmationViewModel extends CurrencyFormatter {

  def apply(dutyDue: BigDecimal, email: String, vpdRef: String, btaLink: String, periodKey: PeriodKey, viewReturnUrl: String)
           (implicit messages: Messages): ConfirmationViewModel =

    confirmationViewModel(email, dutyDue, vpdRef, btaLink, periodKey, viewReturnUrl)


  private def confirmationViewModel(email: String, dutyDue: BigDecimal, vpdRef: String, btaLink: String, periodKey: PeriodKey, viewReturnUrl: String)(implicit messages: Messages) =

    val monthMessage = ReturnsDateUtils.getMonthMessage(ReturnsDateUtils.month)

    new ConfirmationViewModel(
      email,
      makeDateString(monthMessage),
      monthMessage,
      getContent(dutyDue),
      vpdRef,
      btaLink,
      periodKey,
      viewReturnUrl
    )

  private def totalDue(dutyDue: BigDecimal) = {
    currencyFormat(dutyDue)
  }


  private def makeDateString(monthMessage: String)(implicit messages: Messages) = {
    s"${ReturnsDateUtils.getCurrentDay} $monthMessage ${ReturnsDateUtils.getYear}"
  }

  private def elems(html: HtmlFormat.Appendable)(implicit messages: Messages) =

    val p = Paragraph()
    val h2 = Heading2()
    val list = ListWithLinks()
    val link = Link()

    Seq(
      html,
      p(Seq(Text(messages("returns.confirmation.p.youWill")))),
      p(Seq(Text(messages("returns.confirmation.p.yourReturn")))),
      h2(Text(messages("returns.confirmation.h2.howTo"))),
      p(Seq(Text(messages("returns.confirmation.selectOne")))),
      list(Seq(
        link(id = "ddLink", href = "#", text = messages("returns.confirmation.link.directDebit")),
        link(id = "payNowLink", href = "#", text = messages("returns.confirmation.link.payNow"))
      ), classes = "govuk-list govuk-list--bullet")
    )

  private def getContent(dutyDue: BigDecimal)(implicit messages: Messages) = {

    val monthMessage = ReturnsDateUtils.getMonthMessage(ReturnsDateUtils.month)

    if (dutyDue != 0) {
      val warning = GovukWarningText()

      val warningSection = warning(WarningText(
        iconFallbackText = Some(messages("site.warning")),
        content = Text(messages("returns.confirmation.warning.youMust", totalDue(dutyDue), monthMessage, ReturnsDateUtils.getYear.toString))
      ))

      HtmlFormat.fill(elems(warningSection))
    } else {
      val govukInsetText = GovukInsetText()

      val insetSection = govukInsetText(InsetText(content = Text(value = messages("returns.confirmation.inset.youHave"))))

      HtmlFormat.fill(elems(insetSection))
    }
  }
}
