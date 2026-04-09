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

package controllers.actions.enrolment

import models.requests.enrolment.{EnrolmentOptionalDataRequest, NoEnrolmentIdentifierRequest}
import play.api.mvc.ActionTransformer
import repositories.EnrolmentUserAnswersRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnrolmentDataRetrievalActionImpl @Inject()(
                                         enrolmentRepository: EnrolmentUserAnswersRepository
                                       )(implicit val executionContext: ExecutionContext) extends EnrolmentDataRetrievalAction {

  override protected def transform[A](request: NoEnrolmentIdentifierRequest[A]): Future[EnrolmentOptionalDataRequest[A]] = {
    
    enrolmentRepository.get(request.internalId).map {
      case None     => EnrolmentOptionalDataRequest(request, request.internalId, None)
      case Some(ua) => EnrolmentOptionalDataRequest(request, request.internalId, Some(ua))
    }
  }
}

trait EnrolmentDataRetrievalAction extends ActionTransformer[NoEnrolmentIdentifierRequest, EnrolmentOptionalDataRequest]
