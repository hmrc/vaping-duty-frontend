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
import models.emailverification.ErrorModel
import models.returns.{ReturnCreateRequest, ReturnSubmittedResponse, TotalDutyDue, VapingProductsProduced}
import pages.returns.EnterDutyAmountPage
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

  val totalInMl = returnsUserAnswers.get(EnterDutyAmountPage).fold(BigDecimal(0))(value => BigDecimal(value))

  // Temp value
  val zeroValue = BigDecimal(0)

  // Will need to either get or pass the period key here
  val periodKey = "26AF"

  // Will need to enhance this much more
  val totalDue = totalInMl - zeroValue

  val request = ReturnCreateRequest(
    periodKey,
    VapingProductsProduced(Seq.empty, Seq.empty),
    TotalDutyDue(totalInMl, zeroValue, zeroValue, zeroValue, zeroValue, totalDue)
  )

  "submitReturn must" - {

    "successfully submit a return" in {
      server.stubFor(
        post(url).willReturn(aResponse().withStatus(OK).withBody(Json.toJson(testReturnSubmissionResponse).toString))
      )

      val result = connector.submitReturn(request, vpdId).futureValue

      result mustBe Right(testReturnSubmissionResponse)
    }

    "fail when invalid JSON is returned" in {
      server.stubFor(
        post(url).willReturn(aResponse().withStatus(OK).withBody(Json.toJson("InvalidJSON").toString))
      )

      val result = connector.submitReturn(request, vpdId).futureValue

      result mustBe Left(
        ErrorModel(INTERNAL_SERVER_ERROR, "Invalid JSON format. Could not parse response as ...")
      )
    }

    "fail when an unexpected response is returned" in {
      server.stubFor(
        post(url).willReturn(aResponse().withStatus(BAD_GATEWAY))
      )

      val result = connector.submitReturn(request, vpdId).futureValue

      result mustBe Left(ErrorModel(BAD_GATEWAY, "Unexpected response. Status: 502"))
    }

    "fail when an unexpected status code is returned" in {
      server.stubFor(
        post(url).willReturn(aResponse().withStatus(CREATED).withBody(Json.toJson(testReturnSubmissionResponse).toString))
      )

      val result = connector.submitReturn(request, vpdId).futureValue

      result mustBe Left(ErrorModel(INTERNAL_SERVER_ERROR, "Unexpected status code when submitting return: 201"))
    }
  }
}
