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

import connectors.returns.SubmitReturnConnector
import models.requests.contactPreference.DataRequest
import models.requests.returns.ReturnsDataRequest
import models.returns.{Failure, ResponseStatus, ReturnCreateRequest, ReturnSubmittedResponse, Success}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitReturnService @Inject()(submitReturnConnector: SubmitReturnConnector)
                                   (implicit ec: ExecutionContext) {

  def submit(returnSubmission: ReturnCreateRequest, request: ReturnsDataRequest[?]): Future[ResponseStatus] = {

    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(session = request.session, request = request.request)

    submitReturnConnector.submitReturn(returnSubmission, request.enrolmentVpdId).map {
      case Left(error)     => new Failure
      case Right(response) => new Success
    }
  }
}
