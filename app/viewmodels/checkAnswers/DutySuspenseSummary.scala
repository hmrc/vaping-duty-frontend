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
import models.returns.ReturnsUserAnswers
import pages.returns.EnterDutySuspensePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import utils.CssConstants
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object DutySuspenseSummary {

  def summaryList(answers: ReturnsUserAnswers)(implicit messages: Messages): SummaryList = {
    val rows = Seq(
      buildProductReceivedRow(answers),
      buildProductMovedRow(answers),
      buildTotalVolumeRow(answers)
    ).flatten
    
    SummaryList(rows = rows)
  }

  private def buildProductReceivedRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(EnterDutySuspensePage).map { answer =>

      val text = if (answer.volumeReceived == 0) {
        "returns.CheckYourAnswers.dutySummary.nothing"
      } else {
        answer.volumeReceived.toString + " ml"
      }

      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.dutySuspended.received",
        value = ValueViewModel(text),
        actions = Seq(
          ActionItemViewModel("site.change", controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages(""))
        )
      )
    }

  private def buildProductMovedRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(EnterDutySuspensePage).map { answer =>

      val text = if (answer.volumeMoved == 0) {
        "returns.CheckYourAnswers.dutySummary.nothing"
      } else {
        answer.volumeMoved.toString + " ml"
      }

      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.dutySuspended.moved",
        value = ValueViewModel(text),
        actions = Seq(
          ActionItemViewModel("site.change", controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages(""))
        )
      )
    }

  private def buildTotalVolumeRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(EnterDutySuspensePage).map { answer =>

      val text = if (answer.volumeMoved == 0) {
        "returns.CheckYourAnswers.dutySuspended.total.nil"
      } else {
        s"${answer.volumeReceived - answer.volumeMoved} ml"
      }

      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.dutySuspended.total",
        value = ValueViewModel(text).withCssClass(CssConstants.boldFontWeight),
        actions = Seq.empty
        )
    }
}
