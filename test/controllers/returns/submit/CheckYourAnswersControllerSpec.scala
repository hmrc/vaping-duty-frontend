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
import models.returns.{AdjustmentsEligibility, PeriodDisplay}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import models.returns.{DeclarationDetails, ReturnsUserAnswers}
import pages.returns.DeclarationPage
import pages.returns.yourdetails.{YourBusinessEmailPage, YourFullNamePage, YourRolePage}
import services.returns.{DutyRateService, ObligationService, ReturnsUserAnswersService, SubmitReturnService}
import viewmodels.returns.submit.CheckYourAnswersViewModel
import views.html.returns.submit.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  private val otherTasksCompleted = returnsUserAnswers
    .set(pages.returns.DeclareDutyPage, false).success.value
    .set(pages.returns.DeclareDutySuspensePage, false).success.value

  private val allTasksCompleted = otherTasksCompleted
    .set(YourFullNamePage, "John Smith").success.value
    .set(YourRolePage, "Director").success.value
    .set(YourBusinessEmailPage, "john.smith@example.com").success.value

  private def submissionMocks(): (SubmitReturnService, ReturnsUserAnswersService, ObligationService) = {
    val mockSubmitReturnService = mock[SubmitReturnService]
    val mockUserAnswersService = mock[ReturnsUserAnswersService]
    val mockObligationService = mock[ObligationService]

    when(mockSubmitReturnService.submit(any(), any())(any())).thenReturn(Future.successful(testReturnSubmissionResponse))
    when(mockUserAnswersService.clear(any(), any())(any())).thenReturn(Future.successful(Right(())))
    when(mockObligationService.getObligations(any())(using any())).thenReturn(Future.successful(Seq(openObligation(periodKey))))

    (mockSubmitReturnService, mockUserAnswersService, mockObligationService)
  }

  "ReturnsCheckYourAnswers Controller" - {

    "must return OK and the correct view for a GET when adjustments are eligible" in {

      val mockDutyRateService = mock[DutyRateService]
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligations(any())(using any()))
        .thenReturn(Future.successful(Seq(fulfilledObligation(periodKey))))
      
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(periodKey -> testDutyRate))

      val completedAnswers = returnsUserAnswers
        .set(pages.returns.DeclareDutyPage, false).success.value
        .set(pages.returns.DeclareDutySuspensePage, false).success.value
        .set(pages.returns.DeclareSpoiltProductsPage, false).success.value
        .set(pages.returns.adjustments.DeclareAdjustmentPage, false).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(completedAnswers))
        .overrides(
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[services.returns.ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]
        val dutyRates = Map(periodKey -> testDutyRate)
        val vm = CheckYourAnswersViewModel(completedAnswers, dutyRates, periodKey, AdjustmentsEligibility.Eligible, PeriodDisplay("June", "2026"))(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, vm, yourDetailsEnabled = false)(request, messages(application)).toString
      }
    }

    "must return OK and hide spoilt and adjustments cards when not eligible (first return)" in {

      val mockDutyRateService = mock[DutyRateService]
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligations(any())(using any()))
        .thenReturn(Future.successful(Seq(openObligation(periodKey))))
      
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(periodKey -> testDutyRate))

      val notEligibleAnswers = returnsUserAnswers
        .set(pages.returns.DeclareDutyPage, false).success.value
        .set(pages.returns.DeclareDutySuspensePage, false).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(notEligibleAnswers))
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

      when(mockObligationService.getObligations(any())(using any()))
        .thenReturn(Future.successful(Seq(openObligation(periodKey))))

      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(periodKey -> testDutyRate))

      val userAnswersWithoutDutySuspended = returnsUserAnswers
        .set(pages.returns.DeclareDutyPage, false).success.value
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

    "must redirect to return submission recovery when a task is not yet complete" in {

      val mockDutyRateService = mock[DutyRateService]
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligations(any())(using any()))
        .thenReturn(Future.successful(Seq(openObligation(periodKey))))

      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(periodKey -> testDutyRate))

      val incompleteAnswers = returnsUserAnswers
        .set(pages.returns.DeclareDutyPage, true).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(incompleteAnswers))
        .overrides(
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[services.returns.ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe s"${controllers.returns.submit.routes.ReturnSubmissionRecoveryController.onPageLoad().url}?period=${periodKey.value}"
      }
    }

    "must redirect to return submission recovery when your details is enabled and not yet complete" in {

      val mockDutyRateService = mock[DutyRateService]
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligations(any())(using any()))
        .thenReturn(Future.successful(Seq(openObligation(periodKey))))

      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(periodKey -> testDutyRate))

      val otherTasksCompleted = returnsUserAnswers
        .set(pages.returns.DeclareDutyPage, false).success.value
        .set(pages.returns.DeclareDutySuspensePage, false).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(otherTasksCompleted), yourDetailsEnabled = true)
        .overrides(
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[services.returns.ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe s"${controllers.returns.submit.routes.ReturnSubmissionRecoveryController.onPageLoad().url}?period=${periodKey.value}"
      }
    }

    "must fail when obligation service fails" in {

      val mockDutyRateService = mock[DutyRateService]
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligations(any())(using any()))
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

    "must show a Save and continue button when your details is disabled" in {

      val mockDutyRateService = mock[DutyRateService]
      val (_, _, mockObligationService) = submissionMocks()

      when(mockDutyRateService.getDutyRatesForPeriods(any(), any())).thenReturn(Map(periodKey -> testDutyRate))

      val application = applicationBuilder(returnsUserAnswers = Some(otherTasksCompleted))
        .overrides(
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val content = contentAsString(route(application, request).value)

        content must include(messages(application)("site.saveAndContinue"))
        content must not include messages(application)("returns.declaration.button")
      }
    }

    "must show a Confirm and submit button when your details is enabled" in {

      val mockDutyRateService = mock[DutyRateService]
      val (_, _, mockObligationService) = submissionMocks()

      when(mockDutyRateService.getDutyRatesForPeriods(any(), any())).thenReturn(Map(periodKey -> testDutyRate))

      val application = applicationBuilder(returnsUserAnswers = Some(allTasksCompleted), yourDetailsEnabled = true)
        .overrides(
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val content = contentAsString(route(application, request).value)

        content must include(messages(application)("returns.declaration.button"))
        content must include("data-prevent-double-click")
      }
    }

    "when your details is enabled, POST" - {

      "must submit the return once with the your details answers, clear the answers and redirect to Confirmation" in {

        val (mockSubmitReturnService, mockUserAnswersService, mockObligationService) = submissionMocks()

        val application = applicationBuilder(returnsUserAnswers = Some(allTasksCompleted), yourDetailsEnabled = true)
          .overrides(
            bind[SubmitReturnService].toInstance(mockSubmitReturnService),
            bind[ReturnsUserAnswersService].toInstance(mockUserAnswersService),
            bind[ObligationService].toInstance(mockObligationService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, controllers.returns.submit.routes.CheckYourAnswersController.onSubmit().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe s"${controllers.returns.submit.routes.ConfirmationController.onPageLoad().url}?period=${periodKey.value}"

          val captor = ArgumentCaptor.forClass(classOf[ReturnsUserAnswers])
          verify(mockSubmitReturnService, times(1)).submit(captor.capture(), any())(any())
          captor.getValue.get(DeclarationPage).value mustBe DeclarationDetails("John Smith", "Director", "john.smith@example.com")

          verify(mockUserAnswersService, times(1)).clear(any(), any())(any())
        }
      }

      "must redirect to Journey Recovery when the submission fails" in {

        val (mockSubmitReturnService, mockUserAnswersService, mockObligationService) = submissionMocks()

        when(mockSubmitReturnService.submit(any(), any())(any())).thenReturn(Future.failed(new RuntimeException("submission failed")))

        val application = applicationBuilder(returnsUserAnswers = Some(allTasksCompleted), yourDetailsEnabled = true)
          .overrides(
            bind[SubmitReturnService].toInstance(mockSubmitReturnService),
            bind[ReturnsUserAnswersService].toInstance(mockUserAnswersService),
            bind[ObligationService].toInstance(mockObligationService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, controllers.returns.submit.routes.CheckYourAnswersController.onSubmit().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
          verify(mockUserAnswersService, never()).clear(any(), any())(any())
        }
      }

      "must redirect to return submission recovery and not submit when your details is not complete" in {

        val (mockSubmitReturnService, mockUserAnswersService, mockObligationService) = submissionMocks()

        val application = applicationBuilder(returnsUserAnswers = Some(otherTasksCompleted), yourDetailsEnabled = true)
          .overrides(
            bind[SubmitReturnService].toInstance(mockSubmitReturnService),
            bind[ReturnsUserAnswersService].toInstance(mockUserAnswersService),
            bind[ObligationService].toInstance(mockObligationService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, controllers.returns.submit.routes.CheckYourAnswersController.onSubmit().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe s"${controllers.returns.submit.routes.ReturnSubmissionRecoveryController.onPageLoad().url}?period=${periodKey.value}"
          verify(mockSubmitReturnService, never()).submit(any(), any())(any())
        }
      }
    }

    "must redirect to Declaration page and not submit for a POST when your details is disabled" in {

      val (mockSubmitReturnService, _, _) = submissionMocks()

      val application = applicationBuilder(returnsUserAnswers = Some(allTasksCompleted))
        .overrides(bind[SubmitReturnService].toInstance(mockSubmitReturnService))
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.returns.submit.routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe s"${controllers.returns.submit.routes.DeclarationController.onPageLoad().url}?period=${periodKey.value}"
        verify(mockSubmitReturnService, never()).submit(any(), any())(any())
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
