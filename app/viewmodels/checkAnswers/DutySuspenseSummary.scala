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

import models.returns.ReturnsUserAnswers
import pages.contactPreference.EnterEmailPage
import pages.returns.DeclareDutyPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object DutySuspenseSummary {

  def returnsRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage).map { answer =>
      SummaryListRowViewModel(
        key = "",
        value = ValueViewModel(""),
        actions = Seq(
          ActionItemViewModel("site.change", controllers.returns.routes.BeforeYouStartController.onPageLoad().url)
            .withVisuallyHiddenText(messages(""))
        )
      )
    }

  def returnsRow2(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(EnterEmailPage).map { answer =>
      SummaryListRowViewModel(
        key = "",
        value = ValueViewModel(""),
        actions = Seq(
          ActionItemViewModel("site.change", controllers.returns.routes.BeforeYouStartController.onPageLoad().url)
            .withVisuallyHiddenText(messages(""))
        )
      )
    }

  def returnsRow3(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(EnterEmailPage).map { answer =>
      SummaryListRowViewModel(
        key = "",
        value = ValueViewModel(""),
        actions = Seq(
          ActionItemViewModel("site.change", controllers.returns.routes.BeforeYouStartController.onPageLoad().url)
            .withVisuallyHiddenText(messages(""))
        )
      )
    }
}
