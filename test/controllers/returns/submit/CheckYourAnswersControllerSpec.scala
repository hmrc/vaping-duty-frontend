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

package controllers.returns.submit

import base.SpecBase
import models.returns.AdjustmentsEligibility
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{DutyRateService, ObligationService}
import utils.ReturnsDateUtils
import viewmodels.returns.submit.CheckYourAnswersViewModel
import views.html.returns.submit.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  "ReturnsCheckYourAnswers Controller" - {

    "must return OK and the correct view for a GET when adjustments are eligible" in {

      val mockDutyRateService = mock[DutyRateService]
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(Seq(fulfilledObligation(periodKey))))
      
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(periodKey -> testDutyRate))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[services.returns.ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]
        val returnsDateUtils = application.injector.instanceOf[ReturnsDateUtils]
        val dutyRates = Map(periodKey -> testDutyRate)
        val vm = CheckYourAnswersViewModel(returnsUserAnswers, dutyRates, periodKey, returnsDateUtils, AdjustmentsEligibility.Eligible)(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, vm)(request, messages(application)).toString
      }
    }

    "must return OK and hide spoilt and adjustments cards when not eligible (first return)" in {

      val mockDutyRateService = mock[DutyRateService]
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(Seq(openObligation(periodKey))))
      
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(periodKey -> testDutyRate))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[services.returns.ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK
        
        val content = contentAsString(result)

        content must not include "Declare any spoilt products"
        content must not include "Declare any over or under-declared adjustments"
      }
    }

    "must show duty suspended card with only question row when duty suspended is not declared" in {

      val mockDutyRateService = mock[DutyRateService]
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(Seq(openObligation(periodKey))))

      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(periodKey -> testDutyRate))

      val userAnswersWithoutDutySuspended = returnsUserAnswers
        .set(pages.returns.DeclareDutySuspensePage, false).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswersWithoutDutySuspended))
        .overrides(
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK
        
        val content = contentAsString(result)
        
        content must include("Report duty suspended vaping deliveries")
        content must not include "Duty suspended deliveries declared"
      }
    }

    "must fail when obligation service fails" in {

      val mockDutyRateService = mock[DutyRateService]
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.failed(RuntimeException("Obligation service failed")))
      
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(periodKey -> testDutyRate))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustBe "Obligation service failed"
        }
      }
    }

    "must redirect to Declaration page for a POST" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.returns.submit.routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe s"${controllers.returns.submit.routes.DeclarationController.onPageLoad().url}?period=${periodKey.value}"
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(returnsUserAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, controllers.returns.submit.routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
