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
import pages.returns.EnterDutyAmountPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import viewmodels.checkAnswers.ReturnsSummary.{calculateDuty, currencyFormat}
import viewmodels.checkAnswers.{DutySuspenseSummary, ReturnsSummary}

case class CheckYourAnswersViewModel(
                                      finalDutySummaryList: SummaryList,
                                      dutySuspendedSummaryList: SummaryList,
                                      dutyDue: String
                                    )

object CheckYourAnswersViewModel {

  private val ZERO = "0"
  
  def apply(userAnswers: ReturnsUserAnswers)(implicit messages: Messages): CheckYourAnswersViewModel =
    CheckYourAnswersViewModel(
      finalDutySummaryList = ReturnsSummary.summaryList(userAnswers),
      dutySuspendedSummaryList = DutySuspenseSummary.summaryList(userAnswers),
      dutyDue = dutyDue(userAnswers)
    )

  private def dutyDue(userAnswers: ReturnsUserAnswers) = {
    userAnswers.get(EnterDutyAmountPage) match {
      case Some(value) => currencyFormat(calculateDuty(value))
      case None => currencyFormat(BigDecimal(ZERO))
    }
  }
}