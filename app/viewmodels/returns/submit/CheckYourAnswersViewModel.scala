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
import models.returns.{DutyRate, ReturnsUserAnswers}
import models.returns.adjustments.AdjustmentType
import pages.returns.{DeclareDutyPage, DeclareSpoiltProductsPage, EnterDutyAmountPage}
import pages.returns.adjustments.AdjustmentListPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import utils.ReturnsDateUtils
import viewmodels.checkAnswers.ReturnsSummary.currencyFormat
import viewmodels.checkAnswers.{DutySuspenseSummary, ReturnsSummary}
import views.html.components.Paragraph

case class CheckYourAnswersViewModel(
                                      finalDutySummaryList: SummaryList,
                                      dutySuspendedSummaryList: SummaryList,
                                      dutyDue: String,
                                      dutyRateParagraph: HtmlFormat.Appendable,
                                      dutyCalculationParagraph: HtmlFormat.Appendable,
                                      nilReturn: Boolean,
                                      returnPeriod: String,
                                      year: String
                                    )

object CheckYourAnswersViewModel {

  private val ZERO = "0"

  def apply(userAnswers: ReturnsUserAnswers, dutyRate: DutyRate, periodKey: PeriodKey, returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): CheckYourAnswersViewModel = {
    // scalafix:off DisableSyntax.throw
    val returnPeriod = userAnswers.returnPeriod
      .map(month => returnsDateUtils.getReturnMonth(month))
      .getOrElse(throw new IllegalStateException("Return period not found in user answers"))
    
    val year = userAnswers.year
      .getOrElse(throw new IllegalStateException("Return year not found in user answers"))
    
    val nilReturn = isNilReturn(userAnswers)
    
    CheckYourAnswersViewModel(
      finalDutySummaryList = ReturnsSummary.summaryList(userAnswers, dutyRate, periodKey),
      dutySuspendedSummaryList = DutySuspenseSummary.summaryList(userAnswers, periodKey),
      dutyDue = dutyDue(userAnswers, dutyRate),
      dutyRateParagraph = dutyRateParagraph(nilReturn),
      dutyCalculationParagraph = dutyCalculationParagraph(dutyRate),
      nilReturn = nilReturn,
      returnPeriod = returnPeriod,
      year = year
    )
  }

  private def isNilReturn(userAnswers: ReturnsUserAnswers): Boolean = {
    val declareDuty = userAnswers.get(DeclareDutyPage).getOrElse(false)
    val declareSpoilt = userAnswers.get(DeclareSpoiltProductsPage).getOrElse(false)

    !declareDuty && !declareSpoilt
  }

  private def dutyDue(userAnswers: ReturnsUserAnswers, dutyRate: DutyRate): String = {
    userAnswers.get(EnterDutyAmountPage) match {
      case Some(volumeInMl)  => 
        val vapingProductsDuty = dutyRate.calculateDuty(volumeInMl)
        
        // Calculate adjustment totals to match the total duty row calculation
        val adjustmentTotal = userAnswers.get(AdjustmentListPage).map { list =>
          val underDuty = list.adjustments
            .filter(_.adjustmentType == AdjustmentType.UnderDeclared)
            .map(adj => dutyRate.calculateDuty(adj.volumeInMl))
            .sum
          
          val overDuty = list.adjustments
            .filter(_.adjustmentType == AdjustmentType.OverDeclared)
            .map(adj => dutyRate.calculateDuty(adj.volumeInMl))
            .sum
          
          underDuty - overDuty
        }.getOrElse(BigDecimal(ZERO))
        
        val totalDuty = vapingProductsDuty + adjustmentTotal
        currencyFormat(totalDuty)
      case None              => currencyFormat(BigDecimal(ZERO))
    }
  }

  private def dutyRateParagraph(nilReturn: Boolean)(implicit messages: Messages): HtmlFormat.Appendable = {
    val p = new Paragraph()

    if (nilReturn) {
      p(Seq(Text(messages("returns.CheckYourAnswers.nilReturn.paragraph"))))
    } else {
      HtmlFormat.empty
    }
  }

  private def dutyCalculationParagraph(dutyRate: DutyRate)(implicit messages: Messages): HtmlFormat.Appendable = {
    val p = new Paragraph()
    p(Seq(Text(messages("returns.CheckYourAnswers.dutyCalculation", currencyFormat(dutyRate.dutyRateInPoundsPer10Ml)))))
  }
}