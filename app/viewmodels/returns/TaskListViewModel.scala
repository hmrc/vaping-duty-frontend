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

import models.{TaskStatus, UserAnswers}
import models.enrolment.EnrolmentUserAnswers
import play.api.i18n.Messages

object TaskListViewModel {

  def sections(userAnswers: EnrolmentUserAnswers)(implicit messages: Messages): Seq[TaskListSectionViewModel] = {
    Seq(
      declareDutySection(userAnswers),
      declareAdjustmentsSection(userAnswers),
      dutySuspendedSection(userAnswers),
      submissionSection(userAnswers)
    )
  }


  private def declareDutySection(userAnswers: EnrolmentUserAnswers)(implicit messages: Messages): TaskListSectionViewModel = {
    TaskListSectionViewModel(
      headingKey = "returns.taskList.section.declareDuty.heading",
      rows = Seq(
        TaskRowViewModel(
          id       = "duty-task-1",
          linkText = messages("returns.taskList.section.declareDuty.task1"),
          link     = controllers.routes.JourneyRecoveryController.onPageLoad(),
          status   = TaskStatus.NotStarted
        ).toTaskListItem
      )
    )
  }

  private def declareAdjustmentsSection(userAnswers: EnrolmentUserAnswers)(implicit messages: Messages): TaskListSectionViewModel = {
    TaskListSectionViewModel(
      headingKey = "returns.taskList.section.declareAdjustments.heading",
      rows = Seq(
        TaskRowViewModel(
          id = "declareAdjustments-task-1",
          linkText = messages("returns.taskList.declareAdjustments.task1"),
          link = controllers.routes.JourneyRecoveryController.onPageLoad(),
          status = TaskStatus.NotStarted
        ).toTaskListItem,
        TaskRowViewModel(
          id = "declareAdjustments-task-2",
          linkText = messages("returns.taskList.declareAdjustments.task2"),
          link = controllers.routes.JourneyRecoveryController.onPageLoad(),
          status = TaskStatus.NotStarted
        ).toTaskListItem
      )
    )
  }


  private def dutySuspendedSection(userAnswers: EnrolmentUserAnswers)(implicit messages: Messages): TaskListSectionViewModel = {
    TaskListSectionViewModel(
      headingKey = "returns.taskList.section.dutySuspended.heading",
      rows = Seq(
        TaskRowViewModel(
          id = "duty-suspended-task-1",
          linkText = messages("returns.taskList.section.dutySuspended.task1"),
          link = controllers.routes.JourneyRecoveryController.onPageLoad(),
          status = TaskStatus.NotStarted
        ).toTaskListItem
      )
    )
  }

  def submissionSection(userAnswers: EnrolmentUserAnswers)(implicit messages: Messages): TaskListSectionViewModel = {
    TaskListSectionViewModel(
      headingKey = "returns.taskList.section.submitReturn.heading",
      rows = Seq(
        TaskRowViewModel(
          id = "submit",
          linkText = messages("returns.taskList.submitReturn.task1"),
          link = controllers.routes.JourneyRecoveryController.onPageLoad(),
          status = TaskStatus.TasksRemaining,
          hint = Some(messages("returns.taskList.submitReturn.task1.hint"))
        ).toTaskListItem
      )
    )
  }

}