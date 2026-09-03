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

package connectors.email

import base.ISpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import models.email.{DutyDueEmail, DutyDueEmailParameters}
import play.api.Application
import play.api.http.Status.*
import play.api.libs.json.Json
import util.WireMockHelper

class EmailConnectorISpec extends ISpecBase with WireMockHelper {

  private lazy val application: Application = applicationBuilder()
    .configure(
      "microservice.services.email.port" -> server.port
    ).build()

  private val url            = "/hmrc/email"
  private lazy val connector = application.injector.instanceOf[EmailConnector]

  private val testEmail = DutyDueEmail(
    to = List("test@example.com"),
    parameters = DutyDueEmailParameters(
      recipientName = "John Smith",
      returnPeriod = "October 2026",
      submissionDate = "3 November 2026",
      chargeReference = "VPD38270541977",
      amountDue = "£1,234.50",
      paymentDueDate = "15 November 2026"
    )
  )

  "postEmail must" - {

    "post the request body to /hmrc/email and return the HttpResponse on success" in {
      server.stubFor(
        post(urlEqualTo(url))
          .withRequestBody(equalToJson(Json.toJson(testEmail).toString))
          .willReturn(aResponse().withStatus(ACCEPTED))
      )

      val result = connector.postEmail(testEmail).futureValue

      result.status mustBe ACCEPTED
    }

    "return the HttpResponse without throwing when a non-2xx status is returned" in {
      server.stubFor(
        post(urlEqualTo(url))
          .willReturn(aResponse().withStatus(BAD_REQUEST))
      )

      val result = connector.postEmail(testEmail).futureValue

      result.status mustBe BAD_REQUEST
    }
  }
}
