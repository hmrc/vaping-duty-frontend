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

package connectors.contactPreference

import base.ISpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import data.TestData
import models.contactPreference.{PreferenceUserAnswers, UserDetails}
import play.api.Application
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.UpstreamErrorResponse
import util.WireMockHelper

class PreferenceUserAnswersConnectorISpec extends ISpecBase with TestData with WireMockHelper {

  private lazy val application: Application = applicationBuilder()
    .configure(
      "microservice.services.vaping-duty-account.port" -> server.port
    ).build()

  private val url            = "/vaping-duty-account/user-answers"
  private lazy val connector = application.injector.instanceOf[PreferenceUserAnswersConnector]

  private val answers                     = emptyUserAnswers
  private val internalServerErrorResponse = UpstreamErrorResponse("There was a problem", INTERNAL_SERVER_ERROR)

  ".get" - {
    "must successfully fetch user answers" in {
      server.stubFor(
        get(urlEqualTo(s"$url/$vpdId"))
          .willReturn(aResponse().withStatus(OK).withBody(Json.toJson(answers).toString))
      )
      val result = connector.get(vpdId).futureValue

      result mustBe Right(answers)
    }

    "must return UpstreamErrorResponse when there is an issue" in {
      server.stubFor(
        get(urlEqualTo(s"$url/$vpdId"))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody(internalServerErrorResponse.toString))
      )
      val result = connector.get(vpdId).futureValue

      result.isLeft mustBe true
    }
  }

  ".createUserAnswers" - {
    "must successfully create user-answers" in {
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(aResponse().withBody(Json.toJson(answers).toString))
      )
      val result = connector.createUserAnswers(UserDetails(vpdId.toString, internalId.toString)).futureValue

      result mustBe Right(answers)
    }

    "must return UpstreamErrorResponse when there is an issue" in {
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody(internalServerErrorResponse.toString))
      )
      val result = connector.createUserAnswers(UserDetails(vpdId.toString, internalId.toString)).futureValue

      result.isLeft mustBe true
    }
  }

  ".set" - {
    "must successfully write user answers" in {
      server.stubFor(
        put(urlEqualTo(url))
          .willReturn(aResponse().withStatus(CREATED))
      )
      val result = connector.set(answers).futureValue

      result.status mustBe CREATED
    }

    "must return an error status when there is an issue" in {
      server.stubFor(
        put(urlEqualTo(url))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )
      val result = connector.set(answers).futureValue

      result.status mustBe INTERNAL_SERVER_ERROR
    }
  }

  ".keepAlive" - {
    val keepAliveUrl = "/vaping-duty-account/keep-alive"

    "must successfully keepAlive" in {
      server.stubFor(
        post(urlEqualTo(keepAliveUrl))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )
      val result = connector.keepAlive(internalId).futureValue

      result.isRight mustBe true
    }

    "be unsuccessful when response is not NO_CONTENT" in {
      server.stubFor(
        post(urlEqualTo(keepAliveUrl))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )
      val result = connector.keepAlive(internalId).futureValue

      result.isLeft mustBe true
    }
  }

  ".clear" - {
    val deleteUrl = s"$url/clear/$internalId"

    "must successfully clear user answers" in {
      server.stubFor(
        delete(urlEqualTo(deleteUrl))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )
      val result = connector.clear(internalId).futureValue

      result.isRight mustBe true

    }

    "must fail when response is not NO_CONTENT" in {
      server.stubFor(
        delete(urlEqualTo(deleteUrl))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )
      val result = connector.clear(internalId).futureValue

      result.isLeft mustBe true
    }
  }
}
