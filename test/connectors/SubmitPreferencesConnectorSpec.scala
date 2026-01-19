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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{BAD_GATEWAY, CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future

class SubmitPreferencesConnectorSpec extends SpecBase with TestData {

  "submitContactPreferences must" - {
    val mockUrl = s"http://alcohol-duty-contact-preferences/submit-preferences/$vppaId"

    "successfully submit contact preferences" in new SetUp {
      val jsonResponse = Json.toJson(testSubmissionResponse).toString()
      val httpResponse = HttpResponse(OK, jsonResponse)

      when(mockConfig.ecpSubmitContactPreferencesUrl(eqTo(vppaId))).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(httpResponse)))

      when(
        requestBuilder.withBody(
          eqTo(Json.toJson(contactPreferenceSubmissionEmail))
        )(any(), any(), any())
      )
        .thenReturn(requestBuilder)

      when(connector.httpClient.put(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.submitContactPreferences(contactPreferenceSubmissionEmail, vppaId)) { result =>
        result mustBe Right(testSubmissionResponse)

        verify(connector.httpClient, times(1)).put(eqTo(url"$mockUrl"))(any())
        verify(requestBuilder, times(1)).execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }

    "fail when invalid JSON is returned" in new SetUp {
      val invalidJsonResponse = HttpResponse(OK, """{ "invalid": "json" }""")

      when(mockConfig.ecpSubmitContactPreferencesUrl(eqTo(vppaId))).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(invalidJsonResponse)))

      when(
        requestBuilder.withBody(
          eqTo(Json.toJson(contactPreferenceSubmissionEmail))
        )(any(), any(), any())
      )
        .thenReturn(requestBuilder)

      when(connector.httpClient.put(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.submitContactPreferences(contactPreferenceSubmissionEmail, vppaId)) { result =>
        result.swap.toOption.get.status mustBe INTERNAL_SERVER_ERROR
        result.swap.toOption.get.message  must include("Invalid JSON format")

        verify(connector.httpClient, times(1)).put(eqTo(url"$mockUrl"))(any())
        verify(requestBuilder, times(1)).execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }

    "fail when an unexpected response is returned" in new SetUp {
      val upstreamErrorResponse = Future.successful(
        Left[UpstreamErrorResponse, HttpResponse](UpstreamErrorResponse("", BAD_GATEWAY, BAD_GATEWAY, Map.empty))
      )

      when(mockConfig.ecpSubmitContactPreferencesUrl(eqTo(vppaId))).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(upstreamErrorResponse)

      when(
        requestBuilder.withBody(
          eqTo(Json.toJson(contactPreferenceSubmissionEmail))
        )(any(), any(), any())
      )
        .thenReturn(requestBuilder)

      when(connector.httpClient.put(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.submitContactPreferences(contactPreferenceSubmissionEmail, vppaId)) { result =>
        result.swap.toOption.get.status mustBe BAD_GATEWAY
        result.swap.toOption.get.message  must include("Unexpected response")

        verify(connector.httpClient, times(1)).put(eqTo(url"$mockUrl"))(any())
        verify(requestBuilder, times(1)).execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }

    "fail when an unexpected status code is returned" in new SetUp {
      val invalidStatusCodeResponse = HttpResponse(CREATED, "")

      when(mockConfig.ecpSubmitContactPreferencesUrl(eqTo(vppaId))).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(invalidStatusCodeResponse)))

      when(
        requestBuilder.withBody(
          eqTo(Json.toJson(contactPreferenceSubmissionEmail))
        )(any(), any(), any())
      )
        .thenReturn(requestBuilder)

      when(connector.httpClient.put(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.submitContactPreferences(contactPreferenceSubmissionEmail, vppaId)) { result =>
        result.swap.toOption.get.status mustBe INTERNAL_SERVER_ERROR
        result.swap.toOption.get.message  must include("Unexpected status code")

        verify(connector.httpClient, times(1)).put(eqTo(url"$mockUrl"))(any())
        verify(requestBuilder, times(1)).execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }
  }

  class SetUp {
    val mockConfig: FrontendAppConfig  = mock[FrontendAppConfig]
    val httpClient: HttpClientV2       = mock[HttpClientV2]
    val connector                      = new SubmitPreferencesConnector(config = mockConfig, httpClient = httpClient)
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
  }
}
