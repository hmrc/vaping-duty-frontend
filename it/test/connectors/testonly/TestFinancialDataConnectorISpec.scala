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

package connectors.testonly

import base.ISpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import models.identifiers.VpdId
import play.api.http.Status.*
import play.api.libs.json.Json
import util.WireMockHelper

class TestFinancialDataConnectorISpec extends ISpecBase with WireMockHelper {

  override def fakeApplication(): play.api.Application =
    applicationBuilder()
      .configure(
        "microservice.services.vaping-duty-stubs.host" -> "localhost",
        "microservice.services.vaping-duty-stubs.port" -> server.port()
      )
      .build()

  private val vpdId = VpdId("GBWK0000001WK")
  private val scenario = "mixed"

  "TestFinancialDataConnector" - {
    "setScenario" - {
      "must return 200 OK when the stub accepts the scenario" in {
        val url = s"/test-only/financial-data/$vpdId/scenario/$scenario"
        val responseJson = Json.obj(
          "message" -> s"Successfully set scenario '$scenario' for VPD ID $vpdId",
          "vpdId" -> vpdId.value,
          "scenario" -> scenario,
          "documentCount" -> 3,
          "noDataIdentified" -> false
        )

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(responseJson.toString)
            )
        )

        val connector = app.injector.instanceOf[TestFinancialDataConnector]

        whenReady(connector.setScenario(vpdId, scenario)) { response =>
          response.status mustBe OK
          response.json mustBe responseJson
          server.verify(postRequestedFor(urlEqualTo(url)))
        }
      }

      "must return error status when the stub returns an error" in {
        val url = s"/test-only/financial-data/$vpdId/scenario/$scenario"

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        val connector = app.injector.instanceOf[TestFinancialDataConnector]

        whenReady(connector.setScenario(vpdId, scenario)) { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
          server.verify(postRequestedFor(urlEqualTo(url)))
        }
      }
    }

    "clearVpdIdFinancialData" - {
      "must return 200 OK when the stub clears financial data" in {
        val url = s"/test-only/financial-data/$vpdId/clear"
        val responseJson = Json.obj(
          "message" -> s"Successfully cleared financial data for VPD ID $vpdId",
          "vpdId" -> vpdId.value
        )

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(responseJson.toString)
            )
        )

        val connector = app.injector.instanceOf[TestFinancialDataConnector]

        whenReady(connector.clearVpdIdFinancialData(vpdId)) { response =>
          response.status mustBe OK
          response.json mustBe responseJson
          server.verify(postRequestedFor(urlEqualTo(url)))
        }
      }

      "must return error status when the stub returns an error" in {
        val url = s"/test-only/financial-data/$vpdId/clear"

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(NOT_FOUND)
            )
        )

        val connector = app.injector.instanceOf[TestFinancialDataConnector]

        whenReady(connector.clearVpdIdFinancialData(vpdId)) { response =>
          response.status mustBe NOT_FOUND
          server.verify(postRequestedFor(urlEqualTo(url)))
        }
      }
    }

    "clearAllFinancialData" - {
      "must return 200 OK when the stub clears all financial data" in {
        val url = "/test-only/financial-data/clear-all"
        val responseJson = Json.obj(
          "message" -> "Successfully cleared all financial data"
        )

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(responseJson.toString)
            )
        )

        val connector = app.injector.instanceOf[TestFinancialDataConnector]

        whenReady(connector.clearAllFinancialData()) { response =>
          response.status mustBe OK
          response.json mustBe responseJson
          server.verify(postRequestedFor(urlEqualTo(url)))
        }
      }

      "must return error status when the stub returns an error" in {
        val url = "/test-only/financial-data/clear-all"

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        val connector = app.injector.instanceOf[TestFinancialDataConnector]

        whenReady(connector.clearAllFinancialData()) { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
          server.verify(postRequestedFor(urlEqualTo(url)))
        }
      }
    }

    "setCustomFinancialData" - {
      "must return 200 OK when the stub accepts custom financial data" in {
        val url = s"/test-only/financial-data/$vpdId/custom"
        val customFinancialData = Json.obj(
          "vpdId" -> vpdId.value,
          "noDataIdentified" -> false,
          "documentDetails" -> Json.arr(),
          "lastUpdated" -> "2026-10-01T10:15:10Z"
        )
        val responseJson = Json.obj(
          "message" -> s"Successfully set custom financial data for VPD ID $vpdId",
          "vpdId" -> vpdId.value,
          "documentCount" -> 0
        )

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(responseJson.toString)
            )
        )

        val connector = app.injector.instanceOf[TestFinancialDataConnector]

        whenReady(connector.setCustomFinancialData(vpdId, customFinancialData)) { response =>
          response.status mustBe OK
          response.json mustBe responseJson
          server.verify(postRequestedFor(urlEqualTo(url)))
        }
      }

      "must return error status when the stub returns an error" in {
        val url = s"/test-only/financial-data/$vpdId/custom"
        val customFinancialData = Json.obj("vpdId" -> vpdId.value)

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(BAD_REQUEST)
            )
        )

        val connector = app.injector.instanceOf[TestFinancialDataConnector]

        whenReady(connector.setCustomFinancialData(vpdId, customFinancialData)) { response =>
          response.status mustBe BAD_REQUEST
          server.verify(postRequestedFor(urlEqualTo(url)))
        }
      }
    }
  }
}
