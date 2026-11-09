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
import models.returns.{AdjustmentsEligibility, SpoiltVolumeByPeriod}
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import pages.returns.{DeclareDutyPage, DeclareDutySuspensePage, DeclareSpoiltProductsPage, SpoiltVolumeByPeriodPage}
import pages.returns.yourdetails.{YourBusinessEmailPage, YourFullNamePage, YourRolePage}
import pages.returns.adjustments.{AdjustmentListPage, DeclareAdjustmentPage}
import play.api.test.Helpers.*
import viewmodels.returns.submit.{TaskList, TaskRows}
import models.identifiers.PeriodKey

class TaskListSpec extends UnitSpec with SpecBase {

  private val yourDetailsAnswers = returnsUserAnswers
    .set(YourFullNamePage, "John Smith")
    .flatMap(_.set(YourRolePage, "Director"))
    .flatMap(_.set(YourBusinessEmailPage, "john@example.com"))
    .success.value

  "TaskListViewModel.sections" - {

    "returns four sections in the correct order when adjustments are eligible" in {
      val application = applicationBuilder().build()
      running(application) {
        val sections = TaskList.sections(returnsUserAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = false)

        sections.length mustBe 4
        sections(0).headingKey mustBe "returns.taskList.section.declareDuty.heading"
        sections(1).headingKey mustBe "returns.taskList.section.declareAdjustments.heading"
        sections(2).headingKey mustBe "returns.taskList.section.dutySuspended.heading"
        sections(3).headingKey mustBe "returns.taskList.section.submitReturn.heading"
      }
    }

    "returns three sections excluding adjustments when not eligible" in {
      val application = applicationBuilder().build()
      running(application) {
        val sections = TaskList.sections(returnsUserAnswers, AdjustmentsEligibility.NotEligible, yourDetailsEnabled = false)

        sections.length mustBe 3
        sections(0).headingKey mustBe "returns.taskList.section.declareDuty.heading"
        sections(1).headingKey mustBe "returns.taskList.section.dutySuspended.heading"
        sections(2).headingKey mustBe "returns.taskList.section.submitReturn.heading"

        // Verify adjustments section is NOT present
        sections.map(_.headingKey) must not contain "returns.taskList.section.declareAdjustments.heading"
      }
    }

    "your details section" - {

      "is placed before submit return when enabled and adjustments are eligible" in {
        val application = applicationBuilder().build()
        running(application) {
          val sections = TaskList.sections(returnsUserAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = true)

          sections.length mustBe 5
          sections(3).headingKey mustBe "returns.taskList.section.yourDetails.heading"
          sections(4).headingKey mustBe "returns.taskList.section.submitReturn.heading"
        }
      }

      "is placed before submit return when enabled and adjustments are not eligible" in {
        val application = applicationBuilder().build()
        running(application) {
          val sections = TaskList.sections(returnsUserAnswers, AdjustmentsEligibility.NotEligible, yourDetailsEnabled = true)

          sections.length mustBe 4
          sections(2).headingKey mustBe "returns.taskList.section.yourDetails.heading"
          sections(3).headingKey mustBe "returns.taskList.section.submitReturn.heading"
        }
      }

      "is not shown when disabled" in {
        val application = applicationBuilder().build()
        running(application) {
          val sections = TaskList.sections(returnsUserAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = false)

          sections.map(_.headingKey) must not contain "returns.taskList.section.yourDetails.heading"
        }
      }

      "shows a not started task linking to the full name page" in {
        val application = applicationBuilder().build()
        running(application) {
          val sections = TaskList.sections(returnsUserAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = true)
          val yourDetailsSection = sections(3)
          val yourDetailsTask = yourDetailsSection.rows.head

          yourDetailsSection.idPrefix mustBe "your-details-task"
          yourDetailsSection.rows.length mustBe 1
          yourDetailsTask.href.value must include("/your-details/what-is-your-full-name")
          yourDetailsTask.status.tag.value.content.asHtml.toString mustBe messages(application)("returns.taskList.taskStatus.notStarted")
          yourDetailsTask.status.tag.value.classes mustBe "govuk-tag--blue"
          yourDetailsTask.status.tag.value.attributes mustBe Map("id" -> "your-details-status")
        }
      }

      "shows in progress and links to the role page when only the full name is answered" in {
        val application = applicationBuilder().build()
        running(application) {
          val userAnswers = returnsUserAnswers.set(YourFullNamePage, "John Smith").success.value

          val sections = TaskList.sections(userAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = true)
          val yourDetailsTask = sections(3).rows.head

          yourDetailsTask.href.value must include("/your-details/what-is-your-role-in-the-company")
          yourDetailsTask.status.tag.value.content.asHtml.toString mustBe messages(application)("returns.taskList.taskStatus.inProgress")
        }
      }

      "links to the email page when the full name and role are answered" in {
        val application = applicationBuilder().build()
        running(application) {
          val userAnswers = returnsUserAnswers
            .set(YourFullNamePage, "John Smith")
            .flatMap(_.set(YourRolePage, "Director"))
            .success.value

          val sections = TaskList.sections(userAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = true)
          val yourDetailsTask = sections(3).rows.head

          yourDetailsTask.href.value must include("/your-details/what-is-your-business-email-address")
        }
      }

      "shows completed and links to the check your answers page when all questions are answered" in {
        val application = applicationBuilder().build()
        running(application) {
          val sections = TaskList.sections(yourDetailsAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = true)
          val yourDetailsTask = sections(3).rows.head

          yourDetailsTask.href.value must include("/your-details/check-your-answers")
          yourDetailsTask.status.content.asHtml.toString mustBe messages(application)("returns.taskList.taskStatus.completed")
        }
      }

      "keeps the submit task as tasks remaining until your details is completed" in {
        val application = applicationBuilder().build()
        running(application) {
          val otherTasksCompleted = returnsUserAnswers
            .set(DeclareDutyPage, false)
            .flatMap(_.set(DeclareDutySuspensePage, false))
            .success.value

          val sections = TaskList.sections(otherTasksCompleted, AdjustmentsEligibility.NotEligible, yourDetailsEnabled = true)
          val submitTask = sections(3).rows.head

          submitTask.href mustBe None
          submitTask.status.content.asHtml.toString mustBe messages(application)("returns.taskList.taskStatus.tasksRemaining")
        }
      }

      "allows the submit task to start once your details is completed" in {
        val application = applicationBuilder().build()
        running(application) {
          val allTasksCompleted = yourDetailsAnswers
            .set(DeclareDutyPage, false)
            .flatMap(_.set(DeclareDutySuspensePage, false))
            .success.value

          val sections = TaskList.sections(allTasksCompleted, AdjustmentsEligibility.NotEligible, yourDetailsEnabled = true)
          val submitTask = sections(3).rows.head

          submitTask.href.value must include("/complete-return/check-your-answers")
          submitTask.status.tag.value.content.asHtml.toString mustBe messages(application)("returns.taskList.taskStatus.notStarted")
        }
      }

      "does not block the submit task when the feature is disabled" in {
        val application = applicationBuilder().build()
        running(application) {
          val otherTasksCompleted = returnsUserAnswers
            .set(DeclareDutyPage, false)
            .flatMap(_.set(DeclareDutySuspensePage, false))
            .success.value

          val sections = TaskList.sections(otherTasksCompleted, AdjustmentsEligibility.NotEligible, yourDetailsEnabled = false)
          val submitTask = sections(2).rows.head

          submitTask.status.tag.value.content.asHtml.toString mustBe messages(application)("returns.taskList.taskStatus.notStarted")
        }
      }
    }

    "TaskRowViewModel.toTaskListItem" - {

      "suppresses href when status is TasksRemaining" in {
        val application = applicationBuilder().build()
        running(application) {
          val taskRow = TaskRows(
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
          val taskRow = TaskRows(
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
          val taskRow = TaskRows(
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
          val taskRow = TaskRows(
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
          val taskRow = TaskRows(
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
          val taskRow = TaskRows(
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
          val taskRow = TaskRows(
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
          val taskRow = TaskRows(
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
          val taskRow = TaskRows(
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
          val taskRow = TaskRows(
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

    "declareAdjustmentsSection" - {

      "must link to DeclareAdjustmentQuestionController when no adjustment data exists" in {
        val application = applicationBuilder().build()
        running(application) {
          val sections = TaskList.sections(returnsUserAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = false)

          val adjustmentsSection = sections(1)
          val adjustmentsTask = adjustmentsSection.rows(1)

          adjustmentsTask.href.get must include("/declare-adjustments")
        }
      }

      "must link to AdjustmentCheckYourAnswersController when adjustment data exists" in {
        val application = applicationBuilder().build()
        running(application) {
          val adjustmentEntry = AdjustmentEntry(
            period = PeriodKey("24KI"),
            adjustmentType = AdjustmentType.UnderDeclared,
            volumeInMl = BigDecimal(1000)
          )
          val adjustmentList = AdjustmentList(Seq(adjustmentEntry))
          val userAnswers = returnsUserAnswers
            .set(DeclareAdjustmentPage, true).success.value
            .set(AdjustmentListPage, adjustmentList).success.value

          val sections = TaskList.sections(userAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = false)

          val adjustmentsSection = sections(1)
          val adjustmentsTask = adjustmentsSection.rows(1)

          adjustmentsTask.href.get must include("/check-your-answers")
        }
      }

      "must link back to DeclareAdjustmentQuestionController when adjustment declared but no list exists yet" in {
        val application = applicationBuilder().build()
        running(application) {
          val userAnswers = returnsUserAnswers
            .set(DeclareAdjustmentPage, true).success.value
            .remove(AdjustmentListPage).success.value

          val sections = TaskList.sections(userAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = false)

          val adjustmentsSection = sections(1)
          val adjustmentsTask = adjustmentsSection.rows(1)

          adjustmentsTask.href.get must include("/declare-adjustments")
        }
      }

      "must link back to DeclareAdjustmentQuestionController when adjustments declared but with an empty list" in {
        // This occurs when a user chooses 'Yes' to the DeclareAdjustmentQuestionController,
        // navigates back to the task list and clicks to resume the declare over/under adjustments journey, 
        // but has not yet entered any volume data.
        val application = applicationBuilder().build()
        running(application) {
          val userAnswers = returnsUserAnswers
            .set(DeclareAdjustmentPage, true).success.value
            .set(AdjustmentListPage, AdjustmentList(Seq.empty)).success.value

          val sections = TaskList.sections(userAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = false)

          val adjustmentsSection = sections(1)
          val adjustmentsTask = adjustmentsSection.rows(1)

          adjustmentsTask.href.get must include("/declare-adjustments")
        }
      }

      "must link to AdjustmentCheckYourAnswersController when adjustments has been declared as false" in {
        val application = applicationBuilder().build()
        running(application) {
          val userAnswers = returnsUserAnswers
            .set(DeclareAdjustmentPage, false).success.value

          val sections = TaskList.sections(userAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = false)

          val adjustmentsSection = sections(1)
          val adjustmentsTask = adjustmentsSection.rows(1)

          adjustmentsTask.href.get must include("/check-your-answers")
        }
      }
    }

    "declareSpoiltProductsSection" - {

      "must link to DeclareSpoiltProductsController when no spoilt data exists" in {
        val application = applicationBuilder().build()
        running(application) {
          val sections = TaskList.sections(returnsUserAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = false)

          val adjustmentsSection = sections(1)
          val spoiltTask = adjustmentsSection.rows(0)

          spoiltTask.href.get must include("/declare-spoilt-products")
        }
      }

      "must link to SpoiltCheckYourAnswersController when spoilt data exists" in {
        val application = applicationBuilder().build()
        running(application) {
          val spoiltList = List(SpoiltVolumeByPeriod(volume = BigDecimal(1000), periodKey = PeriodKey("24KI")))
          val userAnswers = returnsUserAnswers
            .set(DeclareSpoiltProductsPage, true).success.value
            .set(SpoiltVolumeByPeriodPage, spoiltList).success.value

          val sections = TaskList.sections(userAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = false)

          val adjustmentsSection = sections(1)
          val spoiltTask = adjustmentsSection.rows(0)

          spoiltTask.href.get must include("/check-your-spoilt-products-answers")
        }
      }

      "must link back to DeclareSpoiltProductsController when spoilt declared but no volume exists yet" in {
        // This occurs when a user chooses 'Yes' to the DeclareSpoiltProductsPage,
        // navigates back to the task list and clicks to resume the declare spoilt products journey, 
        // but has not yet entered any volume data.
        val application = applicationBuilder().build()
        running(application) {
          val userAnswers = returnsUserAnswers
            .set(DeclareSpoiltProductsPage, true).success.value
            .remove(SpoiltVolumeByPeriodPage).success.value

          val sections = TaskList.sections(userAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = false)

          val adjustmentsSection = sections(1)
          val spoiltTask = adjustmentsSection.rows(0)

          spoiltTask.href.get must include("/declare-spoilt-products")
        }
      }

      "must link back to DeclareSpoiltProductsController when spoilt has been declared but with an empty list" in {
        val application = applicationBuilder().build()
        running(application) {
          val userAnswers = returnsUserAnswers
            .set(DeclareSpoiltProductsPage, true).success.value
            .set(SpoiltVolumeByPeriodPage, List.empty).success.value

          val sections = TaskList.sections(userAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = false)

          val adjustmentsSection = sections(1)
          val spoiltTask = adjustmentsSection.rows(0)

          spoiltTask.href.get must include("/declare-spoilt-products")
        }
      }

      "must link to SpoiltCheckYourAnswersController when spoilt has been declared as false" in {
        val application = applicationBuilder().build()
        running(application) {
          val userAnswers = returnsUserAnswers
            .set(DeclareSpoiltProductsPage, false).success.value

          val sections = TaskList.sections(userAnswers, AdjustmentsEligibility.Eligible, yourDetailsEnabled = false)

          val adjustmentsSection = sections(1)
          val spoiltTask = adjustmentsSection.rows(0)

          spoiltTask.href.get must include("/check-your-spoilt-products-answers")
        }
      }
    }
  }
}
