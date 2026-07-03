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

import models.TaskStatus
import models.returns.ReturnsUserAnswers
import pages.returns.{DeclareDutyPage, DeclareDutySuspensePage, DeclareSpoiltProductsPage, EnterDutyAmountPage, EnterDutySuspensePage, SpoiltVolumeByPeriodPage}
import pages.returns.adjustments.{AdjustmentListPage, DeclareAdjustmentPage}

object TaskStatusService {

  def declareDutyTaskStatus(answers: ReturnsUserAnswers): TaskStatus = {
    (answers.get(DeclareDutyPage), answers.get(EnterDutyAmountPage)) match {
      case (None, _)             => TaskStatus.NotStarted
      case (Some(false), _)      => TaskStatus.Completed
      case (Some(true), None)    => TaskStatus.InProgress
      case (Some(true), Some(_)) => TaskStatus.Completed
    }
  }

  def dutySuspenseTaskStatus(answers: ReturnsUserAnswers): TaskStatus = {
    (answers.get(DeclareDutySuspensePage), answers.get(EnterDutySuspensePage)) match {
      case (None, _)              => TaskStatus.NotStarted
      case (Some(false), _)       => TaskStatus.Completed
      case (Some(true), None)     => TaskStatus.InProgress
      case (Some(true), Some(_))  => TaskStatus.Completed
    }
  }

  def declareSpoiltProductsTaskStatus(answers: ReturnsUserAnswers): TaskStatus = {
    (answers.get(DeclareSpoiltProductsPage), answers.get(SpoiltVolumeByPeriodPage)) match {
      case (None, _)             => TaskStatus.NotStarted
      case (Some(false), _)      => TaskStatus.Completed
      case (Some(true), None)    => TaskStatus.InProgress
      case (Some(true), Some(_)) => TaskStatus.Completed
    }
  }

  def declareAdjustmentsTaskStatus(answers: ReturnsUserAnswers): TaskStatus = {
    (answers.get(DeclareAdjustmentPage), answers.get(AdjustmentListPage)) match {
      case (None, _) => TaskStatus.NotStarted
      case (Some(false), _) => TaskStatus.Completed
      case (Some(true), None) => TaskStatus.InProgress
      case (Some(true), Some(list)) if list.adjustments.nonEmpty => TaskStatus.Completed
      case (Some(true), _) => TaskStatus.InProgress
    }
  }

  def allTasksCompleted(answers: ReturnsUserAnswers): Boolean = {
    declareDutyTaskStatus(answers) == TaskStatus.Completed &&
    declareSpoiltProductsTaskStatus(answers) == TaskStatus.Completed &&
    declareAdjustmentsTaskStatus(answers) == TaskStatus.Completed &&
    dutySuspenseTaskStatus(answers) == TaskStatus.Completed
  }

  def submitTaskStatus(answers: ReturnsUserAnswers): TaskStatus = {
    if (allTasksCompleted(answers)) TaskStatus.NotStarted
    else TaskStatus.TasksRemaining
  }
}