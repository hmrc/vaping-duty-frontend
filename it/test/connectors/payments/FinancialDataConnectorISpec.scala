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

import base.ISpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import data.TestData
import models.payments.PaymentsResponse
import play.api.Application
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.http.InternalServerException
import util.WireMockHelper

class FinancialDataConnectorISpec extends ISpecBase with WireMockHelper with TestData {

  private lazy val application: Application = applicationBuilder()
    .configure(
      "microservice.services.vaping-duty-finance.port" -> server.port
    ).build()

  private val url            = "/vaping-duty-finance/financial-data/payments"
  private lazy val connector = application.injector.instanceOf[FinancialDataConnector]

  "getPayments must" - {

    "successfully fetch payments" in {
      server.stubFor(
        get(url).willReturn(aResponse().withStatus(OK).withBody(Json.toJson(testPaymentsResponse).toString))
      )

      val result = connector.getPayments(vpdId).futureValue

      result mustBe testPaymentsResponse
    }

    "successfully fetch an entirely empty payments response" in {
      server.stubFor(
        get(url).willReturn(aResponse().withStatus(OK).withBody(Json.toJson(PaymentsResponse.empty).toString))
      )

      val result = connector.getPayments(vpdId).futureValue

      result mustBe PaymentsResponse.empty
    }

    "fail when invalid JSON is returned" in {
      server.stubFor(
        get(url).willReturn(aResponse().withStatus(OK).withBody("invalid json"))
      )

      val result = connector.getPayments(vpdId)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage must include("Parsing failed")
      }
    }

    "fail when http client returns an error response" in {
      server.stubFor(
        get(url).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      val result = connector.getPayments(vpdId)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage must include("Failed to get payments")
      }
    }
  }
}
