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
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.payments.PaymentService
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class StartPaymentControllerSpec extends SpecBase {

  private val chargeReference = "VPD38270541977"

  private val paymentResponse = StartPaymentResponse(
    journeyId = "journey-123",
    nextUrl = "https://payment-provider.example.com/pay"
  )

  "startPayment must" - {

    "redirect to payment provider URL when service returns successfully" in {
      val mockPaymentService = mock[PaymentService]
      
      when(mockPaymentService.startPayment(
        eqTo(vpdId),
        eqTo(chargeReference),
        any(),
        any()
      )(using any()))
        .thenReturn(Future.successful(paymentResponse))

      val application = applicationBuilder()
        .overrides(bind[PaymentService].toInstance(mockPaymentService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.payments.routes.StartPaymentController.startPayment(chargeReference).url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some("https://payment-provider.example.com/pay")
        
        verify(mockPaymentService).startPayment(
          eqTo(vpdId),
          eqTo(chargeReference),
          any(),
          any()
        )(using any())
      }
    }

    "redirect to journey recovery when payment service fails" in {
      val mockPaymentService = mock[PaymentService]
      
      when(mockPaymentService.startPayment(any(), any(), any(), any())(using any()))
        .thenReturn(Future.failed(new RuntimeException("Payment service error")))

      val application = applicationBuilder()
        .overrides(bind[PaymentService].toInstance(mockPaymentService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.payments.routes.StartPaymentController.startPayment(chargeReference).url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to journey recovery when payment not found" in {
      val mockPaymentService = mock[PaymentService]
      
      when(mockPaymentService.startPayment(any(), any(), any(), any())(using any()))
        .thenReturn(Future.failed(new NoSuchElementException(s"No outstanding payment found for charge reference: $chargeReference")))

      val application = applicationBuilder()
        .overrides(bind[PaymentService].toInstance(mockPaymentService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.payments.routes.StartPaymentController.startPayment(chargeReference).url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to journey recovery when connector throws InternalServerException" in {
      val mockPaymentService = mock[PaymentService]
      
      when(mockPaymentService.startPayment(any(), any(), any(), any())(using any()))
        .thenReturn(Future.failed(InternalServerException("Failed to start payment")))

      val application = applicationBuilder()
        .overrides(bind[PaymentService].toInstance(mockPaymentService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.payments.routes.StartPaymentController.startPayment(chargeReference).url)
        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
