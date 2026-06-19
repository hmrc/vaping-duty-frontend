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
import models.returns.{DutySuspenseVolumes, ReturnsUserAnswers}
import pages.returns.{DeclareDutySuspensePage, EnterDutySuspensePage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

case class DutySuspenseCheckAnswersViewModel(
  heading: String,
  summaryList: SummaryList,
  cardActions: Option[Seq[ActionItem]]
)

object DutySuspenseCheckAnswersViewModel {

  private val ML_SUFFIX = " ml"

  def apply(userAnswers: ReturnsUserAnswers, periodKey: PeriodKey)
           (implicit messages: Messages): Option[DutySuspenseCheckAnswersViewModel] = {
    
    userAnswers.get(DeclareDutySuspensePage).flatMap { declareDutySuspense =>
      if (declareDutySuspense) {
        userAnswers.get(EnterDutySuspensePage).map { volumes =>
          DutySuspenseCheckAnswersViewModel(
            heading = messages("returns.dutySuspenseCheckAnswers.heading"),
            summaryList = buildSummaryListWithVolumes(declareDutySuspense, volumes, periodKey),
            cardActions = Some(Seq(
              ActionItemViewModel(
                "site.change",
                s"${controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(NormalMode).url}?period=${periodKey.value}"
              ).withVisuallyHiddenText(messages("returns.dutySuspenseCheckAnswers.cardActions.change.hidden"))
            ))
          )
        }
      } else {
        Some(DutySuspenseCheckAnswersViewModel(
          heading = messages("returns.dutySuspenseCheckAnswers.noDutyHeading"),
          summaryList = buildSummaryListNilReturn(declareDutySuspense, periodKey),
          cardActions = None
        ))
      }
    }
  }

  private def formatVolume(volume: Int): String = 
    s"${volume.toString}$ML_SUFFIX"

  private def buildDeclareDutySuspenseRow(declareDutySuspense: Boolean, periodKey: PeriodKey)
                                         (implicit messages: Messages): SummaryListRow = {
    val value = if (declareDutySuspense) "site.yes" else "site.no"
    
    SummaryListRowViewModel(
      key = "returns.dutySuspenseCheckAnswers.declareDutySuspense",
      value = ValueViewModel(value),
      actions = Seq(
        ActionItemViewModel(
          "site.change",
          s"${controllers.returns.submit.routes.DeclareDutySuspenseController.onPageLoad(NormalMode).url}?period=${periodKey.value}"
        ).withVisuallyHiddenText(messages("returns.dutySuspenseCheckAnswers.declareDutySuspense.change.hidden"))
      )
    )
  }

  private def buildProductReceivedRow(volumes: DutySuspenseVolumes)
                                     (implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key = "returns.dutySuspenseCheckAnswers.productReceived",
      value = ValueViewModel(Text(formatVolume(volumes.volumeReceived))),
      actions = Seq.empty
    )
  }

  private def buildProductMovedRow(volumes: DutySuspenseVolumes)
                                  (implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key = "returns.dutySuspenseCheckAnswers.productMoved",
      value = ValueViewModel(Text(formatVolume(volumes.volumeMoved))),
      actions = Seq.empty
    )
  }

  private def buildSummaryListWithVolumes(declareDutySuspense: Boolean, volumes: DutySuspenseVolumes, 
                                          periodKey: PeriodKey)
                                         (implicit messages: Messages): SummaryList = {
    val rows = Seq(
      buildDeclareDutySuspenseRow(declareDutySuspense, periodKey),
      buildProductReceivedRow(volumes),
      buildProductMovedRow(volumes)
    )

    SummaryList(rows = rows)
  }

  private def buildSummaryListNilReturn(declareDutySuspense: Boolean, periodKey: PeriodKey)
                                       (implicit messages: Messages): SummaryList = {
    val rows = Seq(
      buildDeclareDutySuspenseRow(declareDutySuspense, periodKey)
    )

    SummaryList(rows = rows)
  }
}

