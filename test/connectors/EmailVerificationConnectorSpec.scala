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
import models.emailverification.ErrorModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

class EmailVerificationConnectorSpec extends SpecBase with TestData {

  val testGetResponseString: String = Json.toJson(testGetVerificationStatusResponse).toString

  "getEmailVerification" - {
    "must successfully fetch valid verification details for a given user" in new SetUp {
      val mockUrl = s"http://alcohol-duty-contact-preferences/get-email-verification/$credId"
      when(mockConfig.ecpGetEmailVerificationUrl(any())).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(HttpResponse(status = OK, body = testGetResponseString))))

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.getEmailVerification(testVerificationDetails).value) { a =>
        a mustBe Right(testGetVerificationStatusResponse)
      }
    }
    "must return an error when invalid verification details are returned" in new SetUp {
      val mockUrl = s"http://alcohol-duty-contact-preferences/get-email-verification/$credId"
      when(mockConfig.ecpGetEmailVerificationUrl(any())).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(HttpResponse(status = OK, body = "testInvalidResponse"))))

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.getEmailVerification(testVerificationDetails).value) {
        _ mustBe Left(
          ErrorModel(
            INTERNAL_SERVER_ERROR,
            "Invalid JSON format. Could not parse response as GetVerificationStatusResponse"
          )
        )
      }
    }

    "must return an error when http client returns a response with an error code" in new SetUp {
      val mockUrl = s"http://alcohol-duty-contact-preferences/get-email-verification/$credId"
      when(mockConfig.ecpGetEmailVerificationUrl(any())).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(
          Future.successful(
            Left(UpstreamErrorResponse(statusCode = INTERNAL_SERVER_ERROR, message = "test error message"))
          )
        )

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.getEmailVerification(testVerificationDetails).value) {
        _ mustBe Left(
          ErrorModel(
            INTERNAL_SERVER_ERROR,
            "Unexpected response. Status: 500"
          )
        )
      }
    }
  }

  "startEmailVerification" - {
    "must successfully fetch a redirect url when a valid request is made" in new SetUp {
      val mockUrl = s"http://alcohol-duty-contact-preferences/email-verification/verify-email"
      when(mockConfig.startEmailVerificationJourneyUrl).thenReturn(mockUrl)

      when(requestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(status = CREATED, body = testJsonRedirectUriString)))

      when(requestBuilder.withBody(eqTo(Json.toJson(testEmailVerificationRequest)))(any(), any(), any()))
        .thenReturn(requestBuilder)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.startEmailVerification(testEmailVerificationRequest)) {
        _ mustBe Right(testRedirectUri)
      }
    }
    "must return an error if an invalid redirect url is returned when a valid request is made" in new SetUp {
      val mockUrl = s"http://alcohol-duty-contact-preferences/email-verification/verify-email"
      when(mockConfig.startEmailVerificationJourneyUrl).thenReturn(mockUrl)

      when(requestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(status = CREATED, body = "")))

      when(requestBuilder.withBody(eqTo(Json.toJson(testEmailVerificationRequest)))(any(), any(), any()))
        .thenReturn(requestBuilder)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.startEmailVerification(testEmailVerificationRequest)) {
        _ mustBe Left(
          ErrorModel(INTERNAL_SERVER_ERROR, "Invalid JSON format, failed to parse response as a RedirectUrl")
        )
      }
    }

    "must return an error if an error response status is returned" in new SetUp {
      val mockUrl = s"http://alcohol-duty-contact-preferences/email-verification/verify-email"
      when(mockConfig.startEmailVerificationJourneyUrl).thenReturn(mockUrl)

      when(requestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse(status = INTERNAL_SERVER_ERROR, body = "test error")))

      when(requestBuilder.withBody(eqTo(Json.toJson(testEmailVerificationRequest)))(any(), any(), any()))
        .thenReturn(requestBuilder)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.startEmailVerification(testEmailVerificationRequest)) {
        _ mustBe Left(
          ErrorModel(INTERNAL_SERVER_ERROR, "Unexpected response from email verification service. Http status: 500")
        )
      }
    }
  }

  class SetUp {
    val mockConfig: FrontendAppConfig  = mock[FrontendAppConfig]
    val httpClient: HttpClientV2       = mock[HttpClientV2]
    val connector                      = new EmailVerificationConnector(config = mockConfig, httpClient = httpClient)
    val mockHttpResponse: HttpResponse = mock[HttpResponse]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
  }
}
