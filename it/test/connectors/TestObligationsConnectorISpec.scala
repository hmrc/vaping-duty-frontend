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

import connectors.returns.TestObligationsConnector
import models.identifiers.VpdId
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.http.Status.*
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import test.base.ConnectorISpec
import uk.gov.hmrc.http.HeaderCarrier

class TestObligationsConnectorISpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with ConnectorISpec {

  private val vpdId = VpdId("GBWK0000001WK")
  private val scenario = "only-open"

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(getWireMockAppConfig(Seq("vaping-duty-stubs")))
      .build()

  "TestObligationsConnector" - {
    "setScenario" - {
      "must return 200 OK when the stub accepts the scenario" in {
        val url = s"/test-support/obligations/$vpdId/scenario/$scenario"
        val responseJson = Json.obj(
          "message" -> s"Successfully set scenario '$scenario' for VPD ID $vpdId",
          "vpdId" -> vpdId.value,
          "scenario" -> scenario,
          "obligationCount" -> 3
        )

        stubPost(url, OK, responseJson.toString)

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.setScenario(vpdId, scenario)) { response =>
          response.status mustBe OK
          response.json mustBe responseJson
          verifyPost(url)
        }
      }

      "must return error status when the stub returns an error" in {
        val url = s"/test-support/obligations/$vpdId/scenario/$scenario"

        stubPost(url, INTERNAL_SERVER_ERROR, "")

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.setScenario(vpdId, scenario)) { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
          verifyPost(url)
        }
      }
    }

    "clearVpdIdObligations" - {
      "must return 200 OK when the stub clears obligations" in {
        val url = s"/test-support/obligations/$vpdId"
        val responseJson = Json.obj(
          "message" -> s"Successfully cleared obligations for VPD ID $vpdId",
          "vpdId" -> vpdId.value
        )

        stubDelete(url, OK, responseJson.toString)

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.clearVpdIdObligations(vpdId)) { response =>
          response.status mustBe OK
          response.json mustBe responseJson
          verifyDelete(url)
        }
      }

      "must return error status when the stub returns an error" in {
        val url = s"/test-support/obligations/$vpdId"

        stubDelete(url, NOT_FOUND, "")

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.clearVpdIdObligations(vpdId)) { response =>
          response.status mustBe NOT_FOUND
          verifyDelete(url)
        }
      }
    }

    "clearAllObligations" - {
      "must return 200 OK when the stub clears all obligations" in {
        val url = "/test-support/obligations"
        val responseJson = Json.obj(
          "message" -> "Successfully cleared all obligations data"
        )

        stubDelete(url, OK, responseJson.toString)

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.clearAllObligations()) { response =>
          response.status mustBe OK
          response.json mustBe responseJson
          verifyDelete(url)
        }
      }

      "must return error status when the stub returns an error" in {
        val url = "/test-support/obligations"

        stubDelete(url, INTERNAL_SERVER_ERROR, "")

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.clearAllObligations()) { response =>
          response.status mustBe INTERNAL_SERVER_ERROR
          verifyDelete(url)
        }
      }
    }

    "setCustomObligations" - {
      "must return 200 OK when the stub accepts custom obligations" in {
        val url = s"/test-support/obligations/$vpdId/custom"
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

        stubPost(url, OK, responseJson.toString)

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.setCustomObligations(vpdId, customObligations)) { response =>
          response.status mustBe OK
          response.json mustBe responseJson
          verifyPost(url)
        }
      }

      "must return error status when the stub returns an error" in {
        val url = s"/test-support/obligations/$vpdId/custom"
        val customObligations = Json.obj("vpdId" -> vpdId.value)

        stubPost(url, BAD_REQUEST, "")

        val connector = app.injector.instanceOf[TestObligationsConnector]

        whenReady(connector.setCustomObligations(vpdId, customObligations)) { response =>
          response.status mustBe BAD_REQUEST
          verifyPost(url)
        }
      }
    }
  }
}