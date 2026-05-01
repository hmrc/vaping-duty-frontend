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

import config.CurrencyFormatter
import models.CheckMode
import models.returns.ReturnsUserAnswers
import pages.returns.{DeclareDutyPage, EnterDutyAmountPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ReturnsSummary extends CurrencyFormatter {

  def summaryList(answers: ReturnsUserAnswers)(implicit messages: Messages): SummaryList = {
    val rows = Seq(
      buildDutyRow(answers),
      //      buildSpoiltRow(answers),
      //      buildOverRow(answers),
      //      buildUnderRow(answers),
      buildTotalDutyRow(answers)
    ).flatten

    SummaryList(rows = rows)
  }

  private def dutyRow(value: String)(implicit messages: Messages) = {
    Option(SummaryListRowViewModel(
      key = "returns.CheckYourAnswers.dutySummary.vaping",
      value = ValueViewModel(Text(value)),
      actions = Seq(
        ActionItemViewModel("site.change", controllers.returns.routes.EnterDutyAmountController.onPageLoad(CheckMode).url)
          .withVisuallyHiddenText(messages(""))
      )
    ))
  }

  private def buildDutyRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(EnterDutyAmountPage) match {
      case Some(value) if value < 10 => dutyRow(messages("returns.CheckYourAnswers.dutySummary.nothing"))
      case Some(value) => dutyRow(currencyFormat(calculateDuty(value)))
      case None => dutyRow(messages("returns.CheckYourAnswers.dutySummary.nothing"))
    }

  private def buildSpoiltRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage).map { answer =>
      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.dutySummary.spoilt",
        value = ValueViewModel(""),
        actions = Seq(
          ActionItemViewModel("site.change", controllers.returns.routes.BeforeYouStartController.onPageLoad().url)
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
          ActionItemViewModel("site.change", controllers.returns.routes.BeforeYouStartController.onPageLoad().url)
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
          ActionItemViewModel("site.change", controllers.returns.routes.BeforeYouStartController.onPageLoad().url)
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

  private def buildTotalDutyRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] = {
    answers.get(EnterDutyAmountPage) match {
      case Some(value) => totalDutyRow(currencyFormat(calculateDuty(value)))
      case None => totalDutyRow(messages("returns.CheckYourAnswers.dutySummary.total.nil"))
    }
  }
}
