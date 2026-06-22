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

package services.payments

import base.SpecBase
import connectors.payments.FinancialDataConnector
import models.payments.OutstandingPayment
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.Future

class FinancialDataServiceSpec extends SpecBase {

  val testPayment = OutstandingPayment(
    chargeReference = "VPD38270541977",
    period = "December 2026",
    amountDue = BigDecimal("330000.00"),
    dueDate = "2026-12-15",
    status = "Due"
  )

  "getOutstandingPayments" - {
    "must return payments from the connector" in {
      val mockConnector = mock[FinancialDataConnector]
      val service = new FinancialDataService(mockConnector)

      when(mockConnector.getOutstandingPayments(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(Seq(testPayment)))

      whenReady(service.getOutstandingPayments(vpdId)) { result =>
        result mustBe Seq(testPayment)
      }
    }

    "must return empty sequence when connector returns empty" in {
      val mockConnector = mock[FinancialDataConnector]
      val service = new FinancialDataService(mockConnector)

      when(mockConnector.getOutstandingPayments(eqTo(vpdId))(any()))
        .thenReturn(Future.successful(Seq.empty))

      whenReady(service.getOutstandingPayments(vpdId)) { result =>
        result mustBe Seq.empty
      }
    }

    "must propagate errors from the connector" in {
      val mockConnector = mock[FinancialDataConnector]
      val service = new FinancialDataService(mockConnector)

      when(mockConnector.getOutstandingPayments(eqTo(vpdId))(any()))
        .thenReturn(Future.failed(new RuntimeException("Connector error")))

      whenReady(service.getOutstandingPayments(vpdId).failed) { exception =>
        exception.getMessage mustBe "Connector error"
      }
    }
  }
}