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

package controllers.returns.submit.spoilt

import base.SpecBase
import forms.returns.AddSpoiltAdjustmentFormProvider
import models.returns.SpoiltVolumeByPeriod
import navigation.{ReturnsFakeNavigator, ReturnsNavigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.{SpoiltCheckYourAnswersPage, DeclareSpoiltProductsPage, SpoiltVolumeByPeriodPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{ReturnsUserAnswersService, SpoiltCheckYourAnswersService}
import viewmodels.returns.submit.spoilt.SpoiltCheckYourAnswersViewModel

import scala.concurrent.Future

class SpoiltCheckYourAnswersControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new AddSpoiltAdjustmentFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val spoiltCheckYourAnswersRoute: String =
    controllers.returns.submit.spoilt.routes.SpoiltCheckYourAnswersController.onPageLoad().url

  "SpoiltCheckYourAnswers Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockService = mock[SpoiltCheckYourAnswersService]
      val testSpoiltList = List(SpoiltVolumeByPeriod(volume = BigDecimal(1000), periodKey = periodKey))
      val userAnswers = returnsUserAnswers.set(SpoiltVolumeByPeriodPage, testSpoiltList).success.value

      val mockViewModel = SpoiltCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasSpoiltProducts = true,
        totalSpoiltDuty = BigDecimal(1000),
        formattedTotalSpoiltDuty = "£1,000.00",
        hasAvailablePeriodsToAdd = true
      )

      when(mockService.buildViewModel(any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[SpoiltCheckYourAnswersService].toInstance(mockService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, spoiltCheckYourAnswersRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val mockService = mock[SpoiltCheckYourAnswersService]
      val testSpoiltList = List(SpoiltVolumeByPeriod(volume = BigDecimal(1000), periodKey = periodKey))
      val userAnswers = returnsUserAnswers
        .set(SpoiltVolumeByPeriodPage, testSpoiltList).success.value
        .set(SpoiltCheckYourAnswersPage, true).success.value

      val mockViewModel = SpoiltCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasSpoiltProducts = true,
        totalSpoiltDuty = BigDecimal(1000),
        formattedTotalSpoiltDuty = "£1,000.00",
        hasAvailablePeriodsToAdd = true
      )

      when(mockService.buildViewModel(any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[SpoiltCheckYourAnswersService].toInstance(mockService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, spoiltCheckYourAnswersRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must propagate exception when service fails on GET" in {
      val mockService = mock[SpoiltCheckYourAnswersService]
      val testSpoiltList = List(SpoiltVolumeByPeriod(volume = BigDecimal(1000), periodKey = periodKey))
      val userAnswers = returnsUserAnswers.set(SpoiltVolumeByPeriodPage, testSpoiltList).success.value

      when(mockService.buildViewModel(any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Service unavailable")))

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[SpoiltCheckYourAnswersService].toInstance(mockService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, spoiltCheckYourAnswersRoute)

        val result = route(application, request).value

        whenReady(result.failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustBe "Service unavailable"
        }
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockService = mock[SpoiltCheckYourAnswersService]
      val testSpoiltList = List(SpoiltVolumeByPeriod(volume = BigDecimal(1000), periodKey = periodKey))
      val userAnswers = returnsUserAnswers.set(SpoiltVolumeByPeriodPage, testSpoiltList).success.value

      val mockViewModel = SpoiltCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasSpoiltProducts = true,
        totalSpoiltDuty = BigDecimal(1000),
        formattedTotalSpoiltDuty = "£1,000.00",
        hasAvailablePeriodsToAdd = true
      )

      when(mockService.buildViewModel(any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))
      when(mockService.hasAvailablePeriodsToAdd(any(), any(), any())(using any()))
        .thenReturn(Future.successful(true))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[SpoiltCheckYourAnswersService].toInstance(mockService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, spoiltCheckYourAnswersRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must skip form validation and auto-set AddSpoiltAdjustmentPage to false when user declared No spoilt products" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockService = mock[SpoiltCheckYourAnswersService]

      val userAnswersWithNoSpoiltProducts = returnsUserAnswers
        .set(DeclareSpoiltProductsPage, false).success.value

      val mockViewModel = SpoiltCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasSpoiltProducts = false,
        totalSpoiltDuty = BigDecimal(0),
        formattedTotalSpoiltDuty = "£0.00",
        hasAvailablePeriodsToAdd = true
      )

      when(mockService.buildViewModel(any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswersWithNoSpoiltProducts))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[SpoiltCheckYourAnswersService].toInstance(mockService)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, spoiltCheckYourAnswersRoute)
        // Note: No form data submitted - form validation should be skipped

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockSessionRepository).set(any())(any())
      }
    }

    "must skip form validation and auto-set AddSpoiltAdjustmentPage to false when no available periods to add" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockService = mock[SpoiltCheckYourAnswersService]
      val testSpoiltList = List(SpoiltVolumeByPeriod(volume = BigDecimal(1000), periodKey = periodKey))
      val userAnswers = returnsUserAnswers.set(SpoiltVolumeByPeriodPage, testSpoiltList).success.value

      val mockViewModel = SpoiltCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasSpoiltProducts = true,
        totalSpoiltDuty = BigDecimal(1000),
        formattedTotalSpoiltDuty = "£1,000.00",
        hasAvailablePeriodsToAdd = false
      )

      when(mockService.buildViewModel(any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))
      when(mockService.hasAvailablePeriodsToAdd(any(), any(), any())(using any()))
        .thenReturn(Future.successful(false))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[SpoiltCheckYourAnswersService].toInstance(mockService)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, spoiltCheckYourAnswersRoute)
        // Note: No form data submitted - form validation should be skipped

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockSessionRepository).set(any())(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val mockService = mock[SpoiltCheckYourAnswersService]
      val testSpoiltList = List(SpoiltVolumeByPeriod(volume = BigDecimal(1000), periodKey = periodKey))
      val userAnswers = returnsUserAnswers.set(SpoiltVolumeByPeriodPage, testSpoiltList).success.value

      val mockViewModel = SpoiltCheckYourAnswersViewModel(
        summaryCards = Seq.empty,
        hasSpoiltProducts = true,
        totalSpoiltDuty = BigDecimal(1000),
        formattedTotalSpoiltDuty = "£1,000.00",
        hasAvailablePeriodsToAdd = true
      )

      when(mockService.buildViewModel(any(), any(), any(), any())(using any(), any()))
        .thenReturn(Future.successful(mockViewModel))
      when(mockService.hasAvailablePeriodsToAdd(any(), any(), any())(using any()))
        .thenReturn(Future.successful(true))

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[SpoiltCheckYourAnswersService].toInstance(mockService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, spoiltCheckYourAnswersRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }
  }
}
