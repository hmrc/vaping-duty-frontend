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

package controllers.returns.submit.yourdetails

import base.SpecBase
import forms.returns.yourdetails.YourFullNameFormProvider
import models.{CheckMode, NormalMode}
import navigation.{ReturnsFakeNavigator, ReturnsNavigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.yourdetails.YourFullNamePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.ReturnsUserAnswersService
import views.html.returns.submit.yourdetails.YourFullNameView

import scala.concurrent.Future

class YourFullNameControllerSpec extends SpecBase with MockitoSugar {

  private val form = new YourFullNameFormProvider()()

  def onwardRoute = Call("GET", "/foo")

  private val validAnswer = "John Smith"

  lazy val normalRoute: String = routes.YourFullNameController.onPageLoad(NormalMode).url
  lazy val checkRoute: String = routes.YourFullNameController.onPageLoad(CheckMode).url

  "YourFullNameController" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, normalRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourFullNameView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = returnsUserAnswers.set(YourFullNamePage, validAnswer).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, checkRoute)

        val view = application.injector.instanceOf[YourFullNameView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, form.fill(validAnswer), CheckMode)(request, messages(application)).toString
      }
    }

    "must save the answer and redirect to the next page when valid data is submitted" in {
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
          FakeRequest(POST, normalRoute)
            .withFormUrlEncodedBody(("fullName", validAnswer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, normalRoute)
            .withFormUrlEncodedBody(("fullName", ""))

        val boundForm = form.bind(Map("fullName" -> ""))

        val view = application.injector.instanceOf[YourFullNameView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(periodKey, boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(returnsUserAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, normalRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
