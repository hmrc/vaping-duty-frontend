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
import models.payments.OutstandingPayment
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpResponse, InternalServerException, UpstreamErrorResponse}

import scala.concurrent.Future

class FinancialDataConnectorSpec extends SpecBase {

  val testPayment = OutstandingPayment(
    chargeReference = "VPD38270541977",
    period = "December 2026",
    amountDue = BigDecimal("330000.00"),
    dueDate = "2026-12-15",
    status = "Due"
  )

  val testPaymentsJson = Json.toJson(Seq(testPayment)).toString

  "getOutstandingPayments" - {
    "must successfully fetch outstanding payments" in new SetUp {
      val mockUrl = "http://vaping-duty-finance/financial-data/outstanding-payments"
      when(mockConfig.getOutstandingPaymentsUrl(any())).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(HttpResponse(status = OK, body = testPaymentsJson))))

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.getOutstandingPayments(vpdId)) { result =>
        result mustBe Seq(testPayment)
      }
    }

    "must return an error when invalid JSON is returned" in new SetUp {
      val mockUrl = "http://vaping-duty-finance/financial-data/outstanding-payments"
      when(mockConfig.getOutstandingPaymentsUrl(any())).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(HttpResponse(status = OK, body = "invalid json"))))

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.getOutstandingPayments(vpdId).failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage must include("Parsing failed")
      }
    }

    "must return an error when http client returns an error response" in new SetUp {
      val mockUrl = "http://vaping-duty-finance/financial-data/outstanding-payments"
      when(mockConfig.getOutstandingPaymentsUrl(any())).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(
          Future.successful(
            Left(UpstreamErrorResponse(statusCode = INTERNAL_SERVER_ERROR, message = "test error"))
          )
        )

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.getOutstandingPayments(vpdId).failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage must include("Failed to get outstanding payments")
      }
    }

    "must return an error when an exception occurs" in new SetUp {
      val mockUrl = "http://vaping-duty-finance/financial-data/outstanding-payments"
      when(mockConfig.getOutstandingPaymentsUrl(any())).thenReturn(mockUrl)

      when(requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Network error")))

      when(connector.httpClient.get(any())(any())).thenReturn(requestBuilder)

      whenReady(connector.getOutstandingPayments(vpdId).failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage must include("Failed to get outstanding payments")
      }
    }
  }

  class SetUp {
    val mockConfig: FrontendAppConfig  = mock[FrontendAppConfig]
    val httpClient: HttpClientV2       = mock[HttpClientV2]
    val connector                      = new FinancialDataConnector(config = mockConfig, httpClient = httpClient)
    val requestBuilder: RequestBuilder = mock[RequestBuilder]
  }
}