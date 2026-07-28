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

package controllers.testonly

import base.SpecBase
import controllers.actions.{ApprovedVapingManufacturerAuthAction, FakeApprovedVapingManufacturerAuthAction}
import connectors.testonly.TestFinancialDataConnector
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
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class TestFinancialDataControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[TestFinancialDataConnector]
  private val vpdId = VpdId("GBWK0000001WK")
  private val vpdIdString = "GBWK0000001WK"
  private val scenario = "mixed"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  private def application = GuiceApplicationBuilder()
    .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
    .overrides(
      bind[ApprovedVapingManufacturerAuthAction].to[FakeApprovedVapingManufacturerAuthAction],
      bind[TestFinancialDataConnector].toInstance(mockConnector)
    )
    .build()

  "TestFinancialDataController" - {
    "setScenario" - {
      "must return 200 OK when the connector returns success" in {
        val responseJson = Json.obj(
          "message" -> s"Successfully set scenario '$scenario' for VPD ID $vpdId",
          "vpdId" -> vpdId.value,
          "scenario" -> scenario,
          "documentCount" -> 3,
          "noDataIdentified" -> false
        )

        when(mockConnector.setScenario(eqTo(vpdId), eqTo(scenario))(any()))
          .thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val request = FakeRequest(GET, controllers.testonly.routes.TestFinancialDataController.setScenario(vpdIdString, scenario).url)
        val result = route(application, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe responseJson
        verify(mockConnector).setScenario(eqTo(vpdId), eqTo(scenario))(any())
      }

      "must return error status when the connector returns an error" in {
        when(mockConnector.setScenario(eqTo(vpdId), eqTo(scenario))(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

        val request = FakeRequest(GET, controllers.testonly.routes.TestFinancialDataController.setScenario(vpdIdString, scenario).url)
        val result = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        verify(mockConnector).setScenario(eqTo(vpdId), eqTo(scenario))(any())
      }
    }

    "clearVpdIdFinancialData" - {
      "must return 200 OK when the connector returns success" in {
        val responseJson = Json.obj(
          "message" -> s"Successfully cleared financial data for VPD ID $vpdId",
          "vpdId" -> vpdId.value
        )

        when(mockConnector.clearVpdIdFinancialData(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val request = FakeRequest(GET, controllers.testonly.routes.TestFinancialDataController.clearVpdIdFinancialData(vpdIdString).url)
        val result = route(application, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe responseJson
        verify(mockConnector).clearVpdIdFinancialData(eqTo(vpdId))(any())
      }

      "must return error status when the connector returns an error" in {
        when(mockConnector.clearVpdIdFinancialData(eqTo(vpdId))(any()))
          .thenReturn(Future.successful(HttpResponse(NOT_FOUND, "")))

        val request = FakeRequest(GET, controllers.testonly.routes.TestFinancialDataController.clearVpdIdFinancialData(vpdIdString).url)
        val result = route(application, request).value

        status(result) mustBe NOT_FOUND
        verify(mockConnector).clearVpdIdFinancialData(eqTo(vpdId))(any())
      }
    }

    "clearAllFinancialData" - {
      "must return 200 OK when the connector returns success" in {
        val responseJson = Json.obj(
          "message" -> "Successfully cleared all financial data"
        )

        when(mockConnector.clearAllFinancialData()(any()))
          .thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val request = FakeRequest(GET, controllers.testonly.routes.TestFinancialDataController.clearAllFinancialData().url)
        val result = route(application, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe responseJson
        verify(mockConnector).clearAllFinancialData()(any())
      }

      "must return error status when the connector returns an error" in {
        when(mockConnector.clearAllFinancialData()(any()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

        val request = FakeRequest(GET, controllers.testonly.routes.TestFinancialDataController.clearAllFinancialData().url)
        val result = route(application, request).value

        status(result) mustBe INTERNAL_SERVER_ERROR
        verify(mockConnector).clearAllFinancialData()(any())
      }
    }

    "setCustomFinancialData" - {
      "must return 200 OK when the connector returns success" in {
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

        when(mockConnector.setCustomFinancialData(eqTo(vpdId), eqTo(customFinancialData))(any()))
          .thenReturn(Future.successful(HttpResponse(OK, responseJson.toString)))

        val request = FakeRequest(POST, controllers.testonly.routes.TestFinancialDataController.setCustomFinancialData(vpdIdString).url)
          .withJsonBody(customFinancialData)
        val result = route(application, request).value

        status(result) mustBe OK
        contentAsJson(result) mustBe responseJson
        verify(mockConnector).setCustomFinancialData(eqTo(vpdId), eqTo(customFinancialData))(any())
      }

      "must return error status when the connector returns an error" in {
        val customFinancialData = Json.obj("vpdId" -> vpdId.value)

        when(mockConnector.setCustomFinancialData(eqTo(vpdId), eqTo(customFinancialData))(any()))
          .thenReturn(Future.successful(HttpResponse(BAD_REQUEST, "")))

        val request = FakeRequest(POST, controllers.testonly.routes.TestFinancialDataController.setCustomFinancialData(vpdIdString).url)
          .withJsonBody(customFinancialData)
        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        verify(mockConnector).setCustomFinancialData(eqTo(vpdId), eqTo(customFinancialData))(any())
      }
    }
  }
}
