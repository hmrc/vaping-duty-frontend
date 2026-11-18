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
import forms.contactPreference.EnterEmailFormProvider
import models.NormalMode
import models.contactPreference.EnterEmailOutcome
import models.emailverification.ErrorModel
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.contactPreference.EnterEmailService
import views.html.contactPreference.EnterEmailView

import scala.concurrent.Future

class EnterEmailControllerSpec extends SpecBase with MockitoSugar {

  private val ONWARD_URL = "/foo"
  private val VERIFICATION_URL = "/email-verification/redirect"
  private val ERROR_MESSAGE = "There was a problem"
  private val INVALID_FIELD_KEY = "value"
  private val EMPTY_VALUE = ""

  private val onwardRoute: Call = Call(GET, ONWARD_URL)

  private val formProvider = new EnterEmailFormProvider()
  private val form: Form[String] = formProvider()

  private lazy val enterEmailRoute = controllers.contactPreference.routes.EnterEmailController.onPageLoad(NormalMode).url
  private lazy val lockedEmailRoute = controllers.contactPreference.routes.LockedEmailController.onPageLoad().url
  private lazy val journeyRecoveryRoute = controllers.routes.JourneyRecoveryController.onPageLoad().url

  private def applicationWith(mockEnterEmailService: EnterEmailService): Application =
    applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .overrides(
        bind[EnterEmailService].toInstance(mockEnterEmailService),
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute, mockAppConfig))
      )
      .build()

  private def stubbedService(result: Either[ErrorModel, EnterEmailOutcome]): EnterEmailService = {
    val mockEnterEmailService: EnterEmailService = mock[EnterEmailService]
    when(mockEnterEmailService.submitEmail(any(), any(), any())(using any(), any()))
      .thenReturn(Future.successful(result))
    mockEnterEmailService
  }

  private def validPost = FakeRequest(POST, enterEmailRoute).withFormUrlEncodedBody((formProvider.EMAIL, emailAddress))

  "EnterEmail Controller" - {

    "onPageLoad" - {

      "must return OK and the correct view for a GET" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, enterEmailRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[EnterEmailView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val application = applicationBuilder(userAnswers = Some(userAnswers.copy(emailAddress = Some(emailAddress)))).build()

        running(application) {
          val request = FakeRequest(GET, enterEmailRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[EnterEmailView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(emailAddress), NormalMode)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, enterEmailRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual journeyRecoveryRoute
        }
      }
    }

    "onSubmit" - {

      "must submit the entered email and redirect to the next page when the email is already verified" in {
        val mockEnterEmailService = stubbedService(Right(EnterEmailOutcome.Verified(emptyUserAnswers)))
        val application = applicationWith(mockEnterEmailService)

        running(application) {
          val result = route(application, validPost).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual ONWARD_URL
          verify(mockEnterEmailService).submitEmail(eqTo(emailAddress), eqTo(emptyUserAnswers), eqTo(credId))(using any(), any())
        }
      }

      "must redirect to the locked email page when the email is locked" in {
        val application = applicationWith(stubbedService(Right(EnterEmailOutcome.Locked)))

        running(application) {
          val result = route(application, validPost).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual lockedEmailRoute
        }
      }

      "must redirect to email verification when verification has been started" in {
        val application = applicationWith(stubbedService(Right(EnterEmailOutcome.VerificationStarted(VERIFICATION_URL))))

        running(application) {
          val result = route(application, validPost).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual VERIFICATION_URL
        }
      }

      "must redirect to Journey Recovery when the service returns an error" in {
        val application = applicationWith(stubbedService(Left(ErrorModel(INTERNAL_SERVER_ERROR, ERROR_MESSAGE))))

        running(application) {
          val result = route(application, validPost).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual journeyRecoveryRoute
        }
      }

      "must return a Bad Request and errors without calling the service when invalid data is submitted" in {
        val mockEnterEmailService: EnterEmailService = mock[EnterEmailService]
        val application = applicationWith(mockEnterEmailService)

        running(application) {
          val request = FakeRequest(POST, enterEmailRoute).withFormUrlEncodedBody((INVALID_FIELD_KEY, EMPTY_VALUE))

          val boundForm = form.bind(Map(INVALID_FIELD_KEY -> EMPTY_VALUE))

          val view = application.injector.instanceOf[EnterEmailView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
          verify(mockEnterEmailService, never()).submitEmail(any(), any(), any())(using any(), any())
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val result = route(application, validPost).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual journeyRecoveryRoute
        }
      }
    }
  }
}
