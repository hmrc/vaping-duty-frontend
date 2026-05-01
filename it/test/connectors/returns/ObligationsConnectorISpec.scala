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

package connectors.returns

import base.ISpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import data.TestData
import play.api.Application
import play.api.http.Status.*
import play.api.libs.json.Json
import util.WireMockHelper

class ObligationsConnectorISpec extends ISpecBase with WireMockHelper with TestData {

  private lazy val application: Application = applicationBuilder()
    .configure(
      "microservice.services.vaping-duty.port" -> server.port
    ).build()

  private val url = s"/vaping-duty/obligations/$vpdId"
  private lazy val connector = application.injector.instanceOf[ObligationsConnector]

  "getObligations must" - {

    "successfully retrieve obligation data" in {
      server.stubFor(
        get(url).willReturn(aResponse().withStatus(OK).withBody(Json.toJson(createMockObligationsResponse()).toString))
      )

      val result = connector.getObligations(vpdId).futureValue

      result mustBe createMockObligationsResponse()
    }

    "fail when invalid JSON is returned" in {
      server.stubFor(
        get(url).willReturn(aResponse().withStatus(CREATED).withBody(Json.toJson("InvalidJSON").toString))
      )

      val result = connector.getObligations(vpdId)

      whenReady(result.failed) { exception =>
        exception mustBe an[Exception]
      }
    }

    "fail when an unexpected status code is returned" in {
      server.stubFor(
        get(url).willReturn(aResponse().withStatus(BAD_GATEWAY).withBody(Json.toJson(createMockObligationsResponse()).toString))
      )

      val result = connector.getObligations(vpdId)

      whenReady(result.failed) { exception =>
        exception mustBe an[Exception]
      }
    }
  }
}