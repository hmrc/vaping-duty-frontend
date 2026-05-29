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

import base.{SpecBase, UnitSpec}
import models.CheckMode
import models.returns.DutySuspenseVolumes
import pages.returns.{DeclareDutySuspensePage, EnterDutySuspensePage}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import utils.CssConstants

class DutySuspenseSummarySpec extends SpecBase with UnitSpec {

  val volumeReceived = 1000
  val volumeMoved = 300
  val dutySuspenseVolumes = DutySuspenseVolumes(volumeReceived, volumeMoved)

  "DutySuspenseSummary" - {

    "when duty suspense is not declared" - {

      val ua = returnsUserAnswers.set(DeclareDutySuspensePage, false).success.value

      "must render product received row with one action" in {
        val result = DutySuspenseSummary.summaryList(ua)
        
        result.rows.length mustBe 3
        
        val receivedRow = result.rows.head
        receivedRow.actions.value.items.length mustBe 1
        receivedRow.actions.value.items.head.href mustBe controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(CheckMode).url
      }

      "must render product moved row with one action" in {
        val result = DutySuspenseSummary.summaryList(ua)
        
        val movedRow = result.rows(1)
        movedRow.actions.value.items.length mustBe 1
        movedRow.actions.value.items.head.href mustBe controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(CheckMode).url
      }

      "must render total volume row with no actions and bold styling" in {
        val result = DutySuspenseSummary.summaryList(ua)
        
        val totalRow = result.rows(2)
        totalRow.value.classes must include(CssConstants.boldFontWeight)
        totalRow.actions.value.items mustBe Seq.empty
      }
    }

    "when duty suspense is declared with values" - {

      val ua = returnsUserAnswers
        .set(DeclareDutySuspensePage, true).success.value
        .set(EnterDutySuspensePage, dutySuspenseVolumes).success.value

      "must render product received row with volume in ml" in {
        val result = DutySuspenseSummary.summaryList(ua)
        
        val receivedRow = result.rows.head
        receivedRow.value.content mustBe Text(s"$volumeReceived ml")
        receivedRow.actions.value.items.length mustBe 1
        receivedRow.actions.value.items.head.href mustBe controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(CheckMode).url
      }

      "must render product moved row with volume in ml" in {
        val result = DutySuspenseSummary.summaryList(ua)
        
        val movedRow = result.rows(1)
        movedRow.value.content mustBe Text(s"$volumeMoved ml")
        movedRow.actions.value.items.length mustBe 1
        movedRow.actions.value.items.head.href mustBe controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(CheckMode).url
      }

      "must render total volume row with calculated total in ml and bold styling" in {
        val result = DutySuspenseSummary.summaryList(ua)
        
        val expectedTotal = volumeReceived - volumeMoved
        val totalRow = result.rows(2)
        totalRow.value.content mustBe Text(s"$expectedTotal ml")
        totalRow.value.classes must include(CssConstants.boldFontWeight)
        totalRow.actions.value.items mustBe Seq.empty
      }
    }
  }
}