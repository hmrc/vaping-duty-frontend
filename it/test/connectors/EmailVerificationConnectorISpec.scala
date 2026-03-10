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
import com.github.tomakehurst.wiremock.client.WireMock.*
import data.TestData
import models.emailverification.{ErrorModel, RedirectUri}
import play.api.Application
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import util.WireMockHelper

class EmailVerificationConnectorISpec extends ISpecBase with WireMockHelper with TestData {

  private lazy val application: Application = applicationBuilder()
    .configure(
      "microservice.services.vaping-duty-account.port" -> server.port,
      "microservice.services.email-verification.port"  -> server.port
    ).build()

  private val getUrl   = s"/vaping-duty-account/get-email-verification/$credId"
  private val startUrl = s"/email-verification/verify-email"

  private lazy val connector        = application.injector.instanceOf[EmailVerificationConnector]
  val testGetResponseString: String = Json.toJson(testGetVerificationStatusResponse).toString

  "getEmailVerification" - {

    "must successfully fetch valid verification details for a given user" in {
      server.stubFor(
        get(getUrl).willReturn(aResponse().withStatus(OK).withBody(testGetResponseString))
      )

      val result = connector.getEmailVerification(testVerificationDetails).value.futureValue

      result mustBe Right(testGetVerificationStatusResponse)
    }

    "must return an error when invalid verification details are returned" in {
      server.stubFor(
        get(getUrl).willReturn(aResponse().withStatus(OK).withBody("Invalid"))
      )

      val result = connector.getEmailVerification(testVerificationDetails).value.futureValue

      result mustBe Left(
        ErrorModel(INTERNAL_SERVER_ERROR, "Invalid JSON format. Could not parse response as GetVerificationStatusResponse")
      )
    }

    "must return an error when http client returns a response with an error code" in {
      server.stubFor(
        get(getUrl).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      val result = connector.getEmailVerification(testVerificationDetails).value.futureValue

      result mustBe Left(ErrorModel(INTERNAL_SERVER_ERROR, "Unexpected response. Status: 500"))
    }
  }

  "startEmailVerification" - {

    "must successfully fetch a redirect url when a valid request is made" in {
      server.stubFor(
        post(startUrl).willReturn(aResponse().withStatus(CREATED).withBody(testJsonRedirectUriString))
      )

      val result = connector.startEmailVerification(testEmailVerificationRequest).futureValue

      result mustBe Right(testRedirectUri)
    }

    "must return an error if an invalid redirect url is returned when a valid request is made" in {
      server.stubFor(
        post(startUrl).willReturn(aResponse().withStatus(CREATED).withBody("Invalid"))
      )

      val result = connector.startEmailVerification(testEmailVerificationRequest).futureValue

      result mustBe Left(ErrorModel(INTERNAL_SERVER_ERROR, "Invalid JSON format, failed to parse response as a RedirectUrl"))
    }

    "must return an error if an error response status is returned" in {
      server.stubFor(
        post(startUrl).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      val result = connector.startEmailVerification(testEmailVerificationRequest).futureValue

      result mustBe Left(ErrorModel(INTERNAL_SERVER_ERROR, "Unexpected response from email verification service. Http status: 500"))
    }
  }
}
