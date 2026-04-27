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
import models.emailverification.ErrorModel
import models.requests.returns.ReturnsDataRequest
import models.returns.*
import pages.returns.EnterDutyAmountPage
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitReturnService @Inject()(submitReturnConnector: SubmitReturnConnector)
                                   (using ExecutionContext) {

  def submit(returnSubmission: ReturnCreateRequest)(using request: ReturnsDataRequest[?]): Future[Either[ErrorModel, ReturnSubmittedResponse]] = {

    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(session = request.session, request = request.request)

    submitReturnConnector.submitReturn(returnSubmission, request.enrolmentVpdId).map {
      case Left(error) => Left(error)
      case Right(response) => Right(response)
    }
  }

  def buildSubmission(ua: ReturnsUserAnswers): ReturnCreateRequest =

    val totalInMl = ua.get(EnterDutyAmountPage).fold(BigDecimal(0))(value => BigDecimal(value))

    // Temp value
    val zeroValue = BigDecimal(0)

    // Will need to either get or pass the period key here
    val periodKey = "26AF"

    // Will need to enhance this much more
    val totalDue = totalInMl - zeroValue

    ReturnCreateRequest(
      periodKey,
      VapingProductsProduced(Seq.empty, Seq.empty),
      TotalDutyDue(totalInMl, zeroValue, zeroValue, zeroValue, zeroValue, totalDue)
    )
}
