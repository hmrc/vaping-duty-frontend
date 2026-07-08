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

package viewmodels.checkAnswers

import models.NormalMode
import models.identifiers.PeriodKey
import models.returns.{DutySuspenseVolumes, ReturnsUserAnswers}
import pages.returns.EnterDutySuspensePage
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*
import viewmodels.returns.VolumeFormatter

object DutySuspenseCheckAnswersSummary {

  def summaryList(answers: ReturnsUserAnswers, periodKey: PeriodKey)
                 (implicit messages: Messages): Option[SummaryList] = {
    
    answers.get(EnterDutySuspensePage).map { volumes =>
      SummaryList(rows = Seq(buildCombinedRow(volumes, periodKey)))
    }
  }

  private def buildCombinedRow(volumes: DutySuspenseVolumes, periodKey: PeriodKey)
                              (implicit messages: Messages): SummaryListRow = {
    
    val keyHtml = Html(
      s"""${messages("returns.dutySuspenseCheckAnswers.productReceived")}<br>
         |${messages("returns.dutySuspenseCheckAnswers.productMoved")}""".stripMargin)
    
    val valueHtml = Html(
      s"""${VolumeFormatter.formatVolume(volumes.volumeReceived)}<br>
         |${VolumeFormatter.formatVolume(volumes.volumeMoved)}""".stripMargin
    )

    SummaryListRowViewModel(
      key = KeyViewModel(HtmlContent(keyHtml)),
      value = ValueViewModel(HtmlContent(valueHtml)),
      actions = Seq(
        ActionItemViewModel(
          "site.change",
          s"${controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(NormalMode).url}?period=${periodKey.value}"
        ).withVisuallyHiddenText(messages("returns.dutySuspenseCheckAnswers.change.hidden"))
      )
    )
  }
    
}
