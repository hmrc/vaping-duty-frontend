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

import models.identifiers.VpdId
import models.obligations.ObligationDetails
import models.returns.ReturnsUserAnswers
import pages.returns.EnterDutyAmountPage
import play.api.i18n.Messages
import services.returns.{DutyRateService, ObligationService}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.http.HeaderCarrier
import utils.CurrencyFormatter
import viewmodels.checkAnswers.{DutySuspenseSummary, ReturnsSummary}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

final case class CheckYourAnswersViewModel(
                                            finalDutySummaryList: SummaryList,
                                            dutySuspendedSummaryList: SummaryList,
                                            dutyDue: String,
                                            dutyRate: String
                                          )

@Singleton
class CheckYourAnswersViewModelProvider @Inject()(
  dutyRateService: DutyRateService,
  obligationService: ObligationService
)(implicit ec: ExecutionContext) extends CurrencyFormatter {

  private val ZERO = "0"

  def apply(userAnswers: ReturnsUserAnswers)
           (implicit messages: Messages, hc: HeaderCarrier): Future[CheckYourAnswersViewModel] = {
    obligationService.getObligationByPeriodKey(VpdId(userAnswers.vpdId), userAnswers.periodKey).map {
      case Some(obligation) =>
        CheckYourAnswersViewModel(
          finalDutySummaryList = ReturnsSummary.summaryList(userAnswers, obligation, dutyRateService),
          dutySuspendedSummaryList = DutySuspenseSummary.summaryList(userAnswers),
          dutyDue = dutyDue(userAnswers, obligation),
          dutyRate = currencyFormat(dutyRate(obligation))
        )
      case None =>
        throw new RuntimeException(s"Obligation not found for vpdId: ${userAnswers.vpdId}, periodKey: ${userAnswers.periodKey}")
    }
  }

  def dutyRate(obligation: ObligationDetails): BigDecimal = {
    val currentPeriodRate = dutyRateService.getRateForDate(obligation.iCFromDate)
    val dutyRate = BigDecimal(currentPeriodRate) / 100
    dutyRate
  }

  private def dutyDue(
                       userAnswers: ReturnsUserAnswers,
                       obligation: ObligationDetails
                     ): String = {
    userAnswers.get(EnterDutyAmountPage) match {
      case Some(value) =>
        val dutyDue = (value * dutyRate(obligation)).setScale(2, BigDecimal.RoundingMode.DOWN)
        currencyFormat(dutyDue)
      case None =>
        currencyFormat(BigDecimal(ZERO))
    }
  }
}
