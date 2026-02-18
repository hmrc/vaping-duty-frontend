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
import models.emailverification.ErrorModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.UserAnswersService
import viewmodels.contactPreference.ConfirmAddressViewModel
import views.html.contactPreference.ConfirmAddressView

import scala.concurrent.Future

class ConfirmAddressControllerSpec extends SpecBase {

  "ConfirmAddress Controller" - {

    "must return OK and the correct view for a GET" in {
      
      when(mockAppConfig.changeAddressGuidanceUrl)
        .thenReturn("https://www.gov.uk/find-hmrc-contacts/excise-warehousing-excise-goods-movements-and-alcohol-duties-enquiries")

      val vm = ConfirmAddressViewModel(mockAppConfig, userAnswersPostNoEmail.subscriptionSummary.correspondenceAddress)

      val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ConfirmAddressController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }

    "must return SEE_OTHER when there is an issue during submission" in {

      val mockSubmitPreferencesConnector = mock[SubmitPreferencesConnector]

      when(mockSubmitPreferencesConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, "There was a problem"))))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SubmitPreferencesConnector].toInstance(mockSubmitPreferencesConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.ConfirmAddressController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return SEE_OTHER and redirect to change address page" in {

      val mockSubmitPreferencesConnector = mock[SubmitPreferencesConnector]

      when(mockSubmitPreferencesConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Right(testSubmissionResponse)))

      val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail))
        .overrides(bind[SubmitPreferencesConnector].toInstance(mockSubmitPreferencesConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.ConfirmAddressController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.contactPreference.routes.ChangeAddressController.onPageLoad().url
      }
    }

    "must return SEE_OTHER and redirect to confirmation page" in {

      val mockSubmitPreferencesConnector = mock[SubmitPreferencesConnector]

      when(mockSubmitPreferencesConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Right(testSubmissionResponse)))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SubmitPreferencesConnector].toInstance(mockSubmitPreferencesConnector))
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.ConfirmAddressController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.contactPreference.routes.PostalConfirmationController.onPageLoad().url
      }
    }
  }
}
