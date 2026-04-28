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

package services.returns

import base.SpecBase
import connectors.returns.ReturnsUserAnswersConnector
import models.emailverification.ErrorModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

class ReturnsUserAnswersServiceSpec extends SpecBase {

  "set" - {
    "must return a success if the operation was successful" in new Setup {
      when(mockUserAnswersConnector.set(any())(any()))
        .thenReturn(Future.successful(HttpResponse(status = OK, body = "success response")))

      whenReady(testService.set(returnsUserAnswers)) {
        _.toString mustBe Right(HttpResponse(status = OK, body = "success response")).toString
      }
    }
    "must return an error if the operation was unsuccessful" in new Setup {
      when(mockUserAnswersConnector.set(any())(any()))
        .thenReturn(Future.successful(HttpResponse(status = INTERNAL_SERVER_ERROR, body = "error response")))

      whenReady(testService.set(returnsUserAnswers)) {
        _ mustBe Left(
          ErrorModel(status = INTERNAL_SERVER_ERROR, message = "Unexpected error setting user answers, status: 500")
        )
      }
    }
  }

  "get" - {
    "must return a success if the operation was successful" in new Setup {
      when(mockUserAnswersConnector.get(any())(any()))
        .thenReturn(Future.successful(Right(userAnswers)))

      whenReady(testService.get(internalId)) {
        _ mustBe Right(userAnswers)
      }
    }

    "must return an error if the operation was unsuccessful" in new Setup {
      when(mockUserAnswersConnector.get(any())(any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("There was a problem", INTERNAL_SERVER_ERROR))))

      whenReady(testService.get(internalId)) {
        _ mustBe Left(UpstreamErrorResponse("There was a problem", INTERNAL_SERVER_ERROR))
      }
    }
  }

  "clear" - {
    "must return a success if the operation was successful" in new Setup {
      when(mockUserAnswersConnector.clear(any())(any()))
        .thenReturn(Future.successful(Right(())))

      whenReady(testService.clear(internalId)) {
        _ mustBe Right(())
      }
    }

    "must return an error if the operation was unsuccessful" in new Setup {
      when(mockUserAnswersConnector.clear(any())(any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("There was a problem", INTERNAL_SERVER_ERROR))))

      whenReady(testService.clear(internalId)) {
        _ mustBe Left(UpstreamErrorResponse("There was a problem", INTERNAL_SERVER_ERROR))
      }
    }
  }

  "keepAlive" - {
    "must return a success if the operation was successful" in new Setup {
      when(mockUserAnswersConnector.keepAlive(any())(any()))
        .thenReturn(Future.successful(Right(())))

      whenReady(testService.keepAlive(internalId)) {
        _ mustBe Right(())
      }
    }

    "must return an error if the operation was unsuccessful" in new Setup {
      when(mockUserAnswersConnector.keepAlive(any())(any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("There was a problem", INTERNAL_SERVER_ERROR))))

      whenReady(testService.keepAlive(internalId)) {
        _ mustBe Left(UpstreamErrorResponse("There was a problem", INTERNAL_SERVER_ERROR))
      }
    }
  }

  class Setup {
    val mockUserAnswersConnector: ReturnsUserAnswersConnector = mock[ReturnsUserAnswersConnector]
    val testService                                           = new ReturnsUserAnswersService(mockUserAnswersConnector)
  }
}
