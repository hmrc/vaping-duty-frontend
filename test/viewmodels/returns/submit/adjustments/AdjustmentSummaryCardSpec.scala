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

package viewmodels.returns.submit.adjustments

import base.SpecBase
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Card, CardTitle, SummaryListRow}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

class AdjustmentSummaryCardSpec extends SpecBase {

  "AdjustmentSummaryCard" - {

    "must create a summary card with rows and card details" in {
      val rows = Seq(
        SummaryListRow(
          key = uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key(content = Text("Type")),
          value = uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Value(content = Text("Under declared"))
        )
      )
      val card = Card(
        title = Some(CardTitle(content = Text("October 2027")))
      )

      val summaryCard = AdjustmentSummaryCard(rows, card)

      summaryCard.rows mustBe rows
      summaryCard.card mustBe card
      summaryCard.card.title.value.content mustBe Text("October 2027")
    }

    "must handle multiple rows" in {
      val rows = Seq(
        SummaryListRow(
          key = uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key(content = Text("Type")),
          value = uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Value(content = Text("Under declared"))
        ),
        SummaryListRow(
          key = uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key(content = Text("Volume")),
          value = uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Value(content = Text("100.5 ml"))
        )
      )
      val card = Card(
        title = Some(CardTitle(content = Text("October 2027")))
      )

      val summaryCard = AdjustmentSummaryCard(rows, card)

      summaryCard.rows.size mustBe 2
      summaryCard.rows mustBe rows
    }

    "must handle empty rows" in {
      val rows = Seq.empty[SummaryListRow]
      val card = Card(
        title = Some(CardTitle(content = Text("October 2027")))
      )

      val summaryCard = AdjustmentSummaryCard(rows, card)

      summaryCard.rows mustBe empty
      summaryCard.card.title.value.content mustBe Text("October 2027")
    }
  }
}
