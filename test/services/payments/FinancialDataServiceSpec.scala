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
import models.payments.PaymentsResponse
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.Future

class FinancialDataServiceSpec extends SpecBase {

  "getPayments" - {
    "must return payments from the connector" in {
      val mockConnector = mock[FinancialDataConnector]
      val service = new FinancialDataService(mockConnector)

      when(mockConnector.getPayments(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(testPaymentsResponse))

      whenReady(service.getPayments(vpdId)) { result =>
        result mustBe testPaymentsResponse
      }
    }

    "must return empty sections when connector returns nothing" in {
      val mockConnector = mock[FinancialDataConnector]
      val service = new FinancialDataService(mockConnector)

      when(mockConnector.getPayments(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(PaymentsResponse.empty))

      whenReady(service.getPayments(vpdId)) { result =>
        result mustBe PaymentsResponse.empty
      }
    }

    "must propagate errors from the connector" in {
      val mockConnector = mock[FinancialDataConnector]
      val service = new FinancialDataService(mockConnector)

      when(mockConnector.getPayments(eqTo(vpdId))(using any()))
        .thenReturn(Future.failed(new RuntimeException("Connector error")))

      whenReady(service.getPayments(vpdId).failed) { exception =>
        exception.getMessage mustBe "Connector error"
      }
    }
  }

  "getOutstandingPayment" - {
    "must return the outstanding payment when charge reference exists" in {
      val mockConnector = mock[FinancialDataConnector]
      val service = new FinancialDataService(mockConnector)
      val chargeReference = "VPD38270541980"
      
      when(mockConnector.getPayments(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(testPaymentsResponse))

      whenReady(service.getOutstandingPayment(vpdId, chargeReference)) { result =>
        result.chargeReference mustBe chargeReference
        result.amountDue mustBe BigDecimal("500.00")
      }
    }

    "must throw NoSuchElementException when charge reference does not exist" in {
      val mockConnector = mock[FinancialDataConnector]
      val service = new FinancialDataService(mockConnector)
      val nonExistentReference = "DOES_NOT_EXIST"

      when(mockConnector.getPayments(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(testPaymentsResponse))

      whenReady(service.getOutstandingPayment(vpdId, nonExistentReference).failed) { exception =>
        exception mustBe a[NoSuchElementException]
        exception.getMessage must include(nonExistentReference)
      }
    }

    "must throw NoSuchElementException when there are no outstanding payments" in {
      val mockConnector = mock[FinancialDataConnector]
      val service = new FinancialDataService(mockConnector)
      val chargeReference = "XYZ123"

      when(mockConnector.getPayments(eqTo(vpdId))(using any()))
        .thenReturn(Future.successful(PaymentsResponse.empty))

      whenReady(service.getOutstandingPayment(vpdId, chargeReference).failed) { exception =>
        exception mustBe a[NoSuchElementException]
        exception.getMessage must include(chargeReference)
      }
    }

    "must propagate errors from the connector" in {
      val mockConnector = mock[FinancialDataConnector]
      val service = new FinancialDataService(mockConnector)
      val chargeReference = "XYZ123"

      when(mockConnector.getPayments(eqTo(vpdId))(using any()))
        .thenReturn(Future.failed(new RuntimeException("Connector error")))

      whenReady(service.getOutstandingPayment(vpdId, chargeReference).failed) { exception =>
        exception.getMessage mustBe "Connector error"
      }
    }
  }
}