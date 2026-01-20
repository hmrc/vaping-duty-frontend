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

import base.SpecBase
import connectors.UserAnswersConnector
import models.{ContactPreferenceUserAnswers, UserAnswers}
import models.requests.{IdentifierRequest, OptionalDataRequest}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRetrievalActionSpec extends SpecBase with MockitoSugar {

  class Harness(contactPreferenceUserAnswersConnector: UserAnswersConnector) extends DataRetrievalActionImpl(contactPreferenceUserAnswersConnector) {
    def callTransform[A](request: IdentifierRequest[A]): Future[OptionalDataRequest[A]] = transform(request)
  }

  "Data Retrieval Action" - {

    "when there is no data in the cache" - {

      "must set userAnswers to 'None' in the request" in {

        val sessionRepository = mock[UserAnswersConnector]
        when(sessionRepository.get("id")) thenReturn Future(Left(UpstreamErrorResponse.Upstream4xxResponse))
        val action = new Harness(sessionRepository)

        val result = action.callTransform(IdentifierRequest(FakeRequest(), "vpid", "vpgroup", "id")).futureValue

        result.userAnswers must not be defined
      }
    }

    "when there is data in the cache" - {

      "must build a userAnswers object and add it to the request" in {

        val sessionRepository = mock[UserAnswersConnector]
        when(sessionRepository.get("id")) thenReturn Future(Right(userAnswers))
        val action = new Harness(sessionRepository)

        val result = action.callTransform(new IdentifierRequest(FakeRequest(), "vpid", "vpgroup", "id")).futureValue

        result.userAnswers mustBe defined
      }
    }
  }
}
