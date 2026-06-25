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

import base.ISpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import connectors.testonly.TestObligationsConnector
import models.identifiers.VpdId
import play.api.http.Status.*
import play.api.libs.json.Json
import util.WireMockHelper

class TestObligationsConnectorISpec extends ISpecBase with WireMockHelper {

  override def fakeApplication(): play.api.Application =
    applicationBuilder()
      .configure(
        "microservice.services.vaping-duty-stubs.host" -> "localhost",
        "microservice.services.vaping-duty-stubs.port" -> server.port()
      )
      .build()

  private val vpdId = VpdId("GBWK0000001WK")
  private val scenario = "only-open"

  "TestObligationsConnector" - {
    "setScenario" - {
      "must return 200 OK when the stub accepts the scenario" in {
        val url = s"/test-only/obligations/$vpdId/scenario/$scenario"
        val responseJson = Json.obj(
          "message" -> s"Successfully set scenario '$scenario' for VPD ID $vpdId",
          "vpdId" -> vpdId.value,
          "scenario" -> scenario,
          "obligationCount" -> 3
        )

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(responseJson.toString)
            )
        )

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.setScenario(vpdId, scenario)) { response =>
          response.status mustBe OK
          response.json mustBe responseJson
          server.verify(postRequestedFor(urlEqualTo(url)))
        }
      }

      "must return error status when the stub returns an error" in {
        val url = s"/test-only/obligations/$vpdId/scenario/$scenario"

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.setScenario(vpdId, scenario)) { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
          server.verify(postRequestedFor(urlEqualTo(url)))
        }
      }
    }

    "clearVpdIdObligations" - {
      "must return 200 OK when the stub clears obligations" in {
        val url = s"/test-only/obligations/$vpdId"
        val responseJson = Json.obj(
          "message" -> s"Successfully cleared obligations for VPD ID $vpdId",
          "vpdId" -> vpdId.value
        )

        server.stubFor(
          delete(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(responseJson.toString)
            )
        )

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.clearVpdIdObligations(vpdId)) { response =>
          response.status mustBe OK
          response.json mustBe responseJson
          server.verify(deleteRequestedFor(urlEqualTo(url)))
        }
      }

      "must return error status when the stub returns an error" in {
        val url = s"/test-only/obligations/$vpdId"

        server.stubFor(
          delete(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(NOT_FOUND)
            )
        )

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.clearVpdIdObligations(vpdId)) { response =>
          response.status mustBe NOT_FOUND
          server.verify(deleteRequestedFor(urlEqualTo(url)))
        }
      }
    }

    "clearAllObligations" - {
      "must return 200 OK when the stub clears all obligations" in {
        val url = "/test-only/obligations"
        val responseJson = Json.obj(
          "message" -> "Successfully cleared all obligations data"
        )

        server.stubFor(
          delete(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(responseJson.toString)
            )
        )

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.clearAllObligations()) { response =>
          response.status mustBe OK
          response.json mustBe responseJson
          server.verify(deleteRequestedFor(urlEqualTo(url)))
        }
      }

      "must return error status when the stub returns an error" in {
        val url = "/test-only/obligations"

        server.stubFor(
          delete(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
            )
        )

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.clearAllObligations()) { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
          server.verify(deleteRequestedFor(urlEqualTo(url)))
        }
      }
    }

    "setCustomObligations" - {
      "must return 200 OK when the stub accepts custom obligations" in {
        val url = s"/test-only/obligations/$vpdId/custom"
        val customObligations = Json.obj(
          "vpdId" -> vpdId.value,
          "obligations" -> Json.arr(
            Json.obj(
              "identification" -> Json.obj(),
              "obligationDetails" -> Json.obj(
                "openOrFulfilledStatus" -> "O",
                "iCFromDate" -> "2027-12-01",
                "iCToDate" -> "2027-12-31",
                "iCDateReceived" -> Json.obj(),
                "iCDueDate" -> "2028-01-31",
                "periodKey" -> "27AL"
              )
            )
          )
        )
        val responseJson = Json.obj(
          "message" -> s"Successfully set custom obligations for VPD ID $vpdId",
          "vpdId" -> vpdId.value,
          "obligationCount" -> 1
        )

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(responseJson.toString)
            )
        )

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.setCustomObligations(vpdId, customObligations)) { response =>
          response.status mustBe OK
          response.json mustBe responseJson
          server.verify(postRequestedFor(urlEqualTo(url)))
        }
      }

      "must return error status when the stub returns an error" in {
        val url = s"/test-only/obligations/$vpdId/custom"
        val customObligations = Json.obj("vpdId" -> vpdId.value)

        server.stubFor(
          post(urlEqualTo(url))
            .willReturn(
              aResponse()
                .withStatus(BAD_REQUEST)
            )
        )

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.setCustomObligations(vpdId, customObligations)) { response =>
          response.status mustBe BAD_REQUEST
          server.verify(postRequestedFor(urlEqualTo(url)))
        }
      }
    }
  }
}
