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

package controllers.returns.view

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.ObligationService
import uk.gov.hmrc.http.InternalServerException
import viewmodels.returns.view.ViewMultipleReturnsViewModel
import views.html.returns.view.ViewMultipleReturnsView

import java.time.temporal.TemporalField
import scala.concurrent.Future

class ViewMultipleReturnsControllerSpec extends SpecBase {

  "ViewMultipleReturns Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockService = mock[ObligationService]

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].to(mockService))
        .build()

      when(mockService.getObligations(any())(using any())).thenReturn(Future.successful(createMockObligationsResponse()))

      val vm = ViewMultipleReturnsViewModel(createMockObligationsResponse(), 2027)(messages(application), clock)

      running(application) {
        val request = FakeRequest(GET, controllers.returns.view.routes.ViewMultipleReturnsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewMultipleReturnsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET with an empty obligation response" in {
      val mockService = mock[ObligationService]

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].to(mockService))
        .build()

      val emptyObligationResponse = createMockObligationsResponse().copy(obligation = Seq.empty)

        when(mockService.getObligations(any())(using any())).thenReturn(
        Future.successful(emptyObligationResponse)
      )

      val vm = ViewMultipleReturnsViewModel(emptyObligationResponse, 2026)(messages(application), clock)

      running(application) {
        val request = FakeRequest(GET, controllers.returns.view.routes.ViewMultipleReturnsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewMultipleReturnsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }

    "must redirect when obligation service request fails" in {
      val mockService = mock[ObligationService]

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].to(mockService))
        .build()

      when(mockService.getObligations(any())(using any())).thenReturn(Future.failed(InternalServerException("")))

      running(application) {
        val request = FakeRequest(GET, controllers.returns.view.routes.ViewMultipleReturnsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect when returns journey is disabled" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers), returnsEnabled = false).build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.view.routes.ViewMultipleReturnsController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}