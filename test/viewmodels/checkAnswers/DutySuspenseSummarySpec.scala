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
import pages.returns.DeclareDutySuspensePage
import uk.gov.hmrc.govukfrontend.views.Aliases.Text

class DutySuspenseSummarySpec extends SpecBase with UnitSpec {

  "DutySuspenseSummary" - {

    "when duty suspense is not declared" - {

      val ua = returnsUserAnswers.set(DeclareDutySuspensePage, false).success.value

      "must render only the yes/no question row" in {
        val result = DutySuspenseSummary.summaryList(ua, periodKey)
        
        result.rows.length mustBe 1
      }

      "must render yes/no row with 'No' value" in {
        val result = DutySuspenseSummary.summaryList(ua, periodKey)
        
        val questionRow = result.rows.head
        questionRow.value.content mustBe Text("No")
      }

      "must render yes/no row with change link to DeclareDutySuspenseController" in {
        val result = DutySuspenseSummary.summaryList(ua, periodKey)
        
        val questionRow = result.rows.head
        questionRow.actions.value.items.length mustBe 1
        questionRow.actions.value.items.head.href mustBe s"${controllers.returns.submit.routes.DeclareDutySuspenseController.onPageLoad(CheckMode).url}?period=${periodKey.value}"
      }
    }

    "when duty suspense is declared" - {

      val ua = returnsUserAnswers.set(DeclareDutySuspensePage, true).success.value

      "must render two rows" in {
        val result = DutySuspenseSummary.summaryList(ua, periodKey)
        
        result.rows.length mustBe 2
      }

      "must render yes/no row with 'Yes' value" in {
        val result = DutySuspenseSummary.summaryList(ua, periodKey)
        
        val questionRow = result.rows.head
        questionRow.value.content mustBe Text("Yes")
      }

      "must render yes/no row with change link to DeclareDutySuspenseController" in {
        val result = DutySuspenseSummary.summaryList(ua, periodKey)
        
        val questionRow = result.rows.head
        questionRow.actions.value.items.head.href mustBe s"${controllers.returns.submit.routes.DeclareDutySuspenseController.onPageLoad(CheckMode).url}?period=${periodKey.value}"
      }

      "must render declared row with 'Declared' value" in {
        val result = DutySuspenseSummary.summaryList(ua, periodKey)
        
        val declaredRow = result.rows(1)
        declaredRow.value.content mustBe Text("Declared")
      }

      "must render declared row with change link to EnterDutySuspenseController" in {
        val result = DutySuspenseSummary.summaryList(ua, periodKey)
        
        val declaredRow = result.rows(1)
        declaredRow.actions.value.items.length mustBe 1
        declaredRow.actions.value.items.head.href mustBe s"${controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(CheckMode).url}?period=${periodKey.value}"
      }
    }
  }
}
