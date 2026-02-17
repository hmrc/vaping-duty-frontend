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

package controllers.contactPreference

import base.SpecBase
import forms.contactPreference.HowToBeContactedFormProvider
import models.NormalMode
import models.contactPreference.HowToBeContacted
import navigation.{FakeNavigator, Navigator}
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.contactPreference.HowToBeContactedPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.UserAnswersService
import uk.gov.hmrc.http.UpstreamErrorResponse
import viewmodels.contactPreference.HowToBeContactedViewModel
import views.html.contactPreference.HowToBeContactedView

import scala.concurrent.Future

class HowToBeContactedControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val howToBeContactedRoute = controllers.contactPreference.routes.HowToBeContactedController.onPageLoad(NormalMode).url

  val formProvider = new HowToBeContactedFormProvider()
  val form = formProvider()

  "HowToBeContacted Controller" - {

    "must return OK and the correct view for a GET for paperless preference" in {

      val mockUserAnswersService = mock[UserAnswersService]

      when(mockUserAnswersService.get(any())(any())).thenReturn(Future.successful(emptyUserAnswers))

      when(mockUserAnswersService.createUserAnswers(any())(any())).thenReturn(Future.successful(Right(emptyUserAnswers)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
        .build()

      running(application) {
        val request = FakeRequest(GET, howToBeContactedRoute)

        val result = route(application, request).value

        val vm = HowToBeContactedViewModel(emptyUserAnswers)(messages(application))

        val view = application.injector.instanceOf[HowToBeContactedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, vm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET for paper preference" in {

      val mockUserAnswersService = mock[UserAnswersService]

      when(mockUserAnswersService.get(any())(any())).thenReturn(Future.successful(userAnswersPostNoEmail))

      when(mockUserAnswersService.createUserAnswers(any())(any())).thenReturn(Future.successful(Right(userAnswersPostNoEmail)))

      val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail))
        .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
        .build()

      running(application) {
        val request = FakeRequest(GET, howToBeContactedRoute)

        val result = route(application, request).value

        val vm = HowToBeContactedViewModel(userAnswersPostNoEmail)(messages(application))

        val view = application.injector.instanceOf[HowToBeContactedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, vm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to JourneyRecoveryController when there is an error creating user answers" in {

      val mockUserAnswersService = mock[UserAnswersService]

      when(mockUserAnswersService.createUserAnswers(any())(any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("There was a problem", INTERNAL_SERVER_ERROR))))

      val application = applicationBuilder(userAnswers = None)
        .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
        .build()

      running(application) {
        val request = FakeRequest(GET, howToBeContactedRoute)

        val result = route(application, request).value
        
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val ua = userAnswers.set(HowToBeContactedPage, HowToBeContacted.values.head).success.value

      val mockUserAnswersService = mock[UserAnswersService]

      when(mockUserAnswersService.get(any())(any())).thenReturn(Future.successful(ua))

      when(mockUserAnswersService.createUserAnswers(any())(any())).thenReturn(Future.successful(Right(ua)))

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
        .build()

      running(application) {
        val request = FakeRequest(GET, howToBeContactedRoute)

        val vm = HowToBeContactedViewModel(emptyUserAnswers)(messages(application))

        val view = application.injector.instanceOf[HowToBeContactedView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(HowToBeContacted.values.head), vm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockUserAnswersService = mock[UserAnswersService]

      when(mockUserAnswersService.set(any())(any())).thenReturn(Future.successful(Right(HttpResponse())))
      
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, howToBeContactedRoute)
            .withFormUrlEncodedBody(("value", HowToBeContacted.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, howToBeContactedRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val vm = HowToBeContactedViewModel(emptyUserAnswers)(messages(application))

        val view = application.injector.instanceOf[HowToBeContactedView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, vm, NormalMode)(request, messages(application)).toString
      }
    }
  }
}
