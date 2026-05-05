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

package viewmodels.returns.submit

import models.returns.ReturnsUserAnswers
import models.{NormalMode, TaskStatus}
import play.api.i18n.Messages
import services.returns.TaskStatusService

object TaskList {

  def sections(userAnswers: ReturnsUserAnswers)(implicit messages: Messages): Seq[TaskListSection] = {
    Seq(
      declareDutySection(userAnswers),
      declareAdjustmentsSection(userAnswers),
      dutySuspendedSection(userAnswers),
      submissionSection(userAnswers)
    )
  }


  private def declareDutySection(userAnswers: ReturnsUserAnswers)(implicit messages: Messages): TaskListSection = {
    TaskListSection(
      headingKey = "returns.taskList.section.declareDuty.heading",
      rows = Seq(
        TaskRows(
          id       = "duty-task-1",
          linkText = messages("returns.taskList.section.declareDuty.task1"),
          link     = controllers.returns.submit.routes.DeclareDutyController.onPageLoad(NormalMode),
          status   = TaskStatusService.declareDutyTaskStatus(userAnswers)
        ).toTaskListItem
      )
    )
  }

  private def declareAdjustmentsSection(userAnswers: ReturnsUserAnswers)(implicit messages: Messages): TaskListSection = {
    TaskListSection(
      headingKey = "returns.taskList.section.declareAdjustments.heading",
      rows = Seq(
        TaskRows(
          id = "declareAdjustments-task-1",
          linkText = messages("returns.taskList.declareAdjustments.task1"),
          link = controllers.routes.JourneyRecoveryController.onPageLoad(),
          status = TaskStatus.NotStarted
        ).toTaskListItem,
        TaskRows(
          id = "declareAdjustments-task-2",
          linkText = messages("returns.taskList.declareAdjustments.task2"),
          link = controllers.routes.JourneyRecoveryController.onPageLoad(),
          status = TaskStatus.NotStarted
        ).toTaskListItem
      )
    )
  }


  private def dutySuspendedSection(userAnswers: ReturnsUserAnswers)(implicit messages: Messages): TaskListSection = {
    TaskListSection(
      headingKey = "returns.taskList.section.dutySuspended.heading",
      rows = Seq(
        TaskRows(
          id = "duty-suspended-task-1",
          linkText = messages("returns.taskList.section.dutySuspended.task1"),
          link = controllers.routes.JourneyRecoveryController.onPageLoad(),
          status = TaskStatus.NotStarted
        ).toTaskListItem
      )
    )
  }

  def submissionSection(userAnswers: ReturnsUserAnswers)(implicit messages: Messages): TaskListSection = {
    TaskListSection(
      headingKey = "returns.taskList.section.submitReturn.heading",
      rows = Seq(
        TaskRows(
          id = "submit",
          linkText = messages("returns.taskList.submitReturn.task1"),
          link = controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad(),
          status = TaskStatusService.submitTaskStatus(userAnswers),
          hint = Some(messages("returns.taskList.submitReturn.task1.hint"))
        ).toTaskListItem
      )
    )
  }

}