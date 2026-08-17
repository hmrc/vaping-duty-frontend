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
import play.api.Application
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.http.InternalServerException
import util.WireMockHelper

class DirectDebitConnectorISpec extends ISpecBase with WireMockHelper with TestData {

  private lazy val application: Application = applicationBuilder()
    .configure(
      "microservice.services.vaping-duty-finance.port" -> server.port
    ).build()

  private val url            = "/vaping-duty-finance/direct-debit/vpd-confirmation/start"
  private lazy val connector = application.injector.instanceOf[DirectDebitConnector]

  "startDirectDebit must" - {

    "successfully return a StartDirectDebitResponse when valid response is received" in {
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.toJson(testStartDirectDebitResponse).toString)
          )
      )

      val result = connector.startDirectDebit(testStartDirectDebitRequest).futureValue

      result mustBe testStartDirectDebitResponse
    }

    "return an error when invalid JSON is returned" in {
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(aResponse().withStatus(OK).withBody("invalid json"))
      )

      val result = connector.startDirectDebit(testStartDirectDebitRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage must include("Parsing failed for start direct debit response")
      }
    }

    "return an error when http client returns BAD_REQUEST" in {
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(aResponse().withStatus(BAD_REQUEST))
      )

      val result = connector.startDirectDebit(testStartDirectDebitRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage must include("Failed to start direct debit journey")
      }
    }

    "return an error when http client returns INTERNAL_SERVER_ERROR" in {
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      val result = connector.startDirectDebit(testStartDirectDebitRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage must include("Failed to start direct debit journey")
      }
    }

    "return an error when http client returns SERVICE_UNAVAILABLE" in {
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE))
      )

      val result = connector.startDirectDebit(testStartDirectDebitRequest)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage must include("Failed to start direct debit journey")
      }
    }
  }
}
