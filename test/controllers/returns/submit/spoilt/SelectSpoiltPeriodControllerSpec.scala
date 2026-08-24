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
import models.obligations.ObligationDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.ObligationService
import uk.gov.hmrc.http.InternalServerException
import viewmodels.returns.submit.SelectSpoiltPeriodViewModel
import views.html.returns.submit.spoilt.SelectSpoiltPeriodView

import scala.concurrent.Future

class SelectSpoiltPeriodControllerSpec extends SpecBase {

  private val obligationsSpanning3Years: Seq[ObligationDetails] = {
    Seq(fulfilledObligation(november2027), fulfilledObligation(october2026), fulfilledObligation(september2025))
  }

  "SelectSpoiltPeriodController" - {

    "must return OK and the correct view when no year parameter is provided" in {
      val mockService = mock[ObligationService]

      when(mockService.getObligations(any())(using any())).thenReturn(Future.successful(obligationsSpanning3Years))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].to(mockService))
        .build()

      running(application) {
        val request = FakeRequest(
          GET, controllers.returns.submit.spoilt.routes.SelectSpoiltPeriodController.onPageLoad(None, NormalMode).url
        )

        val result = route(application, request).value

        val returnsDateUtils = application.injector.instanceOf[utils.ReturnsDateUtils]

        val vm = SelectSpoiltPeriodViewModel(
          obligationsSpanning3Years,
          None,
          periodKey,
          None,
          returnsDateUtils,
          NormalMode
        )(messages(application))

        val view = application.injector.instanceOf[SelectSpoiltPeriodView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view when a specific year is provided" in {
      val mockService = mock[ObligationService]
      val specificYear = 2026

      when(mockService.getObligations(any())(using any())).thenReturn(Future.successful(obligationsSpanning3Years))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].to(mockService))
        .build()

      running(application) {
        val request = FakeRequest(
          GET, controllers.returns.submit.spoilt.routes.SelectSpoiltPeriodController.onPageLoad(Some(specificYear), NormalMode).url
        )

        val result = route(application, request).value

        val returnsDateUtils = application.injector.instanceOf[utils.ReturnsDateUtils]

        val vm = SelectSpoiltPeriodViewModel(
          obligationsSpanning3Years,
          Some(specificYear),
          periodKey,
          None,
          returnsDateUtils,
          NormalMode
        )(messages(application))

        val view = application.injector.instanceOf[SelectSpoiltPeriodView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }

    "must exclude periods that already have spoilt data entered" in {
      val mockService = mock[ObligationService]
      val alreadyDeclaredPeriod = PeriodKey("26AJ")

      when(mockService.getObligations(any())(using any())).thenReturn(Future.successful(obligationsSpanning3Years))

      val userAnswers = returnsUserAnswers
        .set(pages.returns.SpoiltVolumeByPeriodPage, List(models.returns.SpoiltVolumeByPeriod(BigDecimal(100), alreadyDeclaredPeriod)))
        .success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(bind[ObligationService].to(mockService))
        .build()

      running(application) {
        val request = FakeRequest(
          GET, controllers.returns.submit.spoilt.routes.SelectSpoiltPeriodController.onPageLoad(None, NormalMode).url
        )

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must not include alreadyDeclaredPeriod.value
      }
    }

    "must redirect to JourneyRecovery when the service fails" in {
      val mockService = mock[ObligationService]

      when(mockService.getObligations(any())(using any())).thenReturn(Future.failed(InternalServerException("")))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].to(mockService))
        .build()

      running(application) {
        val request = FakeRequest(
          GET, controllers.returns.submit.spoilt.routes.SelectSpoiltPeriodController.onPageLoad(None, NormalMode).url
        )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery when returns journey is disabled" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers), returnsEnabled = false).build()

      running(application) {
        val request = FakeRequest(
          GET, controllers.returns.submit.spoilt.routes.SelectSpoiltPeriodController.onPageLoad(None, NormalMode).url
        )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
