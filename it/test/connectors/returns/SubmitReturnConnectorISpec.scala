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
import models.returns.ReturnSubmittedResponse
import play.api.Application
import play.api.http.Status.*
import play.api.libs.json.Json
import util.WireMockHelper

class SubmitReturnConnectorISpec extends ISpecBase with WireMockHelper with TestData {

  private lazy val application: Application = applicationBuilder()
    .configure(
      "microservice.services.vaping-duty.port" -> server.port
    ).build()

  private val url = s"/vaping-duty/vpd-return/$vpdId"
  private lazy val connector = application.injector.instanceOf[SubmitReturnConnector]

  "submitReturn must" - {

    "successfully submit a return" in {
      server.stubFor(
        post(url).willReturn(aResponse().withStatus(OK).withBody(Json.toJson(testReturnSubmissionResponse).toString))
      )

      val result = connector.submitReturn(testSubmitReturnRequest, vpdId).futureValue

      result mustBe testReturnSubmissionResponse
    }

    "fail when invalid JSON is returned" in {
      server.stubFor(
        post(url).willReturn(aResponse().withStatus(CREATED).withBody(Json.toJson("InvalidJSON").toString))
      )

      val result = connector.submitReturn(testSubmitReturnRequest, vpdId)

      whenReady(result.failed) { exception =>
        exception mustBe an[Exception]
      }
    }

    "fail when an unexpected status code is returned" in {
      server.stubFor(
        post(url).willReturn(aResponse().withStatus(BAD_GATEWAY).withBody(Json.toJson(testReturnSubmissionResponse).toString))
      )

      val result = connector.submitReturn(testSubmitReturnRequest, vpdId)

      whenReady(result.failed) { exception =>
        exception mustBe an[Exception]
      }
    }
  }
}
