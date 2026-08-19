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
import connectors.SubscriptionConnector
import data.TestData
import models.SubscriptionInsolvencyStatus
import models.contactPreference.PaperlessPreference.toValue
import models.contactPreference.{PaperlessPreference, SubscriptionContactPreferences}
import play.api.Application
import play.api.http.Status.{BAD_GATEWAY, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import util.WireMockHelper

class SubscriptionConnectorISpec extends ISpecBase with WireMockHelper with TestData {

  private lazy val application: Application = applicationBuilder()
    .configure(
      "microservice.services.vaping-duty-account.port" -> server.port
    ).build()

  private val url            = s"/vaping-duty-account/get-preferences/$vpdId"
  private lazy val connector = application.injector.instanceOf[SubscriptionConnector]

  "SubscriptionConnector must" - {
    "getSubscriptionContactPreferences" - {
      val summary = SubscriptionContactPreferences(toValue(PaperlessPreference.Email), Some(emailAddress))
      
      "successfully submit contact preferences" in {
        server.stubFor(
          get(url).willReturn(aResponse().withStatus(OK).withBody(
            Json.toJson(summary).toString
          ))
        )

        val result = connector.getSubscriptionContactPreferences(vpdId).futureValue

        result mustBe Right(summary)
      }

      "fail when invalid JSON is returned" in {
        server.stubFor(
          get(url).willReturn(aResponse().withStatus(OK).withBody(Json.toJson("InvalidJSON").toString))
        )

        val result = connector.getSubscriptionContactPreferences(vpdId).futureValue

        result mustBe Left(
          ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse subscription contact preferences", None, None)
        )
      }

      "fail when an unexpected response is returned" in {
        server.stubFor(
          get(url).willReturn(aResponse().withStatus(BAD_GATEWAY))
        )

        val result = connector.getSubscriptionContactPreferences(vpdId).futureValue

        result mustBe Left(
          ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected response", None, None)
        )
      }
    }

    "getInsolvencyStatus" - {
      "return Right with Solvent status when successful" in {
        server.stubFor(
          get(url).willReturn(aResponse().withStatus(OK).withBody(
            Json.toJson(subscriptionInsolvencyStatusSolvent).toString
          ))
        )

        val result = connector.getInsolvencyStatus(vpdId).futureValue

        result mustBe Right(subscriptionInsolvencyStatusSolvent)
      }

      "return Right with Insolvent status when successful" in {
        server.stubFor(
          get(url).willReturn(aResponse().withStatus(OK).withBody(
            Json.toJson(subscriptionInsolvencyStatusInsolvent).toString
          ))
        )

        val result = connector.getInsolvencyStatus(vpdId).futureValue

        result mustBe Right(subscriptionInsolvencyStatusInsolvent)
      }

      "fail when invalid JSON is returned" in {
        server.stubFor(
          get(url).willReturn(aResponse().withStatus(OK).withBody(Json.toJson("InvalidJSON").toString))
        )

        val result = connector.getInsolvencyStatus(vpdId).futureValue

        result mustBe Left(
          ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse insolvency status", None, None)
        )
      }

      "fail when BAD_REQUEST is returned" in {
        server.stubFor(
          get(url).willReturn(aResponse().withStatus(play.api.http.Status.BAD_REQUEST))
        )

        val result = connector.getInsolvencyStatus(vpdId).futureValue

        result mustBe Left(
          ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected response", None, None)
        )
      }

      "fail when NOT_FOUND is returned" in {
        server.stubFor(
          get(url).willReturn(aResponse().withStatus(play.api.http.Status.NOT_FOUND))
        )

        val result = connector.getInsolvencyStatus(vpdId).futureValue

        result mustBe Left(
          ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected response", None, None)
        )
      }

      "fail when UNPROCESSABLE_ENTITY is returned" in {
        server.stubFor(
          get(url).willReturn(aResponse().withStatus(play.api.http.Status.UNPROCESSABLE_ENTITY))
        )

        val result = connector.getInsolvencyStatus(vpdId).futureValue

        result mustBe Left(
          ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected response", None, None)
        )
      }

      "fail when INTERNAL_SERVER_ERROR is returned" in {
        server.stubFor(
          get(url).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
        )

        val result = connector.getInsolvencyStatus(vpdId).futureValue

        result mustBe Left(
          ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected response", None, None)
        )
      }

      "fail when BAD_GATEWAY is returned" in {
        server.stubFor(
          get(url).willReturn(aResponse().withStatus(BAD_GATEWAY))
        )

        val result = connector.getInsolvencyStatus(vpdId).futureValue

        result mustBe Left(
          ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected response", None, None)
        )
      }
    }
  }
}
