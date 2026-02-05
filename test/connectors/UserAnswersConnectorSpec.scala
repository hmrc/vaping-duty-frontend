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

package connectors

import base.SpecBase
import config.FrontendAppConfig
import data.TestData
import models.ContactPreferenceUserAnswers
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future

class UserAnswersConnectorSpec extends SpecBase with TestData {
  "GET" - {
    "must successfully fetch user answers" in new SetUp {
      val mockUrl = s"http://vaping-duty-account/user-answers/$vpdId"
      when(mockConfig.cpUserAnswersGetUrl(any())).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, ContactPreferenceUserAnswers]](any(), any()))
        .thenReturn(Future.successful(Right(userAnswers)))

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.get(vpdId)) {
        _ mustBe Right(userAnswers)
      }
    }
  }

  "POST" - {
    "must successfully write user answers" in new SetUp {
      val postUrl = "http://vaping-duty-account/user-answers"

      when(mockConfig.cpUserAnswersUrl).thenReturn(postUrl)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      when(requestBuilder.withBody(eqTo(Json.toJson(userDetails)))(any(), any(), any()))
        .thenReturn(requestBuilder)

      when(requestBuilder.setHeader("Csrf-Token" -> "nocheck"))
        .thenReturn(requestBuilder)

      when(requestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mockHttpResponse))

      connector.createUserAnswers(userDetails)
      verify(connector.httpClient, atLeastOnce).post(eqTo(url"$postUrl"))(any())
    }
  }

  "PUT" - {
    "must successfully write user answers" in new SetUp {
      val putUrl = "http://vaping-duty-account/user-answers"

      when(mockConfig.cpUserAnswersUrl).thenReturn(putUrl)

      when(connector.httpClient.put(any())(any())).thenReturn(requestBuilder)

      when(requestBuilder.withBody(eqTo(Json.toJson(userAnswers)))(any(), any(), any()))
        .thenReturn(requestBuilder)

      when(requestBuilder.setHeader("Csrf-Token" -> "nocheck"))
        .thenReturn(requestBuilder)

      when(requestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mockHttpResponse))

      connector.set(userAnswers)

      verify(connector.httpClient, atLeastOnce).put(eqTo(url"$putUrl"))(any())
    }
  }

  "keepAlive" - {
    "must successfully keepAlive" in new SetUp {
      val postUrl = "http://vaping-duty-account/user-answers/keepAlive"

      when(mockConfig.cpUserAnswersKeepAliveUrl).thenReturn(postUrl)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      when(requestBuilder.setHeader("Csrf-Token" -> "nocheck"))
        .thenReturn(requestBuilder)

      when(requestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      connector.keepAlive(userAnswers.vpdId)
      verify(connector.httpClient, atLeastOnce).post(eqTo(url"$postUrl"))(any())
    }

    "be unsuccessful when response is not NO_CONTENT" in new SetUp {
      val postUrl = "http://vaping-duty-account/user-answers/keepAlive"

      when(mockConfig.cpUserAnswersKeepAliveUrl).thenReturn(postUrl)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      when(requestBuilder.setHeader("Csrf-Token" -> "nocheck"))
        .thenReturn(requestBuilder)

      when(requestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mockHttpResponse))

      connector.keepAlive(userAnswers.vpdId)
      verify(connector.httpClient, atLeastOnce).post(eqTo(url"$postUrl"))(any())
    }
  }

  "clear" - {
    "must successfully clear user answers" in new SetUp {
      val deleteUrl = "http://vaping-duty-account/user-answers/clear"

      when(mockConfig.cpUserAnswersClearUrl(vpdId)).thenReturn(deleteUrl)

      when(connector.httpClient.delete(any())(any())).thenReturn(requestBuilder)

      when(requestBuilder.setHeader("Csrf-Token" -> "nocheck"))
        .thenReturn(requestBuilder)

      when(requestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(NO_CONTENT)))

      connector.clear(userAnswers.vpdId)

      verify(connector.httpClient, atLeastOnce).delete(eqTo(url"$deleteUrl"))(any())
    }

    "must fail when response is not NO_CONTENT" in new SetUp {
      val deleteUrl = "http://vaping-duty-account/user-answers/clear"

      when(mockConfig.cpUserAnswersClearUrl(vpdId)).thenReturn(deleteUrl)

      when(connector.httpClient.delete(any())(any())).thenReturn(requestBuilder)

      when(requestBuilder.setHeader("Csrf-Token" -> "nocheck"))
        .thenReturn(requestBuilder)

      when(requestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mockHttpResponse))

      connector.clear(userAnswers.vpdId)

      verify(connector.httpClient, atLeastOnce).delete(eqTo(url"$deleteUrl"))(any())
    }
  }

  class SetUp {
    val mockConfig: FrontendAppConfig  = mock[FrontendAppConfig]
    val httpClient: HttpClientV2       = mock[HttpClientV2]
    val connector                      = new UserAnswersConnector(config = mockConfig, httpClient = httpClient)
    val mockHttpResponse: HttpResponse = mock[HttpResponse]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
  }
}
