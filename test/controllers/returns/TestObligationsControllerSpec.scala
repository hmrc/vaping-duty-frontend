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

package controllers.returns

import controllers.actions.{ApprovedVapingManufacturerAuthAction, FakeApprovedVapingManufacturerAuthAction}
import models.identifiers.VpdId
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import base.SpecBase
import connectors.testonly.TestObligationsConnector
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class TestObligationsControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[TestObligationsConnector]
  private val vpdId = VpdId("GBWK0000001WK")
  private val vpdIdString = "GBWK0000001WK"
  private val scenario = "only-open"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  private def application = GuiceApplicationBuilder()
    .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
    .overrides(
      bind[ApprovedVapingManufacturerAuthAction].to[FakeApprovedVapingManufacturerAuthAction],
      bind[TestObligationsConnector].toInstance(mockConnector)
    )
    .build()

  "TestObligationsController" - {
    "setScenario" - {
      "must return 200 OK when the connector returns success" in {
        val responseJson = Json.obj(
          "message" -> s"Successfully set scenario '$scenario' for VPD ID $vpdId",
          "vpdId" -> vpdId.value,
          "scenario" -> scenario,
          "obligationCount" -> 3
        )

        when(mockConnector.setScenario(eqTo(vpdId), eqTo(scenario))(any()))
          .thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val request = FakeRequest(GET, controllers.testonly.routes.TestObligationsController.setScenario(vpdIdString, scenario).url)
        val result = route(application, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe responseJson
        verify(mockConnector).setScenario(eqTo(vpdId), eqTo(scenario))(any())
      }

      "must return error status when the connector returns an error" in {
        when(mockConnector.setScenario(eqTo(vpdId), eqTo(scenario))(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

        val request = FakeRequest(GET, controllers.testonly.routes.TestObligationsController.setScenario(vpdIdString, scenario).url)
        val result = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        verify(mockConnector).setScenario(eqTo(vpdId), eqTo(scenario))(any())
      }
    }

    "clearVpdIdObligations" - {
      "must return 200 OK when the connector returns success" in {
        val responseJson = Json.obj(
          "message" -> s"Successfully cleared obligations for VPD ID $vpdId",
          "vpdId" -> vpdId.value
        )

        when(mockConnector.clearVpdIdObligations(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val request = FakeRequest(GET, controllers.testonly.routes.TestObligationsController.clearVpdIdObligations(vpdIdString).url)
        val result = route(application, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe responseJson
        verify(mockConnector).clearVpdIdObligations(eqTo(vpdId))(any())
      }

      "must return error status when the connector returns an error" in {
        when(mockConnector.clearVpdIdObligations(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))

        val request = FakeRequest(GET, controllers.testonly.routes.TestObligationsController.clearVpdIdObligations(vpdIdString).url)
        val result = route(application, request).value

        status(result) mustBe NOT_FOUND
        verify(mockConnector).clearVpdIdObligations(eqTo(vpdId))(any())
      }
    }

    "clearAllObligations" - {
      "must return 200 OK when the connector returns success" in {
        val responseJson = Json.obj(
          "message" -> "Successfully cleared all obligations data"
        )

        when(mockConnector.clearAllObligations()(any()))
          .thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val request = FakeRequest(GET, controllers.testonly.routes.TestObligationsController.clearAllObligations().url)
        val result = route(application, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe responseJson
        verify(mockConnector).clearAllObligations()(any())
      }

      "must return error status when the connector returns an error" in {
        when(mockConnector.clearAllObligations()(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

        val request = FakeRequest(GET, controllers.testonly.routes.TestObligationsController.clearAllObligations().url)
        val result = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        verify(mockConnector).clearAllObligations()(any())
      }
    }

    "setCustomObligations" - {
      "must return 200 OK when the connector returns success" in {
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

        when(mockConnector.setCustomObligations(eqTo(vpdId), eqTo(customObligations))(any()))
          .thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val request = FakeRequest(POST, controllers.testonly.routes.TestObligationsController.setCustomObligations(vpdIdString).url)
          .withJsonBody(customObligations)
        val result = route(application, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe responseJson
        verify(mockConnector).setCustomObligations(eqTo(vpdId), eqTo(customObligations))(any())
      }

      "must return error status when the connector returns an error" in {
        val customObligations = Json.obj("vpdId" -> vpdId.value)

        when(mockConnector.setCustomObligations(eqTo(vpdId), eqTo(customObligations))(any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

        val request = FakeRequest(POST, controllers.testonly.routes.TestObligationsController.setCustomObligations(vpdIdString).url)
          .withJsonBody(customObligations)
        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        verify(mockConnector).setCustomObligations(eqTo(vpdId), eqTo(customObligations))(any())
      }
    }
  }
}