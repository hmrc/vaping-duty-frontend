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
import connectors.SubmitPreferencesConnector
import models.emailverification.{EmailVerificationDetails, ErrorModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.mvc.Results.{Ok, Redirect}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{EmailVerificationService, UserAnswersService}
import uk.gov.hmrc.http.HttpResponse
import views.html.contactPreference.SubmitEmailView

import java.time.Instant
import scala.concurrent.Future

class SubmitEmailControllerSpec extends SpecBase {

  "SubmitEmail Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockUserAnswersService = mock[UserAnswersService]
      val mockEmailVerificationService = mock[EmailVerificationService]

      when(mockUserAnswersService.get(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK, "Okay"))))

      when(mockEmailVerificationService.retrieveAddressStatus(any(), any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, ErrorModel](EmailVerificationDetails(emailAddress, true, true)))

      val view = mock[SubmitEmailView](emailAddress).toString

      when(mockEmailVerificationService.redirectIfLocked(any(), any()))
        .thenReturn(Future.successful(Ok(view)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers.copy(emailAddress = Some(emailAddress))))
        .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
        .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmitEmailController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) contains emailAddress mustEqual true
      }
    }

    "must return SEE_OTHER and redirect to locked email controller" in {

      val mockUserAnswersService = mock[UserAnswersService]
      val mockEmailVerificationService = mock[EmailVerificationService]

      when(mockUserAnswersService.get(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK, "Okay"))))

      when(mockEmailVerificationService.retrieveAddressStatus(any(), any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, ErrorModel](EmailVerificationDetails(emailAddress, false, true)))

      when(mockEmailVerificationService.redirectIfLocked(any(), any()))
        .thenReturn(Future.successful(Redirect(controllers.contactPreference.routes.LockedEmailController.onPageLoad())))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers.copy(emailAddress = Some(emailAddress))))
        .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
        .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmitEmailController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.contactPreference.routes.LockedEmailController.onPageLoad().url
      }
    }

    "must redirect to journey recovery when email verification service fails on GET" in {

      val mockUserAnswersService = mock[UserAnswersService]
      val mockEmailVerificationService = mock[EmailVerificationService]

      when(mockUserAnswersService.get(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK, "Okay"))))

      when(mockEmailVerificationService.retrieveAddressStatus(any(), any(), any())(any()))
        .thenReturn(EitherT.leftT[Future, EmailVerificationDetails](ErrorModel(INTERNAL_SERVER_ERROR, "There was a problem")))

      val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail))
        .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
        .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmitEmailController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to journey recovery when email verification service fails on POST" in {

      val mockUserAnswersService = mock[UserAnswersService]
      val mockEmailVerificationService = mock[EmailVerificationService]

      when(mockUserAnswersService.get(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK, "Okay"))))

      when(mockEmailVerificationService.retrieveAddressStatus(any(), any(), any())(any()))
        .thenReturn(EitherT.leftT[Future, EmailVerificationDetails](ErrorModel(INTERNAL_SERVER_ERROR, "There was a problem")))

      val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail))
        .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
        .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.SubmitEmailController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to journey recovery when submitting preferences fails" in {

      val mockUserAnswersService = mock[UserAnswersService]
      val mockEmailVerificationService = mock[EmailVerificationService]

      when(mockUserAnswersService.get(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK, "Okay"))))

      when(mockEmailVerificationService.retrieveAddressStatus(any(), any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, ErrorModel](EmailVerificationDetails(emailAddress, true, true)))

      when(mockEmailVerificationService.submitVerifiedEmail(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))

      val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail))
        .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
        .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.SubmitEmailController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to journey recovery when attempting to submit unverified email" in {
      // Should not be a scenario that executes in the application, covered for safety.
      val mockUserAnswersService = mock[UserAnswersService]
      val mockEmailVerificationService = mock[EmailVerificationService]
      val mockSubmitPreferencesConnector = mock[SubmitPreferencesConnector]

      when(mockUserAnswersService.get(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK, "Okay"))))

      when(mockEmailVerificationService.retrieveAddressStatus(any(), any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, ErrorModel](EmailVerificationDetails(emailAddress, false, false)))

      when(mockEmailVerificationService.submitVerifiedEmail(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))

      val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail))
        .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
        .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
        .overrides(bind[SubmitPreferencesConnector].toInstance(mockSubmitPreferencesConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.SubmitEmailController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
