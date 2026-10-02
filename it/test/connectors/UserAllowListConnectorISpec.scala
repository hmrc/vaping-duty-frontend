/*
 * Copyright 2026 HM Revenue & Customs
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
import com.github.tomakehurst.wiremock.http.Fault
import connectors.UserAllowListConnector.UnexpectedResponseException
import models.requests.CheckRequest
import play.api.Application
import play.api.http.Status.*
import play.api.libs.json.Json
import util.WireMockHelper

class UserAllowListConnectorISpec extends ISpecBase with WireMockHelper {

  private val FEATURE = "vpd-private-beta"
  private val INTERNAL_AUTH_TOKEN = "test-token"

  override def fakeApplication(): Application = applicationBuilder()
    .configure(
      "microservice.services.user-allow-list.protocol" -> "http",
      "microservice.services.user-allow-list.host" -> "localhost",
      "microservice.services.user-allow-list.port" -> server.port(),
      "internal-auth.token" -> INTERNAL_AUTH_TOKEN,
      "features.returnsAllowListEnabled" -> true
    )
    .build()

  private lazy val connector = app.injector.instanceOf[UserAllowListConnector]
  private val url = s"/user-allow-list/vaping-duty-frontend/$FEATURE/check"
  private val expectedRequestBody = Json.toJson(CheckRequest(vpdId.value)).toString

  "UserAllowListConnector.check must" - {

    "return true when the service returns 200 OK" in {
      server.stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .withHeader("Authorization", equalTo(INTERNAL_AUTH_TOKEN))
          .willReturn(aResponse().withStatus(OK))
      )

      whenReady(connector.check(FEATURE, vpdId)) { result =>
        result mustBe true
      }

      server.verify(
        postRequestedFor(urlEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .withHeader("Authorization", equalTo(INTERNAL_AUTH_TOKEN))
      )
    }

    "return false when the service returns 404 NOT_FOUND" in {
      server.stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .withHeader("Authorization", equalTo(INTERNAL_AUTH_TOKEN))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      whenReady(connector.check(FEATURE, vpdId)) { result =>
        result mustBe false
      }

      server.verify(
        postRequestedFor(urlEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .withHeader("Authorization", equalTo(INTERNAL_AUTH_TOKEN))
      )
    }

    "throw UnexpectedResponseException when the service returns 400 BAD_REQUEST" in {
      server.stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .withHeader("Authorization", equalTo(INTERNAL_AUTH_TOKEN))
          .willReturn(aResponse().withStatus(BAD_REQUEST))
      )

      whenReady(connector.check(FEATURE, vpdId).failed) { exception =>
        exception mustBe a[UnexpectedResponseException]
        exception.getMessage mustBe "Unexpected status: 400"
      }

      server.verify(
        postRequestedFor(urlEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .withHeader("Authorization", equalTo(INTERNAL_AUTH_TOKEN))
      )
    }

    "throw UnexpectedResponseException when the service returns 500 INTERNAL_SERVER_ERROR" in {
      server.stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .withHeader("Authorization", equalTo(INTERNAL_AUTH_TOKEN))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      whenReady(connector.check(FEATURE, vpdId).failed) { exception =>
        exception mustBe a[UnexpectedResponseException]
        exception.getMessage mustBe "Unexpected status: 500"
      }

      server.verify(
        postRequestedFor(urlEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .withHeader("Authorization", equalTo(INTERNAL_AUTH_TOKEN))
      )
    }

    "throw UnexpectedResponseException when the service returns 503 SERVICE_UNAVAILABLE" in {
      server.stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .withHeader("Authorization", equalTo(INTERNAL_AUTH_TOKEN))
          .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE))
      )

      whenReady(connector.check(FEATURE, vpdId).failed) { exception =>
        exception mustBe a[UnexpectedResponseException]
        exception.getMessage mustBe "Unexpected status: 503"
      }

      server.verify(
        postRequestedFor(urlEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .withHeader("Authorization", equalTo(INTERNAL_AUTH_TOKEN))
      )
    }

    "throw an exception when a network fault occurs" in {
      server.stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .withHeader("Authorization", equalTo(INTERNAL_AUTH_TOKEN))
          .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
      )

      whenReady(connector.check(FEATURE, vpdId).failed) { exception =>
        exception mustBe a[Exception]
      }

      server.verify(
        postRequestedFor(urlEqualTo(url))
          .withRequestBody(equalToJson(expectedRequestBody))
          .withHeader("Authorization", equalTo(INTERNAL_AUTH_TOKEN))
      )
    }
  }
}
