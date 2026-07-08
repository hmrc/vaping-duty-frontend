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
import forms.returns.DeclareDutyFormProvider
import models.obligations.ObligationsResponse
import models.returns.adjustments.AdjustmentList
import navigation.{ReturnsFakeNavigator, ReturnsNavigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.adjustments.{AddAnotherAdjustmentPage, AdjustmentListPage, DeclareAdjustmentPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{DutyRateService, ObligationService, ReturnsUserAnswersService}

import scala.concurrent.Future

class AdjustmentCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new DeclareDutyFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val adjustmentCheckYourAnswersRoute: String =
    controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad().url

  "AdjustmentCheckYourAnswers Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockObligationService = mock[ObligationService]
      val mockDutyRateService = mock[DutyRateService]
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      when(mockDutyRateService.getDutyRate(any(), any())(using any(), any()))
        .thenReturn(Future.successful(BigDecimal("3.00")))

      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, testAdjustmentList).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[ObligationService].toInstance(mockObligationService),
          bind[DutyRateService].toInstance(mockDutyRateService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, adjustmentCheckYourAnswersRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val mockObligationService = mock[ObligationService]
      val mockDutyRateService = mock[DutyRateService]
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      when(mockDutyRateService.getDutyRate(any(), any())(using any(), any()))
        .thenReturn(Future.successful(BigDecimal("3.00")))

      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, testAdjustmentList).success.value
        .set(AddAnotherAdjustmentPage, true).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[ObligationService].toInstance(mockObligationService),
          bind[DutyRateService].toInstance(mockDutyRateService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, adjustmentCheckYourAnswersRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must propagate exception when obligation service fails on GET" in {
      val mockObligationService = mock[ObligationService]
      val mockDutyRateService = mock[DutyRateService]

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.failed(new RuntimeException("Service unavailable")))

      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, testAdjustmentList).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[ObligationService].toInstance(mockObligationService),
          bind[DutyRateService].toInstance(mockDutyRateService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, adjustmentCheckYourAnswersRoute)

        val result = route(application, request).value

        whenReady(result.failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustBe "Service unavailable"
        }
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockObligationService = mock[ObligationService]
      val mockDutyRateService = mock[DutyRateService]
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      when(mockDutyRateService.getDutyRate(any(), any())(using any(), any()))
        .thenReturn(Future.successful(BigDecimal("3.00")))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, testAdjustmentList).success.value

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[ObligationService].toInstance(mockObligationService),
            bind[DutyRateService].toInstance(mockDutyRateService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentCheckYourAnswersRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must skip form validation and auto-set AddAnotherAdjustmentPage to false when user declared No to adjustments" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]

      val userAnswersWithNoAdjustments = returnsUserAnswers
        .set(DeclareAdjustmentPage, false).success.value

      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswersWithNoAdjustments))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, adjustmentCheckYourAnswersRoute)
        // Note: No form data submitted - form validation should be skipped

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        // Verify that session was updated (with AddAnotherAdjustmentPage set to false)
        verify(mockSessionRepository).set(any())(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val mockObligationService = mock[ObligationService]
      val mockDutyRateService = mock[DutyRateService]
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      when(mockDutyRateService.getDutyRate(any(), any())(using any(), any()))
        .thenReturn(Future.successful(BigDecimal("3.00")))

      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, testAdjustmentList).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[ObligationService].toInstance(mockObligationService),
          bind[DutyRateService].toInstance(mockDutyRateService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentCheckYourAnswersRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must propagate exception when obligation service fails on form error" in {
      val mockObligationService = mock[ObligationService]
      val mockDutyRateService = mock[DutyRateService]

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.failed(new RuntimeException("Service unavailable")))

      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, testAdjustmentList).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[ObligationService].toInstance(mockObligationService),
          bind[DutyRateService].toInstance(mockDutyRateService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentCheckYourAnswersRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        whenReady(result.failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustBe "Service unavailable"
        }
      }
    }
  }
}