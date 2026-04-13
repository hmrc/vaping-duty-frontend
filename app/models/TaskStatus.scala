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

package models

import models.{Enumerable, WithName}

sealed trait TaskStatus

object TaskStatus extends Enumerable.Implicits {
  case object CannotStart extends WithName("cannotStart") with TaskStatus
  case object NotStarted  extends WithName("notStarted") with TaskStatus
  case object InProgress  extends WithName("inProgress") with TaskStatus
  case object Completed   extends WithName("completed") with TaskStatus

  val values: Seq[TaskStatus] = Seq(
    CannotStart,
    NotStarted,
    InProgress,
    Completed
  )

  implicit val enumerable: Enumerable[TaskStatus] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
