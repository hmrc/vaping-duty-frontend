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
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlMatching}
import org.scalatest.freespec.AnyFreeSpec
import play.api.Application
import play.api.http.Status.*
import util.WireMockHelper

class VapingDutyConnectorHttpISpec extends ISpecBase with WireMockHelper {
  override def fakeApplication(): Application = {
    applicationBuilder(None).configure("microservice.services.vaping-duty.port" -> server.port()).build()
  }

    "VapingDutyConnectorHttp" - {
      "ping" - {
        "must successfully ping the vaping duty service" in new Setup {
          server.stubFor(
            get(urlMatching(pingUrl))
              .willReturn(
                aResponse().withStatus(OK)
              )
          )

          whenReady(connector.ping()) { result =>
            result mustBe ()
          }
        }

        "must fail when authorisation fails" in new Setup {
          server.stubFor(
            get(urlMatching(pingUrl))
              .willReturn(
                aResponse()
                  .withStatus(UNAUTHORIZED)
              )
          )

          whenReady(connector.ping().failed) { e =>
            e.getMessage must include("Not authorised")
          }
        }
        
        "must fail when an unexpected status code is returned" in new Setup {
          server.stubFor(
            get(urlMatching(pingUrl))
              .willReturn(
                aResponse()
                  .withStatus(CREATED)
              )
          )

          whenReady(connector.ping().failed) { e =>
            e.getMessage must include("Unexpected status code: 201")
          }
        }

        "must fail with an Exception when an internal server error status code is returned" in new Setup {
          server.stubFor(
            get(urlMatching(pingUrl))
              .willReturn(
                aResponse()
                  .withStatus(INTERNAL_SERVER_ERROR)
                  .withStatusMessage("<test error message>")
              )
          )

          whenReady(connector.ping().failed) { e =>
            e.getMessage must include("Unexpected response")
            e.getMessage must include("returned 500")
          }
        }

      }
    }

    class Setup {
      val connector = app.injector.instanceOf[VapingDutyConnectorHttp]
      val pingUrl = "/vaping-duty/ping"
    }
}