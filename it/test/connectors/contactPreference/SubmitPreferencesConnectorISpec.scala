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
import models.emailverification.ErrorModel
import play.api.Application
import play.api.http.Status.{BAD_GATEWAY, CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import util.WireMockHelper

class SubmitPreferencesConnectorISpec extends ISpecBase with WireMockHelper with TestData {

  private lazy val application: Application = applicationBuilder()
    .configure(
      "microservice.services.vaping-duty-account.port" -> server.port
    ).build()

  private val url            = s"/vaping-duty-account/submit-preferences/$vpdId"
  private lazy val connector = application.injector.instanceOf[SubmitPreferencesConnector]

  "submitContactPreferences must" - {

    "successfully submit contact preferences" in {
      server.stubFor(
        put(url).willReturn(aResponse().withStatus(OK).withBody(Json.toJson(testSubmissionResponse).toString))
      )

      val result = connector.submitContactPreferences(contactPreferenceSubmissionPost, vpdId).futureValue

      result mustBe Right(testSubmissionResponse)
    }

    "fail when invalid JSON is returned" in {
      server.stubFor(
        put(url).willReturn(aResponse().withStatus(OK).withBody(Json.toJson("InvalidJSON").toString))
      )

      val result = connector.submitContactPreferences(contactPreferenceSubmissionPost, vpdId).futureValue

      result mustBe Left(
        ErrorModel(INTERNAL_SERVER_ERROR, "Invalid JSON format. Could not parse response as PaperlessPreferenceSubmittedResponse")
      )
    }

    "fail when an unexpected response is returned" in {
      server.stubFor(
        put(url).willReturn(aResponse().withStatus(BAD_GATEWAY))
      )

      val result = connector.submitContactPreferences(contactPreferenceSubmissionPost, vpdId).futureValue

      result mustBe Left(ErrorModel(BAD_GATEWAY, "Unexpected response. Status: 502"))
    }

    "fail when an unexpected status code is returned" in {
      server.stubFor(
        put(url).willReturn(aResponse().withStatus(CREATED))
      )

      val result = connector.submitContactPreferences(contactPreferenceSubmissionPost, vpdId).futureValue

      result mustBe Left(ErrorModel(INTERNAL_SERVER_ERROR, "Unexpected status code when submitting contact preferences: 201"))
    }
  }
}
