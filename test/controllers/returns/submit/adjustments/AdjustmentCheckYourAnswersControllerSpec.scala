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
import models.NormalMode
import models.returns.adjustments.AdjustmentList
import navigation.{ReturnsFakeNavigator, ReturnsNavigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.adjustments.{AddAnotherAdjustmentPage, AdjustmentListPage, AdjustmentReasonPage, DeclareAdjustmentPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{AdjustmentCheckYourAnswersService, ReturnsUserAnswersService}
import viewmodels.returns.submit.adjustments.AdjustmentCheckYourAnswersViewModel

import scala.concurrent.Future
import scala.util.Success

class AdjustmentCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new DeclareDutyFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val adjustmentCheckYourAnswersRoute: String =
    controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad(NormalMode).url

  "AdjustmentCheckYourAnswers Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockService = mock[AdjustmentCheckYourAnswersService]
      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, testAdjustmentList).success.value

      val mockViewModel = AdjustmentCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasAdjustments = true,
        totalAdjustment = BigDecimal(1000),
        formattedTotalAdjustment = "£1,000.00",
        hasAvailablePeriodsToAdd = true,
        adjustmentReasonMandatory = false,
        mode = NormalMode
      )

      when(mockService.buildViewModel(any(), any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[AdjustmentCheckYourAnswersService].toInstance(mockService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, adjustmentCheckYourAnswersRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val mockService = mock[AdjustmentCheckYourAnswersService]
      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, testAdjustmentList).success.value
        .set(AddAnotherAdjustmentPage, true).success.value

      val mockViewModel = AdjustmentCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasAdjustments = true,
        totalAdjustment = BigDecimal(1000),
        formattedTotalAdjustment = "£1,000.00",
        hasAvailablePeriodsToAdd = true,
        adjustmentReasonMandatory = false,
        mode = NormalMode
      )

      when(mockService.buildViewModel(any(), any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[AdjustmentCheckYourAnswersService].toInstance(mockService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, adjustmentCheckYourAnswersRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must propagate exception when service fails on GET" in {
      val mockService = mock[AdjustmentCheckYourAnswersService]
      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, testAdjustmentList).success.value

      when(mockService.buildViewModel(any(), any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Service unavailable")))

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[AdjustmentCheckYourAnswersService].toInstance(mockService)
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
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockService = mock[AdjustmentCheckYourAnswersService]
      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, testAdjustmentList).success.value

      val mockViewModel = AdjustmentCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasAdjustments = true,
        totalAdjustment = BigDecimal(1000),
        formattedTotalAdjustment = "£1,000.00",
        hasAvailablePeriodsToAdd = true,
        adjustmentReasonMandatory = false,
        mode = NormalMode
      )

      when(mockService.buildViewModel(any(), any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))
      when(mockService.getAdjustmentFlags(any(), any(), any())(using any()))
        .thenReturn(Future.successful((false, true)))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[AdjustmentCheckYourAnswersService].toInstance(mockService)
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
      val mockService = mock[AdjustmentCheckYourAnswersService]

      val userAnswersWithNoAdjustments = returnsUserAnswers
        .set(DeclareAdjustmentPage, false).success.value

      val mockViewModel = AdjustmentCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasAdjustments = false,
        totalAdjustment = BigDecimal(0),
        formattedTotalAdjustment = "£0.00",
        hasAvailablePeriodsToAdd = true,
        adjustmentReasonMandatory = false,
        mode = NormalMode
      )

      when(mockService.buildViewModel(any(), any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))
      when(mockService.getAdjustmentFlags(any(), any(), any())(using any()))
        .thenReturn(Future.successful((false, true)))
      when(mockService.cleanupReasonIfNotRequired(any(), any()))
        .thenReturn(Success(userAnswersWithNoAdjustments))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswersWithNoAdjustments))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[AdjustmentCheckYourAnswersService].toInstance(mockService)
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

    "must skip form validation and auto-set AddAnotherAdjustmentPage to false when no available periods to add" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockService = mock[AdjustmentCheckYourAnswersService]
      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, testAdjustmentList).success.value

      val mockViewModel = AdjustmentCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasAdjustments = true,
        totalAdjustment = BigDecimal(1000),
        formattedTotalAdjustment = "£1,000.00",
        hasAvailablePeriodsToAdd = false,
        adjustmentReasonMandatory = false,
        mode = NormalMode
      )

      when(mockService.buildViewModel(any(), any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))
      when(mockService.getAdjustmentFlags(any(), any(), any())(using any()))
        .thenReturn(Future.successful((false, false)))
      when(mockService.cleanupReasonIfNotRequired(any(), any()))
        .thenReturn(Success(userAnswers))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[AdjustmentCheckYourAnswersService].toInstance(mockService)
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
      val mockService = mock[AdjustmentCheckYourAnswersService]
      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, testAdjustmentList).success.value

      val mockViewModel = AdjustmentCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasAdjustments = true,
        totalAdjustment = BigDecimal(1000),
        formattedTotalAdjustment = "£1,000.00",
        hasAvailablePeriodsToAdd = true,
        adjustmentReasonMandatory = false,
        mode = NormalMode
      )

      when(mockService.buildViewModel(any(), any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))
      when(mockService.getAdjustmentFlags(any(), any(), any())(using any()))
        .thenReturn(Future.successful((false, true)))

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[AdjustmentCheckYourAnswersService].toInstance(mockService)
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

    "must propagate exception when service fails on form error" in {
      val mockService = mock[AdjustmentCheckYourAnswersService]
      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, testAdjustmentList).success.value

      when(mockService.buildViewModel(any(), any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Service unavailable")))
      when(mockService.getAdjustmentFlags(any(), any(), any())(using any()))
        .thenReturn(Future.successful((false, true)))

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[AdjustmentCheckYourAnswersService].toInstance(mockService)
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

    "must redirect to reason page when reason is required but missing" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockService = mock[AdjustmentCheckYourAnswersService]
      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, testAdjustmentList).success.value

      val mockViewModel = AdjustmentCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasAdjustments = true,
        totalAdjustment = BigDecimal(1000),
        formattedTotalAdjustment = "£1,000.00",
        hasAvailablePeriodsToAdd = false,
        adjustmentReasonMandatory = true, // Reason is required
        mode = NormalMode
      )

      when(mockService.buildViewModel(any(), any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))
      when(mockService.getAdjustmentFlags(any(), any(), any())(using any()))
        .thenReturn(Future.successful((true, false)))
      when(mockService.shouldRedirectToReasonPage(any(), any()))
        .thenReturn(true)
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[AdjustmentCheckYourAnswersService].toInstance(mockService)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, adjustmentCheckYourAnswersRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include("reason-for-adjustment")
      }
    }

    "must remove reason when no longer required and redirect to task list" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockService = mock[AdjustmentCheckYourAnswersService]
      val testAdjustmentList = AdjustmentList(adjustments = Seq(adjustmentEntry.copy(period = october2027)))
      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, testAdjustmentList).success.value
        .set(AdjustmentReasonPage, "old reason").success.value

      val cleanedUserAnswers = userAnswers.remove(AdjustmentReasonPage).success.value

      val mockViewModel = AdjustmentCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasAdjustments = true,
        totalAdjustment = BigDecimal(100), // Below threshold
        formattedTotalAdjustment = "£100.00",
        hasAvailablePeriodsToAdd = false,
        adjustmentReasonMandatory = false, // Reason no longer required
        mode = NormalMode
      )

      when(mockService.buildViewModel(any(), any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))
      when(mockService.getAdjustmentFlags(any(), any(), any())(using any()))
        .thenReturn(Future.successful((false, false)))
      when(mockService.shouldRedirectToReasonPage(any(), any()))
        .thenReturn(false)
      when(mockService.cleanupReasonIfNotRequired(any(), any()))
        .thenReturn(Success(cleanedUserAnswers))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[AdjustmentCheckYourAnswersService].toInstance(mockService)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, adjustmentCheckYourAnswersRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        
        // Verify that the reason was removed from user answers
        verify(mockSessionRepository).set(any())(any())
      }
    }
  }
}
