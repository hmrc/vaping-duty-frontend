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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.ObligationService

import scala.concurrent.Future

class SelectAdjustmentPeriodControllerSpec extends SpecBase with MockitoSugar {

  lazy val selectAdjustmentPeriodRoute: String =
    controllers.returns.submit.adjustments.routes.SelectAdjustmentPeriodController.onPageLoad(NormalMode, None).url

  "SelectAdjustmentPeriod Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockObligationService = mock[ObligationService]
      val obligationDetails = obligations(Seq(
        fulfilledObligation(october2027),
        fulfilledObligation(december2027)
      )).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, selectAdjustmentPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must return OK and the correct view for a GET with year filter" in {
      val mockObligationService = mock[ObligationService]
      val obligationDetails = obligations(Seq(
        fulfilledObligation(october2027),
        fulfilledObligation(december2027)
      )).map(_.obligationDetails)

      when(mockObligationService.getObligationsDirectly(any())(using any()))
        .thenReturn(Future.successful(obligationDetails))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.adjustments.routes.SelectAdjustmentPeriodController.onPageLoad(NormalMode, Some(2027)).url)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Journey Recovery when obligation service fails" in {
      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getObligationsDirectly(any())(using any()))
         .thenReturn(Future.failed(new RuntimeException("Service unavailable")))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, selectAdjustmentPeriodRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
