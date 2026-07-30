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
import connectors.payments.PaymentConnector
import models.payments.{OutstandingPayment, StartPaymentRequest, StartPaymentResponse}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.vapingdutyfinance.models.PaymentStatus

import java.time.LocalDate
import scala.concurrent.Future

class PaymentServiceSpec extends SpecBase with ScalaFutures {

  private val mockConnector: PaymentConnector = mock[PaymentConnector]
  private val mockFinancialDataService: FinancialDataService = mock[FinancialDataService]
  private val service = new PaymentService(mockConnector, mockFinancialDataService)

  private val chargeReference = "VPD38270541977"
  private val amountDue = BigDecimal("330000.00")
  private val amountInPence = 33000000L
  private val returnUrl = "http://localhost:9000/vaping-duty/view-payments"
  private val backUrl = "http://localhost:9000/vaping-duty/view-payments"

  private val outstandingPayment = OutstandingPayment(
    chargeReference = chargeReference,
    amountDue = amountDue,
    dueDate = LocalDate.of(2026, 8, 25),
    status = PaymentStatus.Due
  )

  private val expectedRequest = StartPaymentRequest(
    vapingDutyReference = vpdId.value,
    amountInPence = amountInPence,
    chargeReferenceNumber = chargeReference,
    returnUrl = returnUrl,
    backUrl = backUrl
  )

  private val expectedResponse = StartPaymentResponse(
    journeyId = "journey-123",
    nextUrl = "https://payment-provider.example.com/pay"
  )

  "startPayment must" - {

    "build correct StartPaymentRequest and call connector" in {
      when(mockFinancialDataService.getOutstandingPayment(eqTo(vpdId), eqTo(chargeReference))(using any()))
        .thenReturn(Future.successful(outstandingPayment))
      when(mockConnector.startPayment(eqTo(expectedRequest))(using any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = service.startPayment(
        vpdId,
        chargeReference,
        returnUrl,
        backUrl
      )

      whenReady(result) { response =>
        response mustBe expectedResponse
        verify(mockFinancialDataService).getOutstandingPayment(eqTo(vpdId), eqTo(chargeReference))(using any())
        verify(mockConnector).startPayment(eqTo(expectedRequest))(using any())
      }
    }

    "return StartPaymentResponse from connector" in {
      when(mockFinancialDataService.getOutstandingPayment(eqTo(vpdId), eqTo(chargeReference))(using any()))
        .thenReturn(Future.successful(outstandingPayment))
      when(mockConnector.startPayment(any())(using any()))
        .thenReturn(Future.successful(expectedResponse))

      val result = service.startPayment(
        vpdId,
        chargeReference,
        returnUrl,
        backUrl
      )

      whenReady(result) { response =>
        response.journeyId mustBe "journey-123"
        response.nextUrl mustBe "https://payment-provider.example.com/pay"
      }
    }

    "propagate connector failures" in {
      val expectedException = new RuntimeException("Connector error")

      when(mockFinancialDataService.getOutstandingPayment(eqTo(vpdId), eqTo(chargeReference))(using any()))
        .thenReturn(Future.successful(outstandingPayment))
      when(mockConnector.startPayment(any())(using any()))
        .thenReturn(Future.failed(expectedException))

      val result = service.startPayment(
        vpdId,
        chargeReference,
        returnUrl,
        backUrl
      )

      whenReady(result.failed) { exception =>
        exception mustBe expectedException
      }
    }

    "propagate financial data service failures" in {
      val expectedException = new NoSuchElementException("Payment not found")

      when(mockFinancialDataService.getOutstandingPayment(eqTo(vpdId), eqTo(chargeReference))(using any()))
        .thenReturn(Future.failed(expectedException))

      val result = service.startPayment(
        vpdId,
        chargeReference,
        returnUrl,
        backUrl
      )

      whenReady(result.failed) { exception =>
        exception mustBe a[NoSuchElementException]
        exception.getMessage must include("Payment not found")
      }
    }
  }
}
