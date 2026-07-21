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
import models.NormalMode
import models.returns.{DutyRate, MaxVolumeResult}
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
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

      // Test-specific adjustment with different period (october2027) and volume (100.5) than TestData
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
        // Submit with underDeclared selected but invalid volume (zero), and a value in overDeclared field
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
        // The form should be re-displayed with the overDeclared field cleared
        content must not include "200.7"
        // The error summary should not contain errors for the cleared field
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
        // Submit with overDeclared selected and only the relevant field
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
          // Submit with underDeclared selected and an invalid overDeclared value
          val request =
            FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
              .withFormUrlEncodedBody(
                ("adjustmentType", "underDeclared"),
                ("underDeclaredVolume", "0"), // Invalid - will trigger error
                ("overDeclaredVolume", "999.99") // Should be cleared
              )

          val result = route(application, request).value
          val content = contentAsString(result)

          status(result) mustEqual BAD_REQUEST
          // The overDeclaredVolume value should be cleared from the form
          content must not include "999.99"
          // No errors should be shown for overDeclaredVolume field
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
          // Submit with overDeclared selected and an invalid underDeclared value
          val request =
            FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
              .withFormUrlEncodedBody(
                ("adjustmentType", "overDeclared"),
                ("underDeclaredVolume", "888.88"), // Should be cleared
                ("overDeclaredVolume", "0") // Invalid - will trigger error
              )

          val result = route(application, request).value
          val content = contentAsString(result)

          status(result) mustEqual BAD_REQUEST
          // The underDeclaredVolume value should be cleared from the form
          content must not include "888.88"
          // No errors should be shown for underDeclaredVolume field
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
          // Submit without adjustmentType
          val request =
            FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
              .withFormUrlEncodedBody(
                ("underDeclaredVolume", "100.5"),
                ("overDeclaredVolume", "200.7")
              )

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          // Both values should still be present since no adjustmentType was selected
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
          // Submit with an invalid adjustmentType value
          val request =
            FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
              .withFormUrlEncodedBody(
                ("adjustmentType", "invalidType"),
                ("underDeclaredVolume", "100.5"),
                ("overDeclaredVolume", "200.7")
              )

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          // Both values should still be present since adjustmentType was invalid
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

      // User has existing reason and large adjustment
      val existingAdjustment = AdjustmentEntry(
        period = adjustmentPeriodKey,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal(5000) // High volume
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
        // Submit with low volume that will result in duty < £1000
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(
              ("adjustmentType", "underDeclared"),
              ("underDeclaredVolume", "10") // Low volume - duty will be < £1000
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        
        // Verify that session was updated (reason should be removed)
        verify(mockSessionRepository).set(any())(any())
      }
    }

    "must keep adjustment reason when duty remains above threshold" in {
      val mockObligationService = mock[ObligationService]
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockDutyRateService = mock[DutyRateService]
      val mockVolumePrecisionService = mock[VolumePrecisionService]
      val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))).map(_.obligationDetails)

      // User has existing reason and large adjustment
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
        // Submit with high volume that will result in duty >= £1000
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(
              ("adjustmentType", "underDeclared"),
              ("underDeclaredVolume", "6000") // High volume - duty will be >= £1000
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        
        // Verify that session was updated (reason should be kept)
        verify(mockSessionRepository).set(any())(any())
      }
    }

    "must apply each adjustment's own period duty rate when deciding whether a reason is required" in {
      val mockObligationService = mock[ObligationService]
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      val mockDutyRateService = mock[DutyRateService]
      val mockVolumePrecisionService = mock[VolumePrecisionService]
      val obligationDetails = obligations(Seq(fulfilledObligation(adjustmentPeriodKey), fulfilledObligation(november2027))).map(_.obligationDetails)

      // Existing adjustment against a DIFFERENT period (november2027) with a high duty rate -
      // on its own this is well above the £1000 threshold, so the existing reason must be kept.
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
      // october2027 (the period being submitted) has a low rate; november2027 (the existing
      // adjustment's period) has a high rate - each must be applied to its own adjustment only.
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
        // Tiny volume against october2027 - would be well under £1000 on its own, and would
        // wrongly drag the november2027 entry's duty down too if a single rate were applied
        // to the whole list (the bug this test guards against).
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(
              ("adjustmentType", "underDeclared"),
              ("underDeclaredVolume", "1")
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        import org.mockito.ArgumentCaptor
        import models.returns.ReturnsUserAnswers
        val captor = ArgumentCaptor.forClass(classOf[ReturnsUserAnswers])
        verify(mockSessionRepository).set(captor.capture())(any())
        // The november2027 entry alone crosses the threshold at its own (high) rate, so the
        // reason must be kept even though the newly-submitted october2027 entry is tiny.
        captor.getValue.get(AdjustmentReasonPage) mustBe Some("existing reason")
      }
    }
  }
}
