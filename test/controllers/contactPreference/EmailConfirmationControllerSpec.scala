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
import models.emailverification.{EmailVerificationDetails, ErrorModel, PaperlessPreferenceSubmittedResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{EmailVerificationService, UserAnswersService}
import uk.gov.hmrc.http.HttpResponse
import views.html.contactPreference.EmailConfirmationView

import java.time.Instant
import scala.concurrent.Future

class EmailConfirmationControllerSpec extends SpecBase {

  "EmailConfirmation Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockUserAnswersService = mock[UserAnswersService]
      val mockEmailVerificationService = mock[EmailVerificationService]
      val mockSubmitPreferencesConnector = mock[SubmitPreferencesConnector]

      when(mockAppConfig.continueToBta).thenReturn("http://localhost:9020/business-account")

      when(mockUserAnswersService.get(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK, "Okay"))))

      when(mockEmailVerificationService.retrieveAddressStatus(any(), any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, ErrorModel](EmailVerificationDetails(emailAddress, true, true)))

      when(mockSubmitPreferencesConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Right(PaperlessPreferenceSubmittedResponse(Instant.now(), "formBundleNumber"))))

      val application = applicationBuilder(userAnswers = Some(userAnswersPostWithEmail))
        .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
        .overrides(bind[EmailVerificationService].toInstance(mockEmailVerificationService))
        .overrides(bind[SubmitPreferencesConnector].toInstance(mockSubmitPreferencesConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.contactPreference.routes.EmailConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EmailConfirmationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(emailAddress, mockAppConfig.continueToBta)(request, messages(application)).toString
      }
    }
  }
}
