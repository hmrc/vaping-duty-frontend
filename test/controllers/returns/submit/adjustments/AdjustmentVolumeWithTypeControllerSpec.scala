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
import forms.returns.adjustments.{AdjustmentVolumeWithTypeFormData, AdjustmentVolumeWithTypeFormProvider}
import models.NormalMode
import models.obligations.ObligationsResponse
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import navigation.{ReturnsFakeNavigator, ReturnsNavigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.adjustments.AdjustmentListPage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{ObligationService, ReturnsUserAnswersService}

import scala.concurrent.Future

class AdjustmentVolumeWithTypeControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new AdjustmentVolumeWithTypeFormProvider()
  val form: Form[AdjustmentVolumeWithTypeFormData] = formProvider()

  val adjustmentPeriodKey = october2027

  lazy val adjustmentVolumeWithTypeRoute: String =
    controllers.returns.submit.adjustments.routes.AdjustmentVolumeWithTypeController
      .onPageLoad(NormalMode).url + s"?adjustmentPeriod=${adjustmentPeriodKey.value}"

  lazy val adjustmentVolumeWithTypeSubmitRoute: String =
    controllers.returns.submit.adjustments.routes.AdjustmentVolumeWithTypeController
      .onSubmit(NormalMode).url + s"?period=${periodKey.value}&adjustmentPeriod=${adjustmentPeriodKey.value}"

  "AdjustmentVolumeWithType Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockObligationService = mock[ObligationService]
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))))

      when(mockObligationService.getObligations(any())(using any()))
        .thenReturn(Future.successful(obligationsResponse))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, adjustmentVolumeWithTypeRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must populate the view correctly on a GET when editing an existing adjustment" in {
      val mockObligationService = mock[ObligationService]
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))))

      when(mockObligationService.getObligations(any())(using any()))
        .thenReturn(Future.successful(obligationsResponse))

      // Test-specific adjustment with different period (october2027) and volume (100.5) than TestData
      val existingAdjustment = AdjustmentEntry(
        period = adjustmentPeriodKey,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal("100.5")
      )
      val adjustmentList = AdjustmentList(Seq(existingAdjustment))
      val userAnswers = returnsUserAnswers.set(AdjustmentListPage, adjustmentList).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
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
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))))

      when(mockObligationService.getObligations(any())(using any()))
        .thenReturn(Future.successful(obligationsResponse))
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[ObligationService].toInstance(mockObligationService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(
              ("adjustmentType", "underDeclared"),
              ("underDeclaredVolume", "100.50")
            )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val mockObligationService = mock[ObligationService]
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(fulfilledObligation(adjustmentPeriodKey))))

      when(mockObligationService.getObligations(any())(using any()))
        .thenReturn(Future.successful(obligationsResponse))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentVolumeWithTypeSubmitRoute)
            .withFormUrlEncodedBody(("adjustmentType", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
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
  }
}
