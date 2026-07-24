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
}
