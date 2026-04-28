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
import connectors.returns.SubmitReturnConnector
import data.TestData
import models.emailverification.ErrorModel
import models.requests.contactPreference.DataRequest
import models.requests.returns.ReturnsDataRequest
import models.returns.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.returns.EnterDutyAmountPage
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.FakeRequest

import scala.concurrent.Future

class SubmitReturnServiceSpec extends AnyFreeSpec with Matchers with TestData with SpecBase {

  implicit val request: DataRequest[?] = DataRequest(FakeRequest(), vpdId, internalId, credId, emptyUserAnswers)

  val mockConnector: SubmitReturnConnector = mock[SubmitReturnConnector]

  val totalInMl = returnsUserAnswers.get(EnterDutyAmountPage).fold(BigDecimal(0))(value => BigDecimal(value))

  // Temp value
  val zeroValue = BigDecimal(0)

  // Will need to either get or pass the period key here
  val periodKey = "26AF"

  // Will need to enhance this much more
  val totalDue = totalInMl - zeroValue

  val createRequest = ReturnCreateRequest(
    periodKey,
    VapingProductsProduced(Seq.empty, Seq.empty),
    TotalDutyDue(totalInMl, zeroValue, zeroValue, zeroValue, zeroValue, totalDue)
  )

  given ReturnsDataRequest[?] = ReturnsDataRequest(FakeRequest(), vpdId, internalId, credId, returnsUserAnswers)

  "SubmitReturnService must" - {

    "return Success when a preference is submitted successfully" in {

      when(mockConnector.submitReturn(any(), any())(any()))
        .thenReturn(Future.successful(Right(testReturnSubmissionResponse)))

      val result = SubmitReturnService(mockConnector).submit(createRequest)

      whenReady(result) {
        _ mustBe Right(testReturnSubmissionResponse)
      }
   }

    "return Failure when there was an error submitting a preference" in {

      when(mockConnector.submitReturn(any(), any())(any()))
        .thenReturn(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, "There was a problem"))))

      val result = SubmitReturnService(mockConnector).submit(createRequest)

      whenReady(result) {
        _ mustBe Left(ErrorModel(INTERNAL_SERVER_ERROR, "There was a problem"))
      }
    }

    "return a create request" in {

      val result = SubmitReturnService(mockConnector).buildSubmission(returnsUserAnswers)

      result mustBe createRequest
    }

    "return a create request with a value present" in {

      val result = SubmitReturnService(mockConnector)
        .buildSubmission(returnsUserAnswers.set(EnterDutyAmountPage, 1000).success.value)

      result mustBe createRequest.copy(totalDutyDue = TotalDutyDue(totalDutyDueVapingProducts = BigDecimal(1000),BigDecimal(0),BigDecimal(0),BigDecimal(0),BigDecimal(0), totalDutyDue = BigDecimal(1000)))
    }
  }
}
