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
import forms.returns.SpoiltVolumeByPeriodFormProvider
import models.identifiers.PeriodKey
import models.obligations.{ObligationDetails, ObligationStatus}
import models.returns.SpoiltVolumeByPeriod
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.SpoiltVolumeByPeriodPage
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{ObligationService, ReturnsUserAnswersService}
import uk.gov.hmrc.http.InternalServerException
import viewmodels.returns.submit.SpoiltVolumeByPeriodViewModel
import views.html.returns.submit.spoilt.SpoiltVolumeByPeriodView

import java.time.LocalDate
import scala.concurrent.Future

class SpoiltVolumeByPeriodControllerSpec extends SpecBase with MockitoSugar {

  val spoiltPeriodKey = PeriodKey("24AA")
  val testVolume = BigDecimal(1000)

  private val mockFormProvider = mock[SpoiltVolumeByPeriodFormProvider]
  private val testForm: Form[BigDecimal] = Form("value" -> play.api.data.Forms.bigDecimal)

  val mockObligation: ObligationDetails = ObligationDetails(
    openOrFulfilledStatus = ObligationStatus.F.toString,
    iCFromDate = LocalDate.of(2024, 1, 1),
    iCToDate = LocalDate.of(2024, 1, 31),
    iCDateReceived = Some(LocalDate.now()),
    iCDueDate = LocalDate.now().plusDays(10),
    periodKey = spoiltPeriodKey.value
  )

  lazy val spoiltVolumeByPeriodRoute: String =
    controllers.returns.submit.spoilt.routes.SpoiltVolumeByPeriodController.onPageLoad().url + s"?spoiltPeriod=${spoiltPeriodKey.value}"

  lazy val spoiltVolumeByPeriodSubmitRoute: String =
    controllers.returns.submit.spoilt.routes.SpoiltVolumeByPeriodController.onSubmit().url + s"?spoiltPeriod=${spoiltPeriodKey.value}"

  "SpoiltVolumeByPeriodController" - {

    "must return OK and the correct view for a GET" in {
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(spoiltPeriodKey))(using any()))
        .thenReturn(Future.successful(Some(mockObligation)))
      when(mockFormProvider.apply(any(), any())(any(), any()))
        .thenReturn(Future.successful(testForm))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[SpoiltVolumeByPeriodFormProvider].toInstance(mockFormProvider),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, spoiltVolumeByPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(spoiltPeriodKey))(using any()))
        .thenReturn(Future.successful(Some(mockObligation)))
      when(mockFormProvider.apply(any(), any())(any(), any()))
        .thenReturn(Future.successful(testForm))

      val userAnswers = returnsUserAnswers.set(
        SpoiltVolumeByPeriodPage,
        List(SpoiltVolumeByPeriod(testVolume, spoiltPeriodKey))
      ).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[SpoiltVolumeByPeriodFormProvider].toInstance(mockFormProvider),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, spoiltVolumeByPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Journey Recovery when spoiltPeriod query parameter is missing on a GET" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[SpoiltVolumeByPeriodFormProvider].toInstance(mockFormProvider))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.spoilt.routes.SpoiltVolumeByPeriodController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when obligation is not found on a GET" in {
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(spoiltPeriodKey))(using any()))
        .thenReturn(Future.successful(None))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[SpoiltVolumeByPeriodFormProvider].toInstance(mockFormProvider),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, spoiltVolumeByPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when obligation service fails on a GET" in {
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(spoiltPeriodKey))(using any()))
        .thenReturn(Future.failed(InternalServerException("Service error")))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[SpoiltVolumeByPeriodFormProvider].toInstance(mockFormProvider),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, spoiltVolumeByPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(returnsUserAnswers = None)
        .overrides(bind[SpoiltVolumeByPeriodFormProvider].toInstance(mockFormProvider))
        .build()

      running(application) {
        val request = FakeRequest(GET, spoiltVolumeByPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }


    "must redirect to AddSpoiltAdjustment page when valid data is submitted on a POST" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(spoiltPeriodKey))(using any()))
        .thenReturn(Future.successful(Some(mockObligation)))
      when(mockFormProvider.apply(any(), any())(any(), any()))
        .thenReturn(Future.successful(testForm))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[SpoiltVolumeByPeriodFormProvider].toInstance(mockFormProvider),
          bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, spoiltVolumeByPeriodSubmitRoute)
          .withFormUrlEncodedBody(("value", testVolume.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(
          controllers.returns.submit.spoilt.routes.AddSpoiltAdjustmentController.onPageLoad(models.NormalMode).url
        )
        redirectLocation(result).value must include(s"period=${periodKey.value}")
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(spoiltPeriodKey))(using any()))
        .thenReturn(Future.successful(Some(mockObligation)))
      when(mockFormProvider.apply(any(), any())(any(), any()))
        .thenReturn(Future.successful(testForm))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[SpoiltVolumeByPeriodFormProvider].toInstance(mockFormProvider),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, spoiltVolumeByPeriodSubmitRoute)
          .withFormUrlEncodedBody(("value", "invalid"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must redirect to Journey Recovery when spoiltPeriod query parameter is missing on a POST" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[SpoiltVolumeByPeriodFormProvider].toInstance(mockFormProvider))
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.returns.submit.spoilt.routes.SpoiltVolumeByPeriodController.onSubmit().url)
          .withFormUrlEncodedBody(("value", testVolume.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when obligation is not found on a POST" in {
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(spoiltPeriodKey))(using any()))
        .thenReturn(Future.successful(None))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[SpoiltVolumeByPeriodFormProvider].toInstance(mockFormProvider),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, spoiltVolumeByPeriodSubmitRoute)
          .withFormUrlEncodedBody(("value", testVolume.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when obligation service fails on a POST" in {
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(spoiltPeriodKey))(using any()))
        .thenReturn(Future.failed(InternalServerException("Service error")))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[SpoiltVolumeByPeriodFormProvider].toInstance(mockFormProvider),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, spoiltVolumeByPeriodSubmitRoute)
          .withFormUrlEncodedBody(("value", testVolume.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(returnsUserAnswers = None)
        .overrides(bind[SpoiltVolumeByPeriodFormProvider].toInstance(mockFormProvider))
        .build()

      running(application) {
        val request = FakeRequest(POST, spoiltVolumeByPeriodSubmitRoute)
          .withFormUrlEncodedBody(("value", testVolume.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}