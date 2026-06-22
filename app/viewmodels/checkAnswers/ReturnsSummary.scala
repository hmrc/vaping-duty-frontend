/*
 * Copyright 2024 HM Revenue & Customs
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

package viewmodels.checkAnswers

import models.CheckMode
import models.identifiers.PeriodKey
import models.returns.ReturnsUserAnswers
import pages.returns.{DeclareDutyPage, EnterDutyAmountPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import utils.{CssConstants, CurrencyFormatter}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ReturnsSummary extends CurrencyFormatter {

  def summaryList(
    answers: ReturnsUserAnswers,
    dutyRate: BigDecimal,
    periodKey: PeriodKey
  )(implicit messages: Messages): SummaryList = {
    val rows = Seq(
      buildDeclareDutyRow(answers, periodKey),
      buildDutyRow(answers, dutyRate, periodKey),
      //      buildSpoiltRow(answers, periodKey),
      //      buildOverRow(answers, periodKey),
      //      buildUnderRow(answers, periodKey),
      buildTotalDutyRow(answers, dutyRate)
    ).flatten

    SummaryList(rows = rows, classes = CssConstants.marginBottom9)
  }

  private def buildDeclareDutyRow(
    answers: ReturnsUserAnswers,
    periodKey: PeriodKey
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage).map { answer =>
      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.declareDuty.vaping",
        value = ValueViewModel(Text(messages(if (answer) "site.yes" else "site.no"))),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            s"${controllers.returns.submit.routes.DeclareDutyController.onPageLoad(CheckMode).url}?period=${periodKey.value}"
          ).withVisuallyHiddenText(messages("returns.CheckYourAnswers.declareDuty.vaping"))
        )
      )
    }

  private def dutyRow(dutyDue: String, periodKey: PeriodKey)(implicit messages: Messages) = {
    Option(SummaryListRowViewModel(
      key = "returns.CheckYourAnswers.dutySummary.vaping",
      value = ValueViewModel(Text(dutyDue)),
      actions = Seq(
        ActionItemViewModel(
          "site.change", 
          s"${controllers.returns.submit.routes.EnterDutyAmountController.onPageLoad(CheckMode).url}?period=${periodKey.value}"
        ).withVisuallyHiddenText(messages(""))
      )
    ))
  }

  def calculateDuty(volumeInMl: BigDecimal, dutyRate: BigDecimal): BigDecimal =
    (volumeInMl * dutyRate).setScale(2, BigDecimal.RoundingMode.DOWN)

  private def buildDutyRow(
    answers: ReturnsUserAnswers,
    dutyRate: BigDecimal,
    periodKey: PeriodKey
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage) match {
      case Some(true) =>
        answers.get(EnterDutyAmountPage) match {
          case Some(volumeInMl) if volumeInMl == 0 => dutyRow(messages("returns.CheckYourAnswers.dutySummary.nothing"), periodKey)
          case Some(volumeInMl) => 
            val dutyDue = calculateDuty(volumeInMl, dutyRate)
            dutyRow(currencyFormat(dutyDue), periodKey)
          case None => dutyRow(messages("returns.CheckYourAnswers.dutySummary.nothing"), periodKey)
        }
      case _ => None
    }

  private def buildSpoiltRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage).map { answer =>
      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.dutySummary.spoilt",
        value = ValueViewModel(""),
        actions = Seq(
          ActionItemViewModel("site.change", controllers.returns.submit.routes.BeforeYouStartController.onPageLoad().url)
            .withVisuallyHiddenText(messages(""))
        )
      )
    }

  private def buildOverRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage).map { answer =>
      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.dutySummary.over",
        value = ValueViewModel(""),
        actions = Seq(
          ActionItemViewModel("site.change", controllers.returns.submit.routes.BeforeYouStartController.onPageLoad().url)
            .withVisuallyHiddenText(messages(""))
        )
      )
    }

  private def buildUnderRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage).map { answer =>
      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.dutySummary.under",
        value = ValueViewModel(""),
        actions = Seq(
          ActionItemViewModel("site.change", controllers.returns.submit.routes.BeforeYouStartController.onPageLoad().url)
            .withVisuallyHiddenText(messages(""))
        )
      )
    }

  private def totalDutyRow(value: String)(implicit messages: Messages) = {
    Option(SummaryListRowViewModel(
      key = "returns.CheckYourAnswers.dutySummary.total",
      value = ValueViewModel(Text(value)).withCssClass("govuk-!-font-weight-bold"),
      actions = Seq.empty
    ))
  }

  private def buildTotalDutyRow(
    answers: ReturnsUserAnswers,
    dutyRate: BigDecimal
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage) match {
      case Some(true) =>
        answers.get(EnterDutyAmountPage) match {
          case Some(volumeInMl) => 
            val dutyDue = calculateDuty(volumeInMl, dutyRate)
            totalDutyRow(currencyFormat(dutyDue))
          case None => totalDutyRow(messages("returns.CheckYourAnswers.dutySummary.total.nil"))
        }
      case _ => None
    }
}
