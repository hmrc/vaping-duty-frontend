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
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HasEnrolmentActionImpl @Inject()(implicit val executionContext: ExecutionContext) extends HasEnrolmentAction {

  override protected def refine[A](request: NoEnrolmentIdentifierRequest[A]): Future[Either[Result, NoEnrolmentIdentifierRequest[A]]] = {

    request.enrolmentVpdId match {
      case Some(vpdId) =>
        Future.successful(Right(NoEnrolmentIdentifierRequest(request, Some(vpdId), request.groupId, request.userId)))
      case None =>
        // Placeholder redirect
        Future.successful(Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))) // Our graceful failure page    }
    }
  }
}

trait HasEnrolmentAction extends ActionRefiner[NoEnrolmentIdentifierRequest, NoEnrolmentIdentifierRequest]
