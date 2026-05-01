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
import pages.returns.DeclareDutyPage
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.all.{ActionItemViewModel, SummaryListRowViewModel, ValueViewModel, stringToKey}
import viewmodels.govuk.summarylist.FluentActionItem


class DeclareDutySummarySpec extends SpecBase with UnitSpec {

  val ua = returnsUserAnswers.set(DeclareDutyPage, true).success.value

  def summaryList(answer: String) = SummaryListRowViewModel(
    key = "declareDuty.checkYourAnswersLabel",
    value = ValueViewModel(Text(answer)),
    actions = Seq(
      ActionItemViewModel(Text("Change"), controllers.returns.routes.DeclareDutyController.onPageLoad(CheckMode).url)
        .withVisuallyHiddenText("declareDuty.change.hidden")
    )
  )

  "DeclareDutySummary" - {

    "must render correctly when 'Yes' selected" in {

      val expectedResult = summaryList("Yes")

      DeclareDutySummary.row(ua).value mustBe expectedResult
    }

    "must render correctly when 'No' selected" in {

      val expectedResult = summaryList("No")

      DeclareDutySummary.row(ua.set(DeclareDutyPage, false).success.value).value mustBe expectedResult
    }
  }
}
