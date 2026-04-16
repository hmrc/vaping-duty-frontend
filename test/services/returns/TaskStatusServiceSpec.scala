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

package services.returns

import base.SpecBase
import models.TaskStatus
import models.returns.ReturnsUserAnswers
import pages.{DeclareDutyPage, EnterDutyAmountPage}

class TaskStatusServiceSpec extends SpecBase {

  private val emptyAnswers = ReturnsUserAnswers(
    id = "test-id",
    startedTime = clock.instant(),
    lastUpdated = clock.instant()
  )

  "declareDutyTaskStatus" - {

    "must return NotStarted when no pages are answered" in {
      val result = TaskStatusService.declareDutyTaskStatus(emptyAnswers)
      result mustBe TaskStatus.NotStarted
    }

    "must return InProgress when DeclareDutyPage is answered but EnterDutyAmountPage is not" in {
      val answers = emptyAnswers
        .set(DeclareDutyPage, true)
        .success
        .value

      val result = TaskStatusService.declareDutyTaskStatus(answers)
      result mustBe TaskStatus.InProgress
    }

    "must return Completed when both DeclareDutyPage and EnterDutyAmountPage are answered" in {
      val answers = emptyAnswers
        .set(DeclareDutyPage, true)
        .flatMap(_.set(EnterDutyAmountPage, 1000))
        .success
        .value

      val result = TaskStatusService.declareDutyTaskStatus(answers)
      result mustBe TaskStatus.Completed
    }

    "must return NotStarted when only EnterDutyAmountPage is answered" in {
      val answers = emptyAnswers
        .set(EnterDutyAmountPage, 1000)
        .success
        .value

      val result = TaskStatusService.declareDutyTaskStatus(answers)
      result mustBe TaskStatus.NotStarted
    }
  }

  "allTasksCompleted" - {

    "must return false when no tasks are completed" in {
      val result = TaskStatusService.allTasksCompleted(emptyAnswers)
      result mustBe false
    }

    "must return false when declare duty task is InProgress" in {
      val answers = emptyAnswers
        .set(DeclareDutyPage, true)
        .success
        .value

      val result = TaskStatusService.allTasksCompleted(answers)
      result mustBe false
    }

    "must return true when declare duty task is Completed" in {
      val answers = emptyAnswers
        .set(DeclareDutyPage, true)
        .flatMap(_.set(EnterDutyAmountPage, 1000))
        .success
        .value

      val result = TaskStatusService.allTasksCompleted(answers)
      result mustBe true
    }
  }

  "submitTaskStatus" - {

    "must return TasksRemaining when no tasks are completed" in {
      val result = TaskStatusService.submitTaskStatus(emptyAnswers)
      result mustBe TaskStatus.TasksRemaining
    }

    "must return TasksRemaining when declare duty task is InProgress" in {
      val answers = emptyAnswers
        .set(DeclareDutyPage, true)
        .success
        .value

      val result = TaskStatusService.submitTaskStatus(answers)
      result mustBe TaskStatus.TasksRemaining
    }

    "must return NotStarted when all tasks are Completed" in {
      val answers = emptyAnswers
        .set(DeclareDutyPage, true)
        .flatMap(_.set(EnterDutyAmountPage, 1000))
        .success
        .value

      val result = TaskStatusService.submitTaskStatus(answers)
      result mustBe TaskStatus.NotStarted
    }
  }
}