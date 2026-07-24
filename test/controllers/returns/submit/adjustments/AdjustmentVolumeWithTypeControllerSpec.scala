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
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import models.returns.{DutyRate, MaxVolumeResult}
import models.{CheckMode, NormalMode}
import navigation.{ReturnsFakeNavigator, ReturnsNavigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.adjustments.{AdjustmentListPage, AdjustmentReasonPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{DutyRateService, ObligationService, ReturnsUserAnswersService, VolumePrecisionService}

import scala.concurrent.Future

class AdjustmentVolumeWithTypeControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val adjustmentPeriodKey = october2027
  
  override val testDutyRate: DutyRate = DutyRate(3370)
  val testMaxVolume: BigDecimal = BigDecimal("29000000000")
  val testFormattedMax: String = "29,000,000,000 ml"

  lazy val adjustmentVolumeWithTypeRoute: String =
    controllers.returns.submit.adjustments.routes.AdjustmentVolumeWithTypeController
      .onPageLoad(NormalMode).url + s"?adjustmentPeriod=${adjustmentPeriodKey.value}"

  lazy val adjustmentVolumeWithTypeSubmitRoute: String =
    controllers.returns.submit.adjustments.routes.AdjustmentVolumeWithTypeController
      .onSubmit(NormalMode).url + s"?period=${periodKey.value}&adjustmentPeriod=${adjustmentPeriodKey.value}"

  private def setupFormProviderMocks(mockDutyRateService: DutyRateService, 
                                     mockVolumePrecisionService: VolumePrecisionService): Unit = {
    when(mockDutyRateService.getDutyRate(eqTo(vpdId), eqTo(periodKey))(using any(), any()))
      .thenReturn(Future.successful(testDutyRate))
    when(mockDutyRateService.getDutyRate(eqTo(vpdId), eqTo(adjustmentPeriodKey))(using any(), any()))
      .thenReturn(Future.successful(testDutyRate))
    when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
      .thenReturn(Map(adjustmentPeriodKey -> testDutyRate))
    when(mockVolumePrecisionService.calculateMaxVolume(any()))
      .thenReturn(MaxVolumeResult(testMaxVolume, testFormattedMax))
  }

  "AdjustmentVolumeWithType Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockObligationService = mock[ObligationService]
      val mockDutyRateService = mock[DutyRateService]
      val mockVolumePrecisionService = mock[VolumePrecisionService]
      val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[ObligationService].toInstance(mockObligationService),
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, adjustmentVolumeWithTypeRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must populate the view correctly on a GET when editing an existing adjustment" in {
      val mockObligationService = mock[ObligationService]
      val mockDutyRateService = mock[DutyRateService]
      val mockVolumePrecisionService = mock[VolumePrecisionService]
      val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)

      val existingAdjustment = AdjustmentEntry(
        period = adjustmentPeriodKey,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal("100.5")
      )
      val adjustmentList = AdjustmentList(Seq(existingAdjustment))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, adjustmentList).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(
          bind[ObligationService].toInstance(mockObligationService),
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, adjustmentVolumeWithTypeRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val mockObligationService = mock[ObligationService]
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockDutyRateService = mock[DutyRateService]
      val mockVolumePrecisionService = mock[VolumePrecisionService]
      val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))
      setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)

      val application =
        applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[ObligationService].toInstance(mockObligationService),
            bind[DutyRateService].toInstance(mockDutyRateService),
            bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(
              ("adjustmentType", "underDeclared"),
              ("underDeclaredVolume", "100.5")
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when valid underDeclared data is submitted" in {
      val mockObligationService = mock[ObligationService]
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockDutyRateService = mock[DutyRateService]
      val mockVolumePrecisionService = mock[VolumePrecisionService]
      val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))
      setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)

      val application =
        applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[ObligationService].toInstance(mockObligationService),
            bind[DutyRateService].toInstance(mockDutyRateService),
            bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(
              ("adjustmentType", "underDeclared"),
              ("underDeclaredVolume", "100.5")
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when valid overDeclared data is submitted" in {
      val mockObligationService = mock[ObligationService]
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockDutyRateService = mock[DutyRateService]
      val mockVolumePrecisionService = mock[VolumePrecisionService]
      val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))
      setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)

      val application =
        applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[ObligationService].toInstance(mockObligationService),
            bind[DutyRateService].toInstance(mockDutyRateService),
            bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(
              ("adjustmentType", "overDeclared"),
              ("overDeclaredVolume", "200.7")
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val mockObligationService = mock[ObligationService]
      val mockDutyRateService = mock[DutyRateService]
      val mockVolumePrecisionService = mock[VolumePrecisionService]
      val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[ObligationService].toInstance(mockObligationService),
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(("adjustmentType", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must clear the non-selected field and its errors when form has errors" in {
      val mockObligationService = mock[ObligationService]
      val mockDutyRateService = mock[DutyRateService]
      val mockVolumePrecisionService = mock[VolumePrecisionService]
      val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[ObligationService].toInstance(mockObligationService),
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(
              ("adjustmentType", "underDeclared"),
              ("underDeclaredVolume", "0"),
              ("overDeclaredVolume", "200.7")
            )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        val content = contentAsString(result)

        content must not include "200.7"
        content must not include "overDeclaredVolume-error"
      }
    }

    "must redirect to the next page when valid overDeclared data is submitted without underDeclared field" in {
      val mockObligationService = mock[ObligationService]
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockDutyRateService = mock[DutyRateService]
      val mockVolumePrecisionService = mock[VolumePrecisionService]
      val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))
      setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
          bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
          bind[ObligationService].toInstance(mockObligationService),
          bind[DutyRateService].toInstance(mockDutyRateService),
          bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(
              ("adjustmentType", "overDeclared"),
              ("overDeclaredVolume", "200.7")
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Journey Recovery when no adjustment period is provided" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.adjustments.routes.AdjustmentVolumeWithTypeController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "clearNonSelectedField" - {
      "must clear overDeclaredVolume field and its errors when adjustmentType is underDeclared" in {
        val mockObligationService = mock[ObligationService]
        val mockDutyRateService = mock[DutyRateService]
        val mockVolumePrecisionService = mock[VolumePrecisionService]
        val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

        when(mockObligationService.getObligationsDirectly(any())(using any()))
          .thenReturn(Future.successful(obligationDetails))
        setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)

        val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ObligationService].toInstance(mockObligationService),
            bind[DutyRateService].toInstance(mockDutyRateService),
            bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
              .withFormUrlEncodedBody(
                ("adjustmentType", "underDeclared"),
                ("underDeclaredVolume", "0"),
                ("overDeclaredVolume", "999.99")
              )

          val result = route(application, request).value
          val content = contentAsString(result)

          status(result) mustEqual BAD_REQUEST
          content must not include "999.99"
          content must not include "overDeclaredVolume-error"
        }
      }

      "must clear underDeclaredVolume field and its errors when adjustmentType is overDeclared" in {
        val mockObligationService = mock[ObligationService]
        val mockDutyRateService = mock[DutyRateService]
        val mockVolumePrecisionService = mock[VolumePrecisionService]
        val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

        when(mockObligationService.getObligationsDirectly(any())(using any()))
          .thenReturn(Future.successful(obligationDetails))
        setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)

        val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ObligationService].toInstance(mockObligationService),
            bind[DutyRateService].toInstance(mockDutyRateService),
            bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
              .withFormUrlEncodedBody(
                ("adjustmentType", "overDeclared"),
                ("underDeclaredVolume", "888.88"),
                ("overDeclaredVolume", "0")
              )

          val result = route(application, request).value
          val content = contentAsString(result)

          status(result) mustEqual BAD_REQUEST
          content must not include "888.88"
          content must not include "underDeclaredVolume-error"
        }
      }

      "must return form unchanged when adjustmentType is not present" in {
        val mockObligationService = mock[ObligationService]
        val mockDutyRateService = mock[DutyRateService]
        val mockVolumePrecisionService = mock[VolumePrecisionService]
        val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

        when(mockObligationService.getObligationsDirectly(any())(using any()))
          .thenReturn(Future.successful(obligationDetails))
        setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)

        val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ObligationService].toInstance(mockObligationService),
            bind[DutyRateService].toInstance(mockDutyRateService),
            bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
              .withFormUrlEncodedBody(
                ("underDeclaredVolume", "100.5"),
                ("overDeclaredVolume", "200.7")
              )

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST

          val content = contentAsString(result)
          content must include("100.5")
          content must include("200.7")
        }
      }

      "must return form unchanged when adjustmentType is invalid" in {
        val mockObligationService = mock[ObligationService]
        val mockDutyRateService = mock[DutyRateService]
        val mockVolumePrecisionService = mock[VolumePrecisionService]
        val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

        when(mockObligationService.getObligationsDirectly(any())(using any()))
          .thenReturn(Future.successful(obligationDetails))
        setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)

        val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ObligationService].toInstance(mockObligationService),
            bind[DutyRateService].toInstance(mockDutyRateService),
            bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
          )
          .build()

        running(application) {
          val request =
            FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
              .withFormUrlEncodedBody(
                ("adjustmentType", "invalidType"),
                ("underDeclaredVolume", "100.5"),
                ("overDeclaredVolume", "200.7")
              )

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST

          val content = contentAsString(result)
          content must include("100.5")
          content must include("200.7")
        }
      }
    }

    "must remove adjustment reason when duty drops below threshold" in {
      val mockObligationService = mock[ObligationService]
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockDutyRateService = mock[DutyRateService]
      val mockVolumePrecisionService = mock[VolumePrecisionService]
      val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

      val existingAdjustment = AdjustmentEntry(
        period = adjustmentPeriodKey,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal(5000)
      )
      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(existingAdjustment))).success.value
        .set(AdjustmentReasonPage, "existing reason").success.value

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))
      setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)
      when(mockDutyRateService.getDutyRate(any(), any())(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[ObligationService].toInstance(mockObligationService),
            bind[DutyRateService].toInstance(mockDutyRateService),
            bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(
              ("adjustmentType", "underDeclared"),
              ("underDeclaredVolume", "10")
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        
        verify(mockSessionRepository).set(any())(any())
      }
    }

    "must keep adjustment reason when duty remains above threshold" in {
      val mockObligationService = mock[ObligationService]
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockDutyRateService = mock[DutyRateService]
      val mockVolumePrecisionService = mock[VolumePrecisionService]
      val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

      val existingAdjustment = AdjustmentEntry(
        period = adjustmentPeriodKey,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal(5000)
      )
      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(existingAdjustment))).success.value
        .set(AdjustmentReasonPage, "existing reason").success.value

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))
      setupFormProviderMocks(mockDutyRateService, mockVolumePrecisionService)
      when(mockDutyRateService.getDutyRate(any(), any())(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[ObligationService].toInstance(mockObligationService),
            bind[DutyRateService].toInstance(mockDutyRateService),
            bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(
              ("adjustmentType", "underDeclared"),
              ("underDeclaredVolume", "6000")
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        
        verify(mockSessionRepository).set(any())(any())
      }
    }

    "must apply each adjustment's own period duty rate when deciding whether a reason is required" in {
      val mockObligationService = mock[ObligationService]
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockDutyRateService = mock[DutyRateService]
      val mockVolumePrecisionService = mock[VolumePrecisionService]
      val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey), fulfilledObligation(november2027))).map(_.obligationDetails)
      
      val existingAdjustment = AdjustmentEntry(
        period = november2027,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal(5000)
      )
      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(existingAdjustment))).success.value
        .set(AdjustmentReasonPage, "existing reason").success.value

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))
      when(mockDutyRateService.getDutyRate(eqTo(vpdId), eqTo(periodKey))(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))
      when(mockVolumePrecisionService.calculateMaxVolume(any()))
        .thenReturn(MaxVolumeResult(testMaxVolume, testFormattedMax))
      when(mockDutyRateService.getDutyRatesForPeriods(any(), any()))
        .thenReturn(Map(adjustmentPeriodKey -> DutyRate(100), november2027 -> testDutyRate))

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[ObligationService].toInstance(mockObligationService),
            bind[DutyRateService].toInstance(mockDutyRateService),
            bind[VolumePrecisionService].toInstance(mockVolumePrecisionService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(
              ("adjustmentType", "underDeclared"),
              ("underDeclaredVolume", "1")
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        import models.returns.ReturnsUserAnswers
        import org.mockito.ArgumentCaptor
        val captor = ArgumentCaptor.forClass(classOf[ReturnsUserAnswers])
        verify(mockSessionRepository).set(captor.capture())(any())
        captor.getValue.get(AdjustmentReasonPage) mustBe Some("existing reason")
      }
    }
  }
}
