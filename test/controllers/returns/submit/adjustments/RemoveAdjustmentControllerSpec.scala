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

package controllers.returns.submit.adjustments

import base.SpecBase
import controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController
import controllers.routes.*
import forms.returns.adjustments.RemoveAdjustmentFormProvider
import models.NormalMode
import models.identifiers.PeriodKey
import models.returns.{DutyRate, ReturnsUserAnswers}
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.adjustments.{AdjustmentListPage, DeclareAdjustmentPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{DutyRateService, ObligationService, ReturnsUserAnswersService}
import views.html.returns.submit.adjustments.RemoveAdjustmentView

import scala.concurrent.Future

class RemoveAdjustmentControllerSpec extends SpecBase with MockitoSugar {

  private val reverseController = new ReverseRemoveAdjustmentController(_prefix = "/vaping-duty")

  private def removeAdjustmentGetUrl(periodKey: PeriodKey): String =
    reverseController.onPageLoad(NormalMode).url + s"?adjustmentPeriod=${periodKey.value}"

  private def removeAdjustmentPutUrl(periodKey: PeriodKey) =
    reverseController.onSubmit(NormalMode).url + s"?adjustmentPeriod=${periodKey.value}"

  private def stubbedObligationService: ObligationService = {
    val mockObligationService = mock[ObligationService]
    when(mockObligationService.getObligationsDirectly(any())(using any()))
      .thenReturn(Future.successful(Seq(fulfilledObligation(october2027), fulfilledObligation(november2027))))
    mockObligationService
  }

  private def stubbedDutyRateService: DutyRateService = {
    val mockDutyRateService = mock[DutyRateService]
    when(mockDutyRateService.getDutyRateForDate(any())).thenReturn(DutyRate(3000))
    mockDutyRateService
  }

  private def obligationServiceWithNoMatchingObligation: ObligationService = {
    val mockObligationService = mock[ObligationService]
    when(mockObligationService.getObligationsDirectly(any())(using any()))
      .thenReturn(Future.successful(Seq.empty))
    mockObligationService
  }

  "RemoveAdjustment Controller" - {

    "must return OK and the correct view for a GET" in {
      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(AdjustmentEntry(october2027, AdjustmentType.UnderDeclared, BigDecimal(1000)))))
        .success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[ObligationService].toInstance(stubbedObligationService),
          bind[DutyRateService].toInstance(stubbedDutyRateService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, removeAdjustmentGetUrl(october2027))

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Journey Recovery for a GET when no adjustmentPeriod is supplied" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, reverseController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET when the adjustmentPeriod does not match any entry" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[ObligationService].toInstance(stubbedObligationService),
          bind[DutyRateService].toInstance(stubbedDutyRateService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, removeAdjustmentGetUrl(october2027))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the summary page when confirmed removal leaves other entries" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(
          AdjustmentEntry(october2027, AdjustmentType.UnderDeclared, BigDecimal(1000)),
          AdjustmentEntry(november2027, AdjustmentType.OverDeclared, BigDecimal(500))
        )))
        .success.value

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[ObligationService].toInstance(stubbedObligationService),
            bind[DutyRateService].toInstance(stubbedDutyRateService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeAdjustmentPutUrl(october2027))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(AdjustmentCheckYourAnswersController.onPageLoad(NormalMode).url)
        verify(mockSessionRepository).set(any())(any())
      }
    }

    "must remove the entry and redirect on a confirmed submission even when no obligation exists for the period" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(
          AdjustmentEntry(october2027, AdjustmentType.UnderDeclared, BigDecimal(1000)),
          AdjustmentEntry(november2027, AdjustmentType.OverDeclared, BigDecimal(500))
        )))
        .success.value

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[ObligationService].toInstance(obligationServiceWithNoMatchingObligation)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeAdjustmentPutUrl(october2027))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(AdjustmentCheckYourAnswersController.onPageLoad(NormalMode).url)
        verify(mockSessionRepository).set(any())(any())
      }
    }

    "must redirect to the summary page and clear the declaration when confirmed removal leaves no entries" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(AdjustmentEntry(october2027, AdjustmentType.UnderDeclared, BigDecimal(1000)))))
        .success.value

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[ObligationService].toInstance(stubbedObligationService),
            bind[DutyRateService].toInstance(stubbedDutyRateService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeAdjustmentPutUrl(october2027))
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(AdjustmentCheckYourAnswersController.onPageLoad(NormalMode).url)

        val captor = ArgumentCaptor.forClass(classOf[ReturnsUserAnswers])
        verify(mockSessionRepository).set(captor.capture())(any())
        captor.getValue.get(DeclareAdjustmentPage) mustBe Some(false)
        captor.getValue.get(AdjustmentListPage) mustBe None
      }
    }

    "must redirect to the summary page without removing anything when the user cancels" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]

      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(AdjustmentEntry(october2027, AdjustmentType.UnderDeclared, BigDecimal(1000)))))
        .success.value

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[ObligationService].toInstance(stubbedObligationService),
            bind[DutyRateService].toInstance(stubbedDutyRateService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeAdjustmentPutUrl(october2027))
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(AdjustmentCheckYourAnswersController.onPageLoad(NormalMode).url)
        verify(mockSessionRepository, never).set(any())(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(AdjustmentEntry(october2027, AdjustmentType.UnderDeclared, BigDecimal(1000)))))
        .success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[ObligationService].toInstance(stubbedObligationService),
          bind[DutyRateService].toInstance(stubbedDutyRateService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, removeAdjustmentPutUrl(october2027))
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must redirect to Journey Recovery for a POST when no adjustmentPeriod is supplied" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, reverseController.onSubmit(NormalMode).url)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(returnsUserAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeAdjustmentGetUrl(october2027))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
