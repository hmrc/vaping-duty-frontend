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

package controllers.actions

import models.requests.NoEnrolmentIdentifierRequest
import play.api.mvc.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeHasEnrolmentAction @Inject() extends HasEnrolmentAction {
  
  val optVpdId: Option[String] = Some("vpdId")
  val groupId: String = "groupid"
  val internalId: String = "id"

  override def refine[A](request: NoEnrolmentIdentifierRequest[A]): Future[Either[Result, NoEnrolmentIdentifierRequest[A]]] =
    Future.successful(Right(NoEnrolmentIdentifierRequest(request, optVpdId, groupId, internalId)))

  
  override protected def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}
