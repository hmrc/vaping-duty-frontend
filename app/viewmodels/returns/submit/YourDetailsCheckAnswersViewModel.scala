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

import models.CheckMode
import models.identifiers.PeriodKey
import models.returns.ReturnsUserAnswers
import pages.returns.yourdetails.{YourBusinessEmailPage, YourFullNamePage, YourRolePage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object YourDetailsCheckAnswersViewModel {

  def apply(userAnswers: ReturnsUserAnswers, periodKey: PeriodKey)(implicit messages: Messages): Option[SummaryList] =
    for {
      fullName <- userAnswers.get(YourFullNamePage)
      role     <- userAnswers.get(YourRolePage)
      email    <- userAnswers.get(YourBusinessEmailPage)
    } yield SummaryList(rows = Seq(
      row("fullName", fullName, controllers.returns.submit.yourdetails.routes.YourFullNameController.onPageLoad(CheckMode).url, periodKey),
      row("role", role, controllers.returns.submit.yourdetails.routes.YourRoleController.onPageLoad(CheckMode).url, periodKey),
      row("email", email, controllers.returns.submit.yourdetails.routes.YourBusinessEmailController.onPageLoad(CheckMode).url, periodKey)
    ))

  private def row(key: String, answer: String, changeUrl: String, periodKey: PeriodKey)
                 (implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = s"returns.yourDetails.checkAnswers.$key",
      value = ValueViewModel(Text(answer)),
      actions = Seq(
        ActionItemViewModel("site.change", s"$changeUrl?period=${periodKey.value}")
          .withVisuallyHiddenText(messages(s"returns.yourDetails.checkAnswers.$key.change.hidden"))
      )
    )
}
