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

package viewmodels.returns.view

import models.returns.ConvertToMl
import models.returns.view.OtherOptions
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.CurrencyFormatter

final case class DutySuspenseSectionBuilder(otherOptions: Option[OtherOptions]) extends CurrencyFormatter {

  def build()(implicit messages: Messages): Option[SummaryList] = {
    otherOptions.map { options =>
      val yesNoText = if (options.vapingProductUnderDutySuspense == "1") {
        messages("viewIndividualReturn.dutySuspense.yes")
      } else {
        messages("viewIndividualReturn.dutySuspense.no")
      }

      val questionRow = SummaryListRow(
        key = Key(content = Text(messages("viewIndividualReturn.dutySuspense.question"))),
        value = Value(content = Text(yesNoText))
      )

      val detailRows = if (options.vapingProductUnderDutySuspense == "1") {
        val receivedValue = options.volumeMovedFromDutySuspense match {
          case Some(volume) if volume > 0 => messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(volume).toMl))
          case _ => messages("viewIndividualReturn.dutySuspense.nothingToDeclare")
        }

        val movedValue = options.volumeMovedToDutySuspense match {
          case Some(volume) if volume > 0 => messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(volume).toMl))
          case _ => messages("viewIndividualReturn.dutySuspense.nothingToDeclare")
        }

        Seq(
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.dutySuspense.productReceived"))),
            value = Value(content = Text(receivedValue))
          ),
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.dutySuspense.productMoved"))),
            value = Value(content = Text(movedValue))
          )
        )
      } else Seq.empty

      SummaryList(rows = questionRow +: detailRows)
    }
  }
}