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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.DutyRateService
import utils.ReturnsDateUtils
import viewmodels.returns.submit.CheckYourAnswersViewModel
import views.html.returns.submit.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  "ReturnsCheckYourAnswers Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockDutyRateService = mock[DutyRateService]
      val mockObligationService = mock[services.returns.ObligationService]

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(Seq.empty))
      
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
        val vm = CheckYourAnswersViewModel(returnsUserAnswers, dutyRates, periodKey, returnsDateUtils)(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, vm)(request, messages(application)).toString
        
        val content = contentAsString(result)
        
        // Verify summary cards are present
        content must include("govuk-summary-card")
        content must include("Declare vaping products for duty")
        content must include("Declare any spoilt products")
        content must include("Declare any over or under-declared adjustments")
        
        // Verify inset text with total duty
        content must include("govuk-inset-text")
        content must include("Your total duty to pay is")
      }
    }

    "must show duty suspended card when duty suspended is declared" in {

      val mockDutyRateService = mock[DutyRateService]

      when(mockDutyRateService.getDutyRate(any(), any())(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))

      val userAnswersWithDutySuspended = returnsUserAnswers
        .set(pages.returns.DeclareDutySuspensePage, true).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswersWithDutySuspended))
        .overrides(bind[DutyRateService].toInstance(mockDutyRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK
        
        val content = contentAsString(result)
        
        // Verify duty suspended section is present
        content must include("Duty suspended summary")
        content must include("Report suspended vaping deliveries")
      }
    }

    "must not show duty suspended card when duty suspended is not declared" in {

      val mockDutyRateService = mock[DutyRateService]

      when(mockDutyRateService.getDutyRate(any(), any())(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))

      val userAnswersWithoutDutySuspended = returnsUserAnswers
        .set(pages.returns.DeclareDutySuspensePage, false).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswersWithoutDutySuspended))
        .overrides(bind[DutyRateService].toInstance(mockDutyRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK
        
        val content = contentAsString(result)
        
        // Verify duty suspended section is NOT present
        content must not include "Duty suspended summary"
        content must not include "Report suspended vaping deliveries"
      }
    }

    "must fail when obligation service fails" in {

      val mockDutyRateService = mock[DutyRateService]
      val mockObligationService = mock[services.returns.ObligationService]

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.failed(RuntimeException("Obligation service failed")))
      
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
