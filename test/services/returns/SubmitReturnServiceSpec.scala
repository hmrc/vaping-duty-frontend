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
import models.requests.returns.ReturnsDataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.FakeRequest
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class SubmitReturnServiceSpec extends AnyFreeSpec with Matchers with TestData with SpecBase {

  val mockConnector: SubmitReturnConnector = mock[SubmitReturnConnector]

  given ReturnsDataRequest[?] = ReturnsDataRequest(FakeRequest(), vpdId, internalId, credId, periodKey, returnsUserAnswers)

  "SubmitReturnService must" - {

    "return Success when a preference is submitted successfully" in {

      when(mockConnector.submitReturn(any(), any())(any()))
        .thenReturn(Future.successful(testReturnSubmissionResponse))

      val result = SubmitReturnService(mockConnector, mockAppConfig).submit(returnsUserAnswers)

      whenReady(result) {
        _ mustBe testReturnSubmissionResponse
      }
   }

    "return Failure when there was an error submitting a preference" in {

      when(mockConnector.submitReturn(any(), any())(any()))
        .thenReturn(Future.failed(InternalServerException("error")))

      val result = SubmitReturnService(mockConnector, mockAppConfig).submit(returnsUserAnswers)

      whenReady(result.failed) { exception =>
        exception mustBe an[Exception]
      }
    }
  }
}
