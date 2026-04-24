/*
 * Copyright 2025 HM Revenue & Customs
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
import pages.returns.{DeclareDutyPage, EnterDutyAmountPage}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.all.{ActionItemViewModel, SummaryListRowViewModel, ValueViewModel, stringToKey}
import viewmodels.govuk.summarylist.FluentActionItem


class EnterDutyAmountSummarySpec extends SpecBase with UnitSpec {

  val ml = 1000
  val ua = returnsUserAnswers.set(EnterDutyAmountPage, ml).success.value

  def summaryList(answer: String): SummaryListRow = SummaryListRowViewModel(
    key = "enterDutyAmount.checkYourAnswersLabel",
    value = ValueViewModel(Text(answer)),
    actions = Seq(
      ActionItemViewModel(Text("Change"), controllers.returns.routes.EnterDutyAmountController.onPageLoad(CheckMode).url)
        .withVisuallyHiddenText("enterDutyAmount.change.hidden")
    )
  )

  "EnterDutyAmountSummary" - {
    
    "must render correctly when value is present" in {

      val expectedResult = summaryList(ml.toString)

      EnterDutyAmountSummary.row(ua).value mustBe expectedResult
    }
  }
}
