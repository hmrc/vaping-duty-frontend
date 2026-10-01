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
import play.api.Application
import play.api.http.Status.*
import util.WireMockHelper

class UserAllowListConnectorISpec extends ISpecBase with WireMockHelper {

  override def fakeApplication(): Application = applicationBuilder()
    .configure(
      "microservice.services.user-allow-list.protocol" -> "http",
      "microservice.services.user-allow-list.host" -> "localhost",
      "microservice.services.user-allow-list.port" -> server.port(),
      "features.returnsAllowListEnabled" -> true
    )
    .build()

  private lazy val connector = app.injector.instanceOf[UserAllowListConnector]

  "UserAllowListConnector must" - {

    "return true when the service returns 200 with true" in {
      server.stubFor(
        get(urlEqualTo(s"/user-allow-list/vaping-duty-frontend/check/${vpdId.value}"))
          .willReturn(aResponse().withStatus(OK).withBody("true"))
      )

      whenReady(connector.check(vpdId.value)) { result =>
        result mustBe true
      }
    }

    "return false when the service returns 200 with false" in {
      server.stubFor(
        get(urlEqualTo(s"/user-allow-list/vaping-duty-frontend/check/${vpdId.value}"))
          .willReturn(aResponse().withStatus(OK).withBody("false"))
      )

      whenReady(connector.check(vpdId.value)) { result =>
        result mustBe false
      }
    }

    "return false when the service returns 404" in {
      server.stubFor(
        get(urlEqualTo(s"/user-allow-list/vaping-duty-frontend/check/${vpdId.value}"))
          .willReturn(aResponse().withStatus(NOT_FOUND))
      )

      whenReady(connector.check(vpdId.value)) { result =>
        result mustBe false
      }
    }

    "throw UnexpectedResponseException when the service returns 400" in {
      server.stubFor(
        get(urlEqualTo(s"/user-allow-list/vaping-duty-frontend/check/${vpdId.value}"))
          .willReturn(aResponse().withStatus(BAD_REQUEST))
      )

      whenReady(connector.check(vpdId.value).failed) { exception =>
        exception mustBe a[UnexpectedResponseException]
        exception.getMessage must include("User allow list check failed with status 400")
      }
    }

    "throw UnexpectedResponseException when the service returns 500" in {
      server.stubFor(
        get(urlEqualTo(s"/user-allow-list/vaping-duty-frontend/check/${vpdId.value}"))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      whenReady(connector.check(vpdId.value).failed) { exception =>
        exception mustBe a[UnexpectedResponseException]
        exception.getMessage must include("User allow list check failed with status 500")
      }
    }

    "throw UnexpectedResponseException when the service returns 503" in {
      server.stubFor(
        get(urlEqualTo(s"/user-allow-list/vaping-duty-frontend/check/${vpdId.value}"))
          .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE))
      )

      whenReady(connector.check(vpdId.value).failed) { exception =>
        exception mustBe a[UnexpectedResponseException]
        exception.getMessage must include("User allow list check failed with status 503")
      }
    }

    "throw UnexpectedResponseException when a network fault occurs" in {
      server.stubFor(
        get(urlEqualTo(s"/user-allow-list/vaping-duty-frontend/check/${vpdId.value}"))
          .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
      )

      whenReady(connector.check(vpdId.value).failed) { exception =>
        exception mustBe a[UnexpectedResponseException]
        exception.getMessage must include("User allow list check failed")
      }
    }

    "throw UnexpectedResponseException when the response body cannot be parsed" in {
      server.stubFor(
        get(urlEqualTo(s"/user-allow-list/vaping-duty-frontend/check/${vpdId.value}"))
          .willReturn(aResponse().withStatus(OK).withBody("invalid-json"))
      )

      whenReady(connector.check(vpdId.value).failed) { exception =>
        exception mustBe a[UnexpectedResponseException]
        exception.getMessage must include("Failed to parse user allow list response")
      }
    }

    "throw UnexpectedResponseException when the response is 200 but body is not a boolean" in {
      server.stubFor(
        get(urlEqualTo(s"/user-allow-list/vaping-duty-frontend/check/${vpdId.value}"))
          .willReturn(aResponse().withStatus(OK).withBody("""{"key": "value"}"""))
      )

      whenReady(connector.check(vpdId.value).failed) { exception =>
        exception mustBe a[UnexpectedResponseException]
        exception.getMessage must include("Failed to parse user allow list response")
      }
    }
  }
}
