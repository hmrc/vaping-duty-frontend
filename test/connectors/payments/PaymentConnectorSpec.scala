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

package connectors.payments

import base.SpecBase
import config.FrontendAppConfig
import data.TestData
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK, UNPROCESSABLE_ENTITY}
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

class PaymentConnectorSpec extends SpecBase with TestData {

  val testResponseString: String = Json.toJson(testStartPaymentResponse).toString

  "startPayment" - {
    "must successfully return a StartPaymentResponse when valid response is received" in new SetUp {
      val mockUrl = "http://vaping-duty-finance/vaping-duty-finance/start-payment"
      when(mockConfig.startPaymentUrl).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(HttpResponse(status = OK, body = testResponseString))))

      when(requestBuilder.withBody(eqTo(Json.toJson(testStartPaymentRequest)))(any(), any(), any()))
        .thenReturn(requestBuilder)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.startPayment(testStartPaymentRequest)) { result =>
        result mustBe testStartPaymentResponse
      }
    }

    "must return an error when invalid JSON is returned" in new SetUp {
      val mockUrl = "http://vaping-duty-finance/vaping-duty-finance/start-payment"
      when(mockConfig.startPaymentUrl).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(HttpResponse(status = OK, body = "invalid json"))))

      when(requestBuilder.withBody(eqTo(Json.toJson(testStartPaymentRequest)))(any(), any(), any()))
        .thenReturn(requestBuilder)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.startPayment(testStartPaymentRequest).failed) { exception =>
        exception.getMessage must include("Parsing failed for start payment response")
      }
    }

    "must return an error when http client returns BAD_REQUEST" in new SetUp {
      val mockUrl = "http://vaping-duty-finance/vaping-duty-finance/start-payment"
      when(mockConfig.startPaymentUrl).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(
          Future.successful(
            Left(UpstreamErrorResponse(statusCode = BAD_REQUEST, message = "Bad request"))
          )
        )

      when(requestBuilder.withBody(eqTo(Json.toJson(testStartPaymentRequest)))(any(), any(), any()))
        .thenReturn(requestBuilder)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.startPayment(testStartPaymentRequest).failed) { exception =>
        exception.getMessage must include("Failed to start payment")
      }
    }

    "must return an error when http client returns NOT_FOUND" in new SetUp {
      val mockUrl = "http://vaping-duty-finance/vaping-duty-finance/start-payment"
      when(mockConfig.startPaymentUrl).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(
          Future.successful(
            Left(UpstreamErrorResponse(statusCode = NOT_FOUND, message = "Not found"))
          )
        )

      when(requestBuilder.withBody(eqTo(Json.toJson(testStartPaymentRequest)))(any(), any(), any()))
        .thenReturn(requestBuilder)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.startPayment(testStartPaymentRequest).failed) { exception =>
        exception.getMessage must include("Failed to start payment")
      }
    }

    "must return an error when http client returns UNPROCESSABLE_ENTITY" in new SetUp {
      val mockUrl = "http://vaping-duty-finance/vaping-duty-finance/start-payment"
      when(mockConfig.startPaymentUrl).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(
          Future.successful(
            Left(UpstreamErrorResponse(statusCode = UNPROCESSABLE_ENTITY, message = "Unprocessable entity"))
          )
        )

      when(requestBuilder.withBody(eqTo(Json.toJson(testStartPaymentRequest)))(any(), any(), any()))
        .thenReturn(requestBuilder)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.startPayment(testStartPaymentRequest).failed) { exception =>
        exception.getMessage must include("Failed to start payment")
      }
    }

    "must return an error when http client returns INTERNAL_SERVER_ERROR" in new SetUp {
      val mockUrl = "http://vaping-duty-finance/vaping-duty-finance/start-payment"
      when(mockConfig.startPaymentUrl).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(
          Future.successful(
            Left(UpstreamErrorResponse(statusCode = INTERNAL_SERVER_ERROR, message = "Internal server error"))
          )
        )

      when(requestBuilder.withBody(eqTo(Json.toJson(testStartPaymentRequest)))(any(), any(), any()))
        .thenReturn(requestBuilder)

      when(connector.httpClient.post(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.startPayment(testStartPaymentRequest).failed) { exception =>
        exception.getMessage must include("Failed to start payment")
      }
    }
  }

  class SetUp {
    val mockConfig: FrontendAppConfig  = mock[FrontendAppConfig]
    val httpClient: HttpClientV2       = mock[HttpClientV2]
    val connector                      = new PaymentConnector(config = mockConfig, httpClient = httpClient)
    val mockHttpResponse: HttpResponse = mock[HttpResponse]
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
  }
}