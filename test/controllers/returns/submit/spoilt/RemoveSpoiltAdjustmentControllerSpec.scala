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
import models.NormalMode
import models.identifiers.PeriodKey
import models.returns.{DutyRate, ReturnsUserAnswers, SpoiltVolumeByPeriod}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.{DeclareSpoiltProductsPage, SpoiltVolumeByPeriodPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{DutyRateService, ObligationService, ReturnsUserAnswersService}

import scala.concurrent.Future

class RemoveSpoiltAdjustmentControllerSpec extends SpecBase with MockitoSugar {

  val spoiltPeriodKey: PeriodKey = october2027
  val otherSpoiltPeriodKey: PeriodKey = november2027

  lazy val removeSpoiltAdjustmentRoute: String =
    controllers.returns.submit.spoilt.routes.RemoveSpoiltAdjustmentController.onPageLoad(NormalMode).url + s"?spoiltPeriod=${spoiltPeriodKey.value}"

  lazy val removeSpoiltAdjustmentSubmitRoute: String =
    controllers.returns.submit.spoilt.routes.RemoveSpoiltAdjustmentController.onSubmit(NormalMode).url + s"?spoiltPeriod=${spoiltPeriodKey.value}"

  private def stubbedObligationService: ObligationService = {
    val mockObligationService = mock[ObligationService]
    when(mockObligationService.getObligationsDirectly(any())(using any()))
      .thenReturn(Future.successful(Seq(fulfilledObligation(spoiltPeriodKey), fulfilledObligation(otherSpoiltPeriodKey))))
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

  "RemoveSpoiltAdjustment Controller" - {

    "must return OK and the correct view for a GET" in {
      val userAnswers = returnsUserAnswers
        .set(SpoiltVolumeByPeriodPage, List(SpoiltVolumeByPeriod(BigDecimal(1000), spoiltPeriodKey)))
        .success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[ObligationService].toInstance(stubbedObligationService),
          bind[DutyRateService].toInstance(stubbedDutyRateService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, removeSpoiltAdjustmentRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Journey Recovery for a GET when no spoiltPeriod is supplied" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.spoilt.routes.RemoveSpoiltAdjustmentController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET when the spoiltPeriod does not match any entry" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[ObligationService].toInstance(stubbedObligationService),
          bind[DutyRateService].toInstance(stubbedDutyRateService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, removeSpoiltAdjustmentRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the summary page when confirmed removal leaves other entries" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val userAnswers = returnsUserAnswers
        .set(SpoiltVolumeByPeriodPage, List(
          SpoiltVolumeByPeriod(BigDecimal(1000), spoiltPeriodKey),
          SpoiltVolumeByPeriod(BigDecimal(500), otherSpoiltPeriodKey)
        ))
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
          FakeRequest(POST, removeSpoiltAdjustmentSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(
          controllers.returns.submit.spoilt.routes.SpoiltCheckYourAnswersController.onPageLoad(NormalMode).url
        )
        verify(mockSessionRepository).set(any())(any())
      }
    }

    "must remove the entry and redirect on a confirmed submission even when no obligation exists for the period" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val userAnswers = returnsUserAnswers
        .set(SpoiltVolumeByPeriodPage, List(
          SpoiltVolumeByPeriod(BigDecimal(1000), spoiltPeriodKey),
          SpoiltVolumeByPeriod(BigDecimal(500), otherSpoiltPeriodKey)
        ))
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
          FakeRequest(POST, removeSpoiltAdjustmentSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(
          controllers.returns.submit.spoilt.routes.SpoiltCheckYourAnswersController.onPageLoad(NormalMode).url
        )
        verify(mockSessionRepository).set(any())(any())
      }
    }

    "must redirect to the summary page and clear the declaration when confirmed removal leaves no entries" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val userAnswers = returnsUserAnswers
        .set(SpoiltVolumeByPeriodPage, List(SpoiltVolumeByPeriod(BigDecimal(1000), spoiltPeriodKey)))
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
          FakeRequest(POST, removeSpoiltAdjustmentSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(
          controllers.returns.submit.spoilt.routes.SpoiltCheckYourAnswersController.onPageLoad(NormalMode).url
        )

        val captor = ArgumentCaptor.forClass(classOf[ReturnsUserAnswers])
        verify(mockSessionRepository).set(captor.capture())(any())
        captor.getValue.get(DeclareSpoiltProductsPage) mustBe Some(false)
        captor.getValue.get(SpoiltVolumeByPeriodPage) mustBe None
      }
    }

    "must redirect to the summary page without removing anything when the user cancels" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]

      val userAnswers = returnsUserAnswers
        .set(SpoiltVolumeByPeriodPage, List(SpoiltVolumeByPeriod(BigDecimal(1000), spoiltPeriodKey)))
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
          FakeRequest(POST, removeSpoiltAdjustmentSubmitRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(
          controllers.returns.submit.spoilt.routes.SpoiltCheckYourAnswersController.onPageLoad(NormalMode).url
        )
        verify(mockSessionRepository, never).set(any())(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val userAnswers = returnsUserAnswers
        .set(SpoiltVolumeByPeriodPage, List(SpoiltVolumeByPeriod(BigDecimal(1000), spoiltPeriodKey)))
        .success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[ObligationService].toInstance(stubbedObligationService),
          bind[DutyRateService].toInstance(stubbedDutyRateService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, removeSpoiltAdjustmentSubmitRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must redirect to Journey Recovery for a POST when no spoiltPeriod is supplied" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.returns.submit.spoilt.routes.RemoveSpoiltAdjustmentController.onSubmit(NormalMode).url)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(returnsUserAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeSpoiltAdjustmentRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
