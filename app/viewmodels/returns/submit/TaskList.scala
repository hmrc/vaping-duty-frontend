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

import models.returns.{AdjustmentsEligibility, ReturnsUserAnswers}
import models.{NormalMode, TaskStatus}
import play.api.i18n.Messages
import services.returns.TaskStatusService

object TaskList {

  def sections(userAnswers: ReturnsUserAnswers, adjustmentsEligibility: AdjustmentsEligibility)(implicit messages: Messages): Seq[TaskListSection] = {
    val periodKey = userAnswers.periodKey
    
    Seq(
      Some(declareDutySection(userAnswers, periodKey)),
      adjustmentsEligibility match {
        case AdjustmentsEligibility.Eligible    => Some(declareAdjustmentsSection(userAnswers, periodKey))
        case AdjustmentsEligibility.NotEligible => None
      },
      Some(dutySuspendedSection(userAnswers, periodKey)),
      Some(submissionSection(userAnswers, periodKey))
    ).flatten
  }


  private def declareDutySection(userAnswers: ReturnsUserAnswers, periodKey: String)(implicit messages: Messages): TaskListSection = {
    TaskListSection(
      headingKey = "returns.taskList.section.declareDuty.heading",
      rows = Seq(
        TaskRows(
          id       = "duty-task",
          linkText = messages("returns.taskList.section.declareDuty.task1"),
          link     = controllers.returns.submit.routes.DeclareDutyController.onPageLoad(NormalMode),
          status   = TaskStatusService.declareDutyTaskStatus(userAnswers),
          periodKey = Some(periodKey)
        ).toTaskListItem
      )
    )
  }

  private def declareAdjustmentsSection(userAnswers: ReturnsUserAnswers, periodKey: String)(implicit messages: Messages): TaskListSection = {
    TaskListSection(
      headingKey = "returns.taskList.section.declareAdjustments.heading",
      rows = Seq(
        TaskRows(
          id = "declareAdjustments-task-1",
          linkText = messages("returns.taskList.declareAdjustments.task1"),
          link = controllers.returns.submit.spoilt.routes.DeclareSpoiltProductsController.onPageLoad(NormalMode),
          status = TaskStatusService.declareSpoiltProductsTaskStatus(userAnswers),
          periodKey = Some(periodKey)
        ).toTaskListItem,
        TaskRows(
          id = "declareAdjustments-task-2",
          linkText = messages("returns.taskList.declareAdjustments.task2"),
          link = controllers.routes.JourneyRecoveryController.onPageLoad(),
          status = TaskStatus.NotStarted,
          periodKey = Some(periodKey)
        ).toTaskListItem
      )
    )
  }


  private def dutySuspendedSection(userAnswers: ReturnsUserAnswers, periodKey: String)(implicit messages: Messages): TaskListSection = {
    TaskListSection(
      headingKey = "returns.taskList.section.dutySuspended.heading",
      rows = Seq(
        TaskRows(
          id = "duty-suspended",
          linkText = messages("returns.taskList.section.dutySuspended.task1"),
          link = controllers.returns.submit.routes.DeclareDutySuspenseController.onPageLoad(NormalMode),
          status = TaskStatusService.dutySuspenseTaskStatus(userAnswers),
          periodKey = Some(periodKey)
        ).toTaskListItem
      )
    )
  }

  def submissionSection(userAnswers: ReturnsUserAnswers, periodKey: String)(implicit messages: Messages): TaskListSection = {
    TaskListSection(
      headingKey = "returns.taskList.section.submitReturn.heading",
      rows = Seq(
        TaskRows(
          id = "submit",
          linkText = messages("returns.taskList.submitReturn.task1"),
          link = controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad(),
          status = TaskStatusService.submitTaskStatus(userAnswers),
          hint = Some(messages("returns.taskList.submitReturn.task1.hint")),
          periodKey = Some(periodKey)
        ).toTaskListItem
      )
    )
  }

}