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
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Key, SummaryList, SummaryListRow}
import utils.CssConstants
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object DutySuspenseSummary {

  private val VOLUME_UNIT = " ml"
  private val ZERO_VOLUME = 0

  def summaryList(answers: ReturnsUserAnswers, periodKey: PeriodKey)(implicit messages: Messages): SummaryList = {
    val rows = Seq(
      buildDeclareDutySuspenseRow(answers, periodKey),
      buildProductReceivedRow(answers, periodKey),
      buildProductMovedRow(answers, periodKey),
      buildTotalVolumeRow(answers)
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

  private def formatVolume(volume: Int)(implicit messages: Messages): String =
    if (volume == ZERO_VOLUME) messages("returns.CheckYourAnswers.dutySummary.nothing")
    else s"$volume$VOLUME_UNIT"

  private def createChangeAction(url: String, messageKey: String, periodKey: PeriodKey)(implicit messages: Messages) =
    Seq(
      ActionItemViewModel("site.change", s"$url?period=${periodKey.value}")
        .withVisuallyHiddenText(messages(messageKey))
    )

  private def buildRow(
                        messageKey: String,
                        value: String,
                        actions: Seq[ActionItem],
                        cssClass: Option[String] = None
                      ): SummaryListRow = {
    val valueViewModel = cssClass match {
      case Some(css) => ValueViewModel(Text(value)).withCssClass(css)
      case None => ValueViewModel(Text(value))
    }

    SummaryListRowViewModel(
      key = Key(Text(messageKey), ""),
      value = valueViewModel,
      actions = actions
    )
  }

  private def buildProductReceivedRow(answers: ReturnsUserAnswers, periodKey: PeriodKey)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutySuspensePage) match {
      case Some(true) =>
        answers.get(EnterDutySuspensePage).map { answer =>
          buildRow(
            messages("returns.CheckYourAnswers.dutySuspended.received"),
            formatVolume(answer.volumeReceived),
            createChangeAction(controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(CheckMode).url, "", periodKey)
          )
        }
      case _ => None
    }

  private def buildProductMovedRow(answers: ReturnsUserAnswers, periodKey: PeriodKey)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutySuspensePage) match {
      case Some(true) =>
        answers.get(EnterDutySuspensePage).map { answer =>
          buildRow(
            messages("returns.CheckYourAnswers.dutySuspended.moved"),
            formatVolume(answer.volumeMoved),
            createChangeAction(controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(CheckMode).url, "", periodKey)
          )
        }
      case _ => None
    }

  private def buildTotalVolumeRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutySuspensePage) match {
      case Some(true) =>
        answers.get(EnterDutySuspensePage).map { answer =>
          val totalVolume = answer.volumeReceived - answer.volumeMoved
          val text = if (totalVolume == ZERO_VOLUME) {
            messages("returns.CheckYourAnswers.dutySuspended.total.nil")
          } else {
            s"$totalVolume$VOLUME_UNIT"
          }

          buildRow(
            messages("returns.CheckYourAnswers.dutySuspended.total"),
            text,
            Seq.empty,
            Some(CssConstants.boldFontWeight)
          )
        }
      case _ => None
    }
}
