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

package controllers.returns.submit

import base.SpecBase
import connectors.returns.GetReturnsConnector
import models.obligations.ObligationDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.ObligationService
import viewmodels.returns.submit.ConfirmationViewModel
import views.html.returns.submit.ConfirmationEmailView

import scala.concurrent.Future

class ConfirmationControllerSpec extends SpecBase {

  private def createObligation(): ObligationDetails = fulfilledObligation(periodKey)

  "ConfirmationController" - {

    "must return OK and the correct view for a GET with positive duty" in {

      val mockGetReturnsConnector = mock[GetReturnsConnector]
      val mockObligationService = mock[ObligationService]

      val application = applicationBuilder(returnsUserAnswers = Option(returnsUserAnswers))
        .overrides(bind[GetReturnsConnector].toInstance(mockGetReturnsConnector))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {

        val returnsResponse = createReturnDisplayResponse()
        val obligation = createObligation()

        when(mockGetReturnsConnector.getReturn(any(), any())(using any()))
          .thenReturn(Future.successful(returnsResponse))

        when(mockObligationService.getObligationByPeriodKey(any(), any())(using any()))
          .thenReturn(Future.successful(Some(obligation)))

        val request = FakeRequest(GET, s"${controllers.returns.submit.routes.ConfirmationController.onPageLoad().url}?period=$periodKey")

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmationEmailView]

        val vm = ConfirmationViewModel(returnsResponse, obligation, btaLink)(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET with nil return" in {

      val mockGetReturnsConnector = mock[GetReturnsConnector]
      val mockObligationService = mock[ObligationService]

      val application = applicationBuilder(returnsUserAnswers = Option(returnsUserAnswers))
        .overrides(bind[GetReturnsConnector].toInstance(mockGetReturnsConnector))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {

        val nilReturnResponse = createReturnDisplayResponse().copy(
          success = createReturnDisplayResponse().success.copy(
            totalDutyDue = Some(createReturnDisplayResponse().success.totalDutyDue.get.copy(totalDue = BigDecimal(0)))
          )
        )
        val obligation = createObligation()

        when(mockGetReturnsConnector.getReturn(any(), any())(using any()))
          .thenReturn(Future.successful(nilReturnResponse))

        when(mockObligationService.getObligationByPeriodKey(any(), any())(using any()))
          .thenReturn(Future.successful(Some(obligation)))

        val request = FakeRequest(GET, s"${controllers.returns.submit.routes.ConfirmationController.onPageLoad().url}?period=$periodKey")

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmationEmailView]

        val vm = ConfirmationViewModel(nilReturnResponse, obligation, btaLink)(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }

    "must redirect to journey recovery when totalDutyDue is None" in {

      val mockGetReturnsConnector = mock[GetReturnsConnector]
      val mockObligationService = mock[ObligationService]

      val application = applicationBuilder(returnsUserAnswers = Option(returnsUserAnswers))
        .overrides(bind[GetReturnsConnector].toInstance(mockGetReturnsConnector))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {

        val responseWithoutDuty = createReturnDisplayResponse().copy(
          success = createReturnDisplayResponse().success.copy(totalDutyDue = None)
        )
        val obligation = createObligation()

        when(mockGetReturnsConnector.getReturn(any(), any())(using any()))
          .thenReturn(Future.successful(responseWithoutDuty))

        when(mockObligationService.getObligationByPeriodKey(any(), any())(using any()))
          .thenReturn(Future.successful(Some(obligation)))

        val request = FakeRequest(GET, controllers.returns.submit.routes.ConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to journey recovery when obligation is not found" in {

      val mockGetReturnsConnector = mock[GetReturnsConnector]
      val mockObligationService = mock[ObligationService]

      val application = applicationBuilder(returnsUserAnswers = Option(returnsUserAnswers))
        .overrides(bind[GetReturnsConnector].toInstance(mockGetReturnsConnector))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {

        val returnsResponse = createReturnDisplayResponse()

        when(mockGetReturnsConnector.getReturn(any(), any())(using any()))
          .thenReturn(Future.successful(returnsResponse))

        when(mockObligationService.getObligationByPeriodKey(any(), any())(using any()))
          .thenReturn(Future.successful(None))

        val request = FakeRequest(GET, controllers.returns.submit.routes.ConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
