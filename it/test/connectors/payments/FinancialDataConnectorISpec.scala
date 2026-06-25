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

package connectors.payments

import base.ISpecBase
import com.github.tomakehurst.wiremock.client.WireMock.*
import data.TestData
import models.payments.OutstandingPayment
import play.api.Application
import play.api.http.Status.*
import play.api.libs.json.Json
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.vapingdutyfinance.models.PaymentStatus
import util.WireMockHelper

class FinancialDataConnectorISpec extends ISpecBase with WireMockHelper with TestData {

  private lazy val application: Application = applicationBuilder()
    .configure(
      "microservice.services.vaping-duty-finance.port" -> server.port
    ).build()

  private val url            = "/vaping-duty-finance/financial-data/outstanding-payments"
  private lazy val connector = application.injector.instanceOf[FinancialDataConnector]

  private val testPayment = OutstandingPayment(
    chargeReference = "VPD38270541977",
    period = "December 2026",
    amountDue = BigDecimal("330000.00"),
    dueDate = "2026-12-15",
    status = PaymentStatus.Due
  )

  "getOutstandingPayments must" - {

    "successfully fetch outstanding payments" in {
      server.stubFor(
        get(url).willReturn(aResponse().withStatus(OK).withBody(Json.toJson(Seq(testPayment)).toString))
      )

      val result = connector.getOutstandingPayments(vpdId).futureValue

      result mustBe Seq(testPayment)
    }

    "fail when invalid JSON is returned" in {
      server.stubFor(
        get(url).willReturn(aResponse().withStatus(OK).withBody("invalid json"))
      )

      val result = connector.getOutstandingPayments(vpdId)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage must include("Parsing failed")
      }
    }

    "fail when http client returns an error response" in {
      server.stubFor(
        get(url).willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR))
      )

      val result = connector.getOutstandingPayments(vpdId)

      whenReady(result.failed) { exception =>
        exception mustBe an[InternalServerException]
        exception.getMessage must include("Failed to get outstanding payments")
      }
    }
  }
}
