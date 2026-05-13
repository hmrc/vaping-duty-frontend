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
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.ReturnsUserAnswersService
import viewmodels.returns.submit.BeforeYouStartViewModel
import views.html.returns.submit.BeforeYouStartView

import scala.concurrent.Future

class BeforeYouStartControllerSpec extends SpecBase {

  "BeforeYouStart Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockService = mock[ReturnsUserAnswersService]

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ReturnsUserAnswersService].to(mockService))
        .build()

      val vm = BeforeYouStartViewModel()(messages(application))

      when(mockService.set(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK))))

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.BeforeYouStartController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BeforeYouStartView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }

    "must redirect when returns journey is disabled" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers), returnsEnabled = false).build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.BeforeYouStartController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
