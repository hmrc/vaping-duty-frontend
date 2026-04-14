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

import base.{SpecBase, UnitSpec}
import models.TaskStatus
import models.enrolment.EnrolmentUserAnswers
import play.api.i18n.Messages
import play.api.libs.json.JsObject
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.Aliases.TaskListItem

import java.time.Instant

class TaskListViewModelSpec extends UnitSpec with SpecBase {

  implicit val messages: Messages = messages(applicationBuilder(None).build())

  private val emptyEnrolmentUserAnswers = new EnrolmentUserAnswers("", JsObject.empty, Instant.now(), Instant.now())

  private def findRowById(sections: Seq[TaskListSectionViewModel], id: String): TaskListItem = {
    val allRows = sections.flatMap(_.rows)
    allRows.find(row =>
      row.status.tag.exists(_.attributes.get("id").contains(s"$id-status")) ||
        row.title.content.asHtml.body.contains(id)
    ).getOrElse {
      fail(s"Row with id '$id' not found; available rows: " +
        allRows.map(r => r.status.tag.flatMap(_.attributes.get("id"))).mkString(", "))
    }
  }

  "TaskListViewModel.sections" - {

    "returns four sections in the correct order" in {
      val application = applicationBuilder().build()
      running(application) {
        val sections = TaskListViewModel.sections(emptyEnrolmentUserAnswers)

        sections.length mustBe 4
        sections(0).headingKey mustBe "returns.taskList.section.declareDuty.heading"
        sections(1).headingKey mustBe "returns.taskList.section.declareAdjustments.heading"
        sections(2).headingKey mustBe "returns.taskList.section.dutySuspended.heading"
        sections(3).headingKey mustBe "returns.taskList.section.submitReturn.heading"
      }
    }

    "TaskRowViewModel.toTaskListItem" - {

      "suppresses href when status is TasksRemaining" in {
        val application = applicationBuilder().build()
        running(application) {
          val taskRow = TaskRowViewModel(
            id = "test-task",
            linkText = "Test Task",
            link = controllers.routes.JourneyRecoveryController.onPageLoad(),
            status = TaskStatus.TasksRemaining
          )

          val taskListItem = taskRow.toTaskListItem

          taskListItem.href mustBe None
        }
      }

      "uses govuk-task-list__status--cannot-start-yet class when status is TasksRemaining" in {
        val application = applicationBuilder().build()
        running(application) {
          val taskRow = TaskRowViewModel(
            id = "test-task",
            linkText = "Test Task",
            link = controllers.routes.JourneyRecoveryController.onPageLoad(),
            status = TaskStatus.TasksRemaining
          )

          val taskListItem = taskRow.toTaskListItem

          taskListItem.status.classes mustBe "govuk-task-list__status--cannot-start-yet"
        }
      }

      "does not use a tag when status is TasksRemaining" in {
        val application = applicationBuilder().build()
        running(application) {
          val taskRow = TaskRowViewModel(
            id = "test-task",
            linkText = "Test Task",
            link = controllers.routes.JourneyRecoveryController.onPageLoad(),
            status = TaskStatus.TasksRemaining
          )

          val taskListItem = taskRow.toTaskListItem

          taskListItem.status.tag mustBe None
        }
      }

      "has href when status is NotStarted" in {
        val application = applicationBuilder().build()
        running(application) {
          val taskRow = TaskRowViewModel(
            id = "test-task",
            linkText = "Test Task",
            link = controllers.routes.JourneyRecoveryController.onPageLoad(),
            status = TaskStatus.NotStarted
          )

          val taskListItem = taskRow.toTaskListItem

          taskListItem.href mustBe defined
          taskListItem.href.value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "uses tag with govuk-tag--blue class when status is NotStarted" in {
        val application = applicationBuilder().build()
        running(application) {
          val taskRow = TaskRowViewModel(
            id = "test-task",
            linkText = "Test Task",
            link = controllers.routes.JourneyRecoveryController.onPageLoad(),
            status = TaskStatus.NotStarted
          )

          val taskListItem = taskRow.toTaskListItem

          taskListItem.status.tag mustBe defined
          taskListItem.status.tag.value.classes must include("govuk-tag--blue")
        }
      }

      "uses tag with govuk-tag--light-blue class when status is InProgress" in {
        val application = applicationBuilder().build()
        running(application) {
          val taskRow = TaskRowViewModel(
            id = "test-task",
            linkText = "Test Task",
            link = controllers.routes.JourneyRecoveryController.onPageLoad(),
            status = TaskStatus.InProgress
          )

          val taskListItem = taskRow.toTaskListItem

          taskListItem.status.tag mustBe defined
          taskListItem.status.tag.value.classes must include("govuk-tag--light-blue")
        }
      }

      "has href when status is Completed" in {
        val application = applicationBuilder().build()
        running(application) {
          val taskRow = TaskRowViewModel(
            id = "test-task",
            linkText = "Test Task",
            link = controllers.routes.JourneyRecoveryController.onPageLoad(),
            status = TaskStatus.Completed
          )

          val taskListItem = taskRow.toTaskListItem

          taskListItem.href mustBe defined
          taskListItem.href.value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "does not use a tag when status is Completed" in {
        val application = applicationBuilder().build()
        running(application) {
          val taskRow = TaskRowViewModel(
            id = "test-task",
            linkText = "Test Task",
            link = controllers.routes.JourneyRecoveryController.onPageLoad(),
            status = TaskStatus.Completed
          )

          val taskListItem = taskRow.toTaskListItem

          taskListItem.status.tag mustBe None
        }
      }

      "includes hint text when provided" in {
        val application = applicationBuilder().build()
        running(application) {
          val taskRow = TaskRowViewModel(
            id = "test-task",
            linkText = "Test Task",
            link = controllers.routes.JourneyRecoveryController.onPageLoad(),
            status = TaskStatus.TasksRemaining,
            hint = Some("This is a hint")
          )

          val taskListItem = taskRow.toTaskListItem

          taskListItem.hint mustBe defined
        }
      }

      "does not include hint text when not provided" in {
        val application = applicationBuilder().build()
        running(application) {
          val taskRow = TaskRowViewModel(
            id = "test-task",
            linkText = "Test Task",
            link = controllers.routes.JourneyRecoveryController.onPageLoad(),
            status = TaskStatus.NotStarted
          )

          val taskListItem = taskRow.toTaskListItem

          taskListItem.hint mustBe None
        }
      }
    }
  }
}