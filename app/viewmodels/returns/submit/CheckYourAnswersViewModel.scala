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

import models.returns.ReturnsUserAnswers
import pages.returns.{DeclareDutyPage, EnterDutyAmountPage}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.checkAnswers.ReturnsSummary.currencyFormat
import viewmodels.checkAnswers.{DutySuspenseSummary, ReturnsSummary}
import views.html.components.Paragraph

case class CheckYourAnswersViewModel(
                                      finalDutySummaryList: SummaryList,
                                      dutySuspendedSummaryList: SummaryList,
                                      dutyDue: String,
                                      dutyRate: String,
                                      dutyRateParagraph: HtmlFormat.Appendable
                                    )

object CheckYourAnswersViewModel {

  private val ZERO = "0"

  def apply(userAnswers: ReturnsUserAnswers, dutyRate: BigDecimal)(implicit messages: Messages): CheckYourAnswersViewModel =
    CheckYourAnswersViewModel(
      finalDutySummaryList = ReturnsSummary.summaryList(userAnswers, dutyRate),
      dutySuspendedSummaryList = DutySuspenseSummary.summaryList(userAnswers),
      dutyDue = dutyDue(userAnswers, dutyRate),
      dutyRate = currencyFormat(dutyRate),
      dutyRateParagraph = dutyRateParagraph(userAnswers, dutyRate)
    )

  private def dutyDue(userAnswers: ReturnsUserAnswers, dutyRate: BigDecimal): String = {
    userAnswers.get(EnterDutyAmountPage) match {
      case Some(volumeInMl)  => currencyFormat(ReturnsSummary.calculateDuty(volumeInMl, dutyRate))
      case None              => currencyFormat(BigDecimal(ZERO))
    }
  }

  private def dutyRateParagraph(userAnswers: ReturnsUserAnswers, dutyRate: BigDecimal)(implicit messages: Messages) = {
    val p = new Paragraph()

    userAnswers.get(DeclareDutyPage) match {
      case Some(true)   => p(Seq(Text(messages("returns.CheckYourAnswers.p.duty", currencyFormat(dutyRate)))))
      case Some(false)  => p(Seq(Text(messages("NIL RETURN, CONTENT TBC"))))
      case None         => p(Seq(Text("")))
    }
  }
}