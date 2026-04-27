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

package viewmodels.returns

import config.CurrencyFormatter
import models.returns.ReturnsUserAnswers
import pages.returns.EnterDutyAmountPage
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.html.components.{GovukInsetText, GovukWarningText}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.insettext.InsetText
import uk.gov.hmrc.govukfrontend.views.viewmodels.warningtext.WarningText
import utils.ReturnsDateUtils
import views.html.components.{Heading2, Link, Paragraph, ListWithLinks}

case class ConfirmationViewModel(email: String,
                                 date: String,
                                 currentMonth: String,
                                 content: Html,
                                 vpdRef: String,
                                 btaLink: String)

object ConfirmationViewModel extends CurrencyFormatter {

  def apply(ua: ReturnsUserAnswers, email: String, vpdRef: String, btaLink: String)(implicit messages: Messages): ConfirmationViewModel =
    confirmationViewModel(email, ua, vpdRef, btaLink)


  private def confirmationViewModel(email: String, ua: ReturnsUserAnswers, vpdRef: String, btaLink: String)(implicit messages: Messages) =

    val monthMessage = ReturnsDateUtils.getCurrentMonthMessage(ReturnsDateUtils.month)

    val amountInMl = ua.get(EnterDutyAmountPage) match {
      case Some(value) => value
      case None => 0
    }
    new ConfirmationViewModel(email, makeDateString(monthMessage), monthMessage, getContent(amountInMl), vpdRef, btaLink)

  private def totalDue(valueInMl: Int) =
    currencyFormat(calculateDuty(valueInMl))


  private def makeDateString(monthMessage: String)(implicit messages: Messages) = {
    s"${ReturnsDateUtils.getCurrentDay} $monthMessage ${ReturnsDateUtils.getYear}"
  }

  private def getContent(valueInMl: Int)(implicit messages: Messages) = {

    val monthMessage = ReturnsDateUtils.getCurrentMonthMessage(ReturnsDateUtils.month)

    if (valueInMl > 9) {
      val warning = GovukWarningText()
      val p = Paragraph()
      val h2 = Heading2()
      val list = ListWithLinks()
      val link = Link()

      val elems = Seq(
        warning(WarningText(
          iconFallbackText = Some(messages("site.warning")),
          content = Text(messages("returns.confirmation.warning.youMust", totalDue(valueInMl), monthMessage))
        )),
        p(Seq(Text(messages("returns.confirmation.p.youWill")))),
        p(Seq(Text(messages("returns.confirmation.p.yourReturn")))),
        h2(Text(messages("returns.confirmation.h2.howTo"))),
        p(Seq(Text(messages("returns.confirmation.selectOne")))),
        list(Seq(
          link(id = "ddLink", href = "#", text = messages("returns.confirmation.link.directDebit")),
          link(id = "payNowLink", href = "#", text = messages("returns.confirmation.link.payNow"))
        ), classes = "govuk-list govuk-list--bullet")
      )

      HtmlFormat.fill(elems)
    } else {
      val govukInsetText = GovukInsetText()
      govukInsetText(InsetText(content = Text(value = messages("returns.confirmation.inset.youHave"))))
    }
  }
}
