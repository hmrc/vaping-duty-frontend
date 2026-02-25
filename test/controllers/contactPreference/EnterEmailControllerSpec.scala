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
import cats.data.EitherT
import connectors.EmailVerificationConnector
import controllers.routes
import forms.contactPreference.EnterEmailFormProvider
import models.NormalMode
import models.emailverification.{EmailVerificationDetails, ErrorModel, RedirectUri}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.contactPreference.EnterEmailPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{EmailVerificationService, UserAnswersService}
import uk.gov.hmrc.http.HttpResponse
import views.html.contactPreference.EnterEmailView

import scala.concurrent.Future

class EnterEmailControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new EnterEmailFormProvider()
  val form = formProvider()

  lazy val enterEmailRoute = controllers.contactPreference.routes.EnterEmailController.onPageLoad(NormalMode).url

  "EnterEmail Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockUserAnswersService = mock[UserAnswersService]

      when(mockUserAnswersService.get(any())(any())).thenReturn(Future.successful(emptyUserAnswers))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
        .build()

      running(application) {
        val request = FakeRequest(GET, enterEmailRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EnterEmailView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val ua = userAnswers.copy(emailAddress = Some("answer"))

      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, enterEmailRoute)

        val view = application.injector.instanceOf[EnterEmailView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockUserAnswersService = mock[UserAnswersService]

      when(mockEmailVerificationConnector.getEmailVerification(any())(any()))
        .thenReturn(EitherT.rightT[Future, ErrorModel](testGetVerificationStatusResponse))

      when(mockEmailVerificationConnector.startEmailVerification(any())(any())).thenReturn(Future.successful(Right(RedirectUri("/redirectUri"))))

      when(mockUserAnswersService.set(any())(any())).thenReturn(Future.successful(Right(HttpResponse(BAD_REQUEST, "Bad request"))))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
          .overrides(bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute, mockAppConfig)),
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, enterEmailRoute)
            .withFormUrlEncodedBody(("value", emailAddress))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value contains "/redirectUri" mustBe true
      }
    }

    "must redirect to journey recovery when receiving an error starting email verification" in {

      val mockUserAnswersService = mock[UserAnswersService]

      when(mockEmailVerificationConnector.startEmailVerification(any())(any()))
        .thenReturn(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, "There was a problem"))))

      when(mockUserAnswersService.set(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK, "Okay"))))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
          .overrides(bind[EmailVerificationConnector].toInstance(mockEmailVerificationConnector))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute, mockAppConfig)),
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, enterEmailRoute)
            .withFormUrlEncodedBody(("value", emailAddress))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to journey recovery if failing to set user answers" in {

      val mockUserAnswersService = mock[UserAnswersService]
      val mockEmailVerificationService = mock[EmailVerificationService]

      when(mockEmailVerificationService.retrieveAddressStatus(any(), any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, ErrorModel](EmailVerificationDetails(emailAddress2, true, false)))

      when(mockEmailVerificationService.redirectIfLocked(any(), any()))
        .thenReturn(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))

      when(mockUserAnswersService.set(any())(any()))
        .thenReturn(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, "There was a problem"))))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute, mockAppConfig)),
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, enterEmailRoute)
            .withFormUrlEncodedBody(("value", emailAddress))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to journey recovery if failing to retrieve verified emails" in {

      val mockUserAnswersService = mock[UserAnswersService]
      val mockEmailVerificationService = mock[EmailVerificationService]

      when(mockEmailVerificationService.retrieveAddressStatus(any(), any(), any())(any()))
        .thenReturn(EitherT.leftT[Future, EmailVerificationDetails](ErrorModel(INTERNAL_SERVER_ERROR, "There was a problem")))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute, mockAppConfig)),
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, enterEmailRoute)
            .withFormUrlEncodedBody(("value", emailAddress))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to email confirmation(CYA when implemented) page when address entered is already verified" in {

      val mockUserAnswersService = mock[UserAnswersService]
      val mockEmailVerificationService = mock[EmailVerificationService]

      when(mockEmailVerificationService.retrieveAddressStatus(any(), any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, ErrorModel](EmailVerificationDetails(emailAddress2, true, false)))

      when(mockEmailVerificationService.redirectIfLocked(any(), any()))
        .thenReturn(Future.successful(Redirect(new FakeNavigator(onwardRoute, mockAppConfig).nextPage(EnterEmailPage, NormalMode, userAnswers))))

      when(mockUserAnswersService.set(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK, "Okay"))))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute, mockAppConfig)),
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, enterEmailRoute)
            .withFormUrlEncodedBody(("value", emailAddress2))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe onwardRoute.url
      }
    }

    "must redirect to locked email page when attempting to verify a locked email" in {

      val mockUserAnswersService = mock[UserAnswersService]
      val mockEmailVerificationService = mock[EmailVerificationService]

      when(mockEmailVerificationService.retrieveAddressStatus(any(), any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, ErrorModel](EmailVerificationDetails(emailAddress2, false, true)))

      when(mockEmailVerificationService.redirectIfLocked(any(), any()))
        .thenReturn(Future.successful(Redirect(controllers.contactPreference.routes.LockedEmailController.onPageLoad())))

      when(mockUserAnswersService.set(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK, "Okay"))))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
          .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute, mockAppConfig)),
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, enterEmailRoute)
            .withFormUrlEncodedBody(("value", emailAddress2))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.contactPreference.routes.LockedEmailController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, enterEmailRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[EnterEmailView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, enterEmailRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, enterEmailRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
