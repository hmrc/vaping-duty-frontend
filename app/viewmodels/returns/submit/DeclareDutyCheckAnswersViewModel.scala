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

import models.NormalMode
import models.identifiers.PeriodKey
import models.returns.ReturnsUserAnswers
import pages.returns.EnterDutyAmountPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.checkAnswers.ReturnsSummary
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

case class DeclareDutyCheckAnswersViewModel(
  volumeFormatted: String,
  dutyDue: String,
  summaryList: SummaryList
)

object DeclareDutyCheckAnswersViewModel {

  private val ML_SUFFIX = " ml"

  def apply(userAnswers: ReturnsUserAnswers, dutyRate: BigDecimal, periodKey: PeriodKey)
           (implicit messages: Messages): Option[DeclareDutyCheckAnswersViewModel] = {
    
    userAnswers.get(EnterDutyAmountPage).map { volumeInMl =>
      val dutyAmount = ReturnsSummary.calculateDuty(volumeInMl, dutyRate)
      
      DeclareDutyCheckAnswersViewModel(
        volumeFormatted = formatVolume(volumeInMl),
        dutyDue = ReturnsSummary.currencyFormat(dutyAmount),
        summaryList = buildSummaryList(volumeInMl, dutyAmount, periodKey)
      )
    }
  }

  private def formatVolume(volumeInMl: BigDecimal): String =
    s"${volumeInMl.toString}$ML_SUFFIX"

  private def buildSummaryList(volumeInMl: BigDecimal, dutyAmount: BigDecimal, periodKey: PeriodKey)(implicit messages: Messages): SummaryList = {
    val rows = Seq(
      buildVolumeRow(volumeInMl, periodKey),
      buildDutyDueRow(dutyAmount)
    )

    SummaryList(rows = rows)
  }

  private def buildVolumeRow(volumeInMl: BigDecimal, periodKey: PeriodKey)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key = "returns.declareDutyCheckAnswers.volume",
      value = ValueViewModel(Text(formatVolume(volumeInMl))),
      actions = Seq(
        ActionItemViewModel(
          "site.change",
          s"${controllers.returns.submit.routes.EnterDutyAmountController.onPageLoad(NormalMode).url}?period=${periodKey.value}"
        ).withVisuallyHiddenText(messages("returns.declareDutyCheckAnswers.volume.change.hidden"))
      )
    )
  }

  private def buildDutyDueRow(dutyAmount: BigDecimal)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key = "returns.declareDutyCheckAnswers.dutyDue",
      value = ValueViewModel(Text(ReturnsSummary.currencyFormat(dutyAmount))),
      actions = Seq.empty
    )
  }
}