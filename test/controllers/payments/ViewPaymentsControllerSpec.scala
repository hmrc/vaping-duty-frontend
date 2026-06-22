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
import controllers.JourneyRecoveryController
import models.payments.OutstandingPayment
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.payments.FinancialDataService
import views.html.payments.ViewPaymentsView

import scala.concurrent.Future

class ViewPaymentsControllerSpec extends SpecBase {

  val testPayment = OutstandingPayment(
    chargeReference = "VPD38270541977",
    period = "December 2026",
    amountDue = BigDecimal("330000.00"),
    dueDate = "2026-12-15",
    status = "Due"
  )

  "ViewPaymentsController" - {
    "onPageLoad" - {
      "must return OK and the correct view when payments exist" in {
        val mockService = mock[FinancialDataService]
        when(mockService.getOutstandingPayments(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq(testPayment)))

        val application = applicationBuilder()
          .overrides(bind[FinancialDataService].toInstance(mockService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.ViewPaymentsController.onPageLoad().url)
          val result = route(application, request).value
          val view = application.injector.instanceOf[ViewPaymentsView]

          status(result) mustBe OK
          contentAsString(result) must include("Payment overview")
          contentAsString(result) must include("£330,000")
        }
      }

      "must return OK and show no payments message when no payments exist" in {
        val mockService = mock[FinancialDataService]
        when(mockService.getOutstandingPayments(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq.empty))

        val application = applicationBuilder()
          .overrides(bind[FinancialDataService].toInstance(mockService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.ViewPaymentsController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustBe OK
          contentAsString(result) must include("Payment overview")
          contentAsString(result) must include("£0")
        }
      }

      "must redirect to journey recovery when service returns an error" in {
        val mockService = mock[FinancialDataService]
        when(mockService.getOutstandingPayments(eqTo(vpdId))(using any()))
          .thenReturn(Future.failed(new RuntimeException("Service error")))

        val application = applicationBuilder()
          .overrides(bind[FinancialDataService].toInstance(mockService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.ViewPaymentsController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must display only the first payment when multiple payments exist" in {
        val secondPayment = testPayment.copy(
          chargeReference = "VPD38270541978",
          amountDue = BigDecimal("100000.00")
        )

        val mockService = mock[FinancialDataService]
        when(mockService.getOutstandingPayments(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq(testPayment, secondPayment)))

        val application = applicationBuilder()
          .overrides(bind[FinancialDataService].toInstance(mockService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.ViewPaymentsController.onPageLoad().url)
          val result = route(application, request).value

          status(result) mustBe OK
          contentAsString(result) must include("VPD38270541977")
          contentAsString(result) must not include "VPD38270541978"
        }
      }
    }
  }
}