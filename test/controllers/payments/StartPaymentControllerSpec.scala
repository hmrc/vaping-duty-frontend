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

package controllers.payments

import base.SpecBase
import controllers.routes
import models.payments.StartPaymentResponse
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.payments.PaymentService
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class StartPaymentControllerSpec extends SpecBase {

  private val chargeReference = "VPD38270541977"
  private val amountInPence = 33000000L

  private val paymentResponse = StartPaymentResponse(
    journeyId = "journey-123",
    nextUrl = "https://payment-provider.example.com/pay"
  )

  "startPayment must" - {

    "redirect to payment provider URL when service returns successfully" in {
      val mockService = mock[PaymentService]
      
      when(mockAppConfig.host).thenReturn("http://localhost:9000")
      when(mockService.startPayment(
        eqTo(vpdId),
        eqTo(chargeReference),
        eqTo(amountInPence),
        eqTo("http://localhost:9000/vaping-duty/view-payments"),
        eqTo("http://localhost:9000/vaping-duty/view-payments")
      )(using any()))
        .thenReturn(Future.successful(paymentResponse))

      val controller = new StartPaymentController(
        fakeApprovedVapingManufacturerAuthAction,
        mockService,
        mockAppConfig,
        stubMessagesControllerComponents()
      )

      val request = FakeRequest(GET, controllers.payments.routes.StartPaymentController.startPayment(chargeReference, amountInPence).url)
      val result = controller.startPayment(chargeReference, amountInPence)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some("https://payment-provider.example.com/pay")
    }

    "redirect to journey recovery when service fails" in {
      val mockService = mock[PaymentService]
      
      when(mockAppConfig.host).thenReturn("http://localhost:9000")
      when(mockService.startPayment(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.failed(new RuntimeException("Service error")))

      val controller = new StartPaymentController(
        fakeApprovedVapingManufacturerAuthAction,
        mockService,
        mockAppConfig,
        stubMessagesControllerComponents()
      )

      val request = FakeRequest(GET, controllers.payments.routes.StartPaymentController.startPayment(chargeReference, amountInPence).url)
      val result = controller.startPayment(chargeReference, amountInPence)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.JourneyRecoveryController.onPageLoad().url)
    }

    "redirect to journey recovery when connector throws InternalServerException" in {
      val mockService = mock[PaymentService]
      
      when(mockAppConfig.host).thenReturn("http://localhost:9000")
      when(mockService.startPayment(any(), any(), any(), any(), any())(using any()))
        .thenReturn(Future.failed(InternalServerException("Failed to start payment")))

      val controller = new StartPaymentController(
        fakeApprovedVapingManufacturerAuthAction,
        mockService,
        mockAppConfig,
        stubMessagesControllerComponents()
      )

      val request = FakeRequest(GET, controllers.payments.routes.StartPaymentController.startPayment(chargeReference, amountInPence).url)
      val result = controller.startPayment(chargeReference, amountInPence)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(routes.JourneyRecoveryController.onPageLoad().url)
    }

    "pass correct parameters to service" in {
      val mockService = mock[PaymentService]
      
      when(mockAppConfig.host).thenReturn("http://localhost:9000")
      when(mockService.startPayment(
        eqTo(vpdId),
        eqTo(chargeReference),
        eqTo(amountInPence),
        eqTo("http://localhost:9000/vaping-duty/view-payments"),
        eqTo("http://localhost:9000/vaping-duty/view-payments")
      )(using any()))
        .thenReturn(Future.successful(paymentResponse))

      val controller = new StartPaymentController(
        fakeApprovedVapingManufacturerAuthAction,
        mockService,
        mockAppConfig,
        stubMessagesControllerComponents()
      )

      val request = FakeRequest(GET, controllers.payments.routes.StartPaymentController.startPayment(chargeReference, amountInPence).url)
      val result = controller.startPayment(chargeReference, amountInPence)(request)

      status(result) mustBe SEE_OTHER
      verify(mockService).startPayment(
        eqTo(vpdId),
        eqTo(chargeReference),
        eqTo(amountInPence),
        eqTo("http://localhost:9000/vaping-duty/view-payments"),
        eqTo("http://localhost:9000/vaping-duty/view-payments")
      )(using any())
    }
  }
}