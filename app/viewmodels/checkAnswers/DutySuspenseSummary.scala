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
import pages.returns.{DeclareDutySuspensePage, EnterDutySuspensePage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object DutySuspenseSummary {

  def summaryList(answers: ReturnsUserAnswers, periodKey: PeriodKey)(implicit messages: Messages): SummaryList = {
    val rows = Seq(
      buildDeclareDutySuspenseRow(answers, periodKey),
      buildDutySuspenseDeclaredRow(answers, periodKey)
    ).flatten

    SummaryList(rows = rows)
  }

  private def buildDeclareDutySuspenseRow(
    answers: ReturnsUserAnswers,
    periodKey: PeriodKey
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutySuspensePage).map { answer =>
      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.declareDutySuspense.question",
        value = ValueViewModel(Text(messages(if (answer) "site.yes" else "site.no"))),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            s"${controllers.returns.submit.routes.DeclareDutySuspenseController.onPageLoad(CheckMode).url}?period=${periodKey.value}"
          ).withVisuallyHiddenText(messages("returns.CheckYourAnswers.declareDutySuspense.question"))
        )
      )
    }

  private def buildDutySuspenseDeclaredRow(
    answers: ReturnsUserAnswers,
    periodKey: PeriodKey
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutySuspensePage) match {
      case Some(true) =>
        Some(SummaryListRowViewModel(
          key = "returns.CheckYourAnswers.dutySuspended.declared",
          value = ValueViewModel(Text(messages("returns.CheckYourAnswers.dutySuspended.declared.value"))),
          actions = Seq(
            ActionItemViewModel(
              "site.change",
              s"${controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(CheckMode).url}?period=${periodKey.value}"
            ).withVisuallyHiddenText(messages("returns.CheckYourAnswers.dutySuspended.declared"))
          )
        ))
      case _ => None
    }
}
