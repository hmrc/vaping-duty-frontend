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
import forms.returns.EnterDutySuspenseFormProvider
import models.NormalMode
import models.identifiers.PeriodKey
import models.returns.{DutySuspenseVolumes, ReturnsUserAnswers}
import navigation.{ReturnsFakeNavigator, ReturnsNavigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.EnterDutySuspensePage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.ReturnsUserAnswersService
import views.html.returns.submit.EnterDutySuspenseView

import scala.concurrent.Future

class EnterDutySuspenseControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new EnterDutySuspenseFormProvider()
  val form: Form[DutySuspenseVolumes] = formProvider()

  lazy val enterDutySuspenseRoute: String = controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(NormalMode).url

  val validAnswer = DutySuspenseVolumes(volumeReceived = 1000, volumeMoved = 2000)

  "EnterDutySuspense Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, enterDutySuspenseRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EnterDutySuspenseView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(PeriodKey(periodKey), form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = returnsUserAnswers.set(EnterDutySuspensePage, validAnswer).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, enterDutySuspenseRoute)

        val view = application.injector.instanceOf[EnterDutySuspenseView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(PeriodKey(periodKey), form.fill(validAnswer), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[ReturnsUserAnswersService]

      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, enterDutySuspenseRoute)
            .withFormUrlEncodedBody(("volumeReceived", "1000"), ("volumeMoved", "2000"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted (empty volumeReceived)" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, enterDutySuspenseRoute)
            .withFormUrlEncodedBody(("volumeReceived", ""), ("volumeMoved", "2000"))

        val boundForm = form.bind(Map("volumeReceived" -> "", "volumeMoved" -> "2000"))

        val view = application.injector.instanceOf[EnterDutySuspenseView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(PeriodKey(periodKey), boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted (empty volumeMoved)" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, enterDutySuspenseRoute)
            .withFormUrlEncodedBody(("volumeReceived", "1000"), ("volumeMoved", ""))

        val boundForm = form.bind(Map("volumeReceived" -> "1000", "volumeMoved" -> ""))

        val view = application.injector.instanceOf[EnterDutySuspenseView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(PeriodKey(periodKey), boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted (both empty)" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, enterDutySuspenseRoute)
            .withFormUrlEncodedBody(("volumeReceived", ""), ("volumeMoved", ""))

        val boundForm = form.bind(Map("volumeReceived" -> "", "volumeMoved" -> ""))

        val view = application.injector.instanceOf[EnterDutySuspenseView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(PeriodKey(periodKey), boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when non-numeric data is submitted" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, enterDutySuspenseRoute)
            .withFormUrlEncodedBody(("volumeReceived", "abc"), ("volumeMoved", "xyz"))

        val boundForm = form.bind(Map("volumeReceived" -> "abc", "volumeMoved" -> "xyz"))

        val view = application.injector.instanceOf[EnterDutySuspenseView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(PeriodKey(periodKey), boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when values less than zero are submitted" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, enterDutySuspenseRoute)
            .withFormUrlEncodedBody(("volumeReceived", "-1"), ("volumeMoved", "-1"))

        val boundForm = form.bind(Map("volumeReceived" -> "-1", "volumeMoved" -> "-1"))

        val view = application.injector.instanceOf[EnterDutySuspenseView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(PeriodKey(periodKey), boundForm, NormalMode)(request, messages(application)).toString
      }
    }
  }
}
