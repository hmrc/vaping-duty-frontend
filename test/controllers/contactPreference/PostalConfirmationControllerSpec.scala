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
import connectors.SubmitPreferencesConnector
import models.emailverification.{ErrorModel, PaperlessPreferenceSubmittedResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.contactPreference.PostalConfirmationView

import java.time.Instant
import scala.concurrent.Future

class PostalConfirmationControllerSpec extends SpecBase {

  "PostalConfirmation Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockSubmitPreferencesConnector = mock[SubmitPreferencesConnector]
      val processingDate = Instant.now()

      when(mockAppConfig.continueToBta).thenReturn("http://localhost:9020/business-account")

      when(mockSubmitPreferencesConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Right(PaperlessPreferenceSubmittedResponse(processingDate, ""))))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SubmitPreferencesConnector].toInstance(mockSubmitPreferencesConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.contactPreference.routes.PostalConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PostalConfirmationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(mockAppConfig.continueToBta)(request, messages(application)).toString
      }
    }

    "must return OK and render the view when user is already on postal preference" in {
      // Test used to be:
      // must return SEE_OTHER and redirect to continue postal when user is already on postal preference
      
      val mockSubmitPreferencesConnector = mock[SubmitPreferencesConnector]

      when(mockAppConfig.continueToBta).thenReturn("http://localhost:9020/business-account")

      when(mockSubmitPreferencesConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, "There was a problem"))))

      val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail))
        .overrides(bind[SubmitPreferencesConnector].toInstance(mockSubmitPreferencesConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.contactPreference.routes.PostalConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PostalConfirmationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(mockAppConfig.continueToBta)(request, messages(application)).toString
      }
    }
  }
}
