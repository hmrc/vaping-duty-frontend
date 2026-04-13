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

package viewmodels.returns

import models.TaskStatus
import models.UserAnswers
import models.enrolment.EnrolmentUserAnswers
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.*


object TaskListViewModel {
  def status(userAnswers: EnrolmentUserAnswers): TaskStatus = TaskStatus.NotStarted

  def rows(userAnswers: EnrolmentUserAnswers)(implicit messages: Messages): Seq[TaskListItem] = {

      Seq(TaskRowViewModel(
        id       = "id",
        linkText = "linkText(userAnswers)",
        link     = controllers.routes.JourneyRecoveryController.onPageLoad(),
        status   = status(userAnswers)
      ).toTaskListItem,
        TaskRowViewModel(
          id       = "id2",
          linkText = "linkText(userAnswers2)",
          link     = controllers.routes.JourneyRecoveryController.onPageLoad(),
          status   = status(userAnswers)
        ).toTaskListItem)

  }

  def submissionRow(userAnswers: EnrolmentUserAnswers)(implicit messages: Messages): TaskListItem = {
    TaskRowViewModel(
      id       = "id",
      linkText = "linkTextKey",
      link     = controllers.routes.JourneyRecoveryController.onPageLoad(),
      status   = status(userAnswers)
    ).toTaskListItem
  }
}
