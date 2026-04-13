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
import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.Aliases.*
import viewmodels.govuk.all.HintViewModel

final case class TaskRowViewModel(
                                   id: String,
                                   linkText: String,
                                   link: Call,
                                   status: TaskStatus,
                                   hint: Option[String] = None
                                 ) {

  def toTaskListItem(implicit messages: Messages): TaskListItem = {
    val statusBlock: TaskListItemStatus = {
      val (msgKey, extraClasses) = status match {
        case TaskStatus.TasksRemaining => ("returns.taskList.taskStatus.tasksRemaining", "govuk-task-list__status--cannot-start-yet")
        case TaskStatus.NotStarted => ("returns.taskList.taskStatus.notStarted", "govuk-tag--blue")
        case TaskStatus.InProgress => ("returns.taskList.taskStatus.inProgress", "govuk-tag--light-blue")
        case TaskStatus.Completed => ("returns.taskList.taskStatus.completed", "")
      }

      taskListStatusType(msgKey, extraClasses)
    }

    TaskListItem(
      title = TaskListItemTitle(Text(linkText)),
      href = if (status != TaskStatus.TasksRemaining) Some(link.url) else None,
      hint = hint.map(h => HintViewModel(Text(h))),
      status = statusBlock,
      classes = "govuk-task-list__status--cannot-start-yet"
    )
  }

  private def taskListStatusType(msgKey: String, extraClasses: String)(implicit messages: Messages): TaskListItemStatus = {
    status match {
      case TaskStatus.TasksRemaining => TaskListItemStatus(
        content = Text(messages(msgKey)),
        classes = "govuk-task-list__status--cannot-start-yet"
      )
      case TaskStatus.Completed => TaskListItemStatus(
        content = Text(messages(msgKey))
      )
      case _ => TaskListItemStatus(
        tag =
          Some(
            Tag(
              content = Text(messages(msgKey)),
              classes = extraClasses,
              attributes = Map("id" -> s"$id-status")
            )
          )
      )
    }

  }
}
