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
import models.returns.ReturnsUserAnswers
import pages.returns.{DeclareDutyPage, DeclareDutySuspensePage, DeclareSpoiltProductsPage, EnterDutyAmountPage}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.ReturnsDateUtils
import viewmodels.checkAnswers.ReturnsSummary.currencyFormat
import viewmodels.checkAnswers.{DutySuspenseSummary, ReturnsSummary}
import views.html.components.Paragraph

import java.time.Month

case class CheckYourAnswersViewModel(
                                      finalDutySummaryList: SummaryList,
                                      dutySuspendedSummaryList: SummaryList,
                                      dutyDue: String,
                                      dutyRate: String,
                                      dutyRateParagraph: HtmlFormat.Appendable,
                                      dutyCalculationParagraph: HtmlFormat.Appendable,
                                      nilReturn: Boolean,
                                      returnPeriod: String,
                                      year: String
                                    )

object CheckYourAnswersViewModel {

  private val ZERO = "0"

  def apply(userAnswers: ReturnsUserAnswers, dutyRate: BigDecimal, periodKey: PeriodKey)(implicit messages: Messages): CheckYourAnswersViewModel = {
    
    val returnPeriod = userAnswers.returnPeriod
      .map(monthStr => Month.valueOf(monthStr))
      .map(month => ReturnsDateUtils.getReturnMonth(month))
      // scalafix:off DisableSyntax.throw
      .getOrElse(throw new IllegalStateException("Return period not found in user answers"))
    
    val year = userAnswers.year
      // scalafix:off DisableSyntax.throw
      .getOrElse(throw new IllegalStateException("Return year not found in user answers"))
    
    val nilReturn = isNilReturn(userAnswers)
    
    CheckYourAnswersViewModel(
      finalDutySummaryList = ReturnsSummary.summaryList(userAnswers, dutyRate, periodKey),
      dutySuspendedSummaryList = DutySuspenseSummary.summaryList(userAnswers, periodKey),
      dutyDue = dutyDue(userAnswers, dutyRate),
      dutyRate = currencyFormat(dutyRate),
      dutyRateParagraph = dutyRateParagraph(nilReturn, dutyRate),
      dutyCalculationParagraph = dutyCalculationParagraph(dutyRate),
      nilReturn = nilReturn,
      returnPeriod = returnPeriod,
      year = year
    )
  }

  private def isNilReturn(userAnswers: ReturnsUserAnswers): Boolean = {
    val declareDuty = userAnswers.get(DeclareDutyPage).getOrElse(false)
    val declareSpoilt = userAnswers.get(DeclareSpoiltProductsPage).getOrElse(false)
    val declareSuspense = userAnswers.get(DeclareDutySuspensePage).getOrElse(false)
    
    !declareDuty && !declareSpoilt && !declareSuspense
  }

  private def dutyDue(userAnswers: ReturnsUserAnswers, dutyRate: BigDecimal): String = {
    userAnswers.get(EnterDutyAmountPage) match {
      case Some(volumeInMl)  => currencyFormat(ReturnsSummary.calculateDuty(volumeInMl, dutyRate))
      case None              => currencyFormat(BigDecimal(ZERO))
    }
  }

  private def dutyRateParagraph(nilReturn: Boolean, dutyRate: BigDecimal)(implicit messages: Messages): HtmlFormat.Appendable = {
    val p = new Paragraph()

    if (nilReturn) {
      p(Seq(Text(messages("returns.CheckYourAnswers.nilReturn.paragraph"))))
    } else {
      p(Seq(Text(messages("returns.CheckYourAnswers.p.duty", currencyFormat(dutyRate * 10)))))
    }
  }

  private def dutyCalculationParagraph(dutyRate: BigDecimal)(implicit messages: Messages): HtmlFormat.Appendable = {
    val p = new Paragraph()
    p(Seq(Text(messages("returns.CheckYourAnswers.dutyCalculation", currencyFormat(dutyRate * 10)))))
  }
}