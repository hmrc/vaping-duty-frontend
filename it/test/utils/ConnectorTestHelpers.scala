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

package utils

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.Suite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
trait ConnectorTestHelpers extends WireMockHelper {
  this: Suite =>

  protected def endpointName: String

  protected def getWireMockAppConfig(endpoints: Seq[String]): Map[String, Any] =
    endpoints.flatMap { endpoint =>
      Seq(
        s"microservice.services.$endpoint.host" -> "localhost",
        s"microservice.services.$endpoint.port" -> server.port()
      )
    }.toMap

  protected def stubPost(url: String, status: Int, requestBody: String, responseBody: String): Unit =
    server.stubFor(
      post(urlPathEqualTo(url))
        .withRequestBody(equalToJson(requestBody))
        .willReturn(
          aResponse()
            .withStatus(status)
            .withBody(responseBody)
        )
    )

  protected def stubPostFault(url: String, requestBody: String, fault: Fault): Unit =
    server.stubFor(
      post(urlPathEqualTo(url))
        .withRequestBody(equalToJson(requestBody))
        .willReturn(
          aResponse()
            .withFault(fault)
        )
    )

  protected def verifyPost(url: String): Unit =
    server.verify(postRequestedFor(urlPathEqualTo(url)))

  protected def verifyPostWithHeader(url: String, headerName: String, headerValue: String): Unit =
    server.verify(
      postRequestedFor(urlPathEqualTo(url))
        .withHeader(headerName, equalTo(headerValue))
    )

  abstract class ConnectorFixture {
    lazy val app: Application =
      GuiceApplicationBuilder()
        .configure(getWireMockAppConfig(Seq(endpointName)))
        .build()
  }
}