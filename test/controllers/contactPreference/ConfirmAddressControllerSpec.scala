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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.UserAnswersService
import views.html.contactPreference.ConfirmAddressView

import scala.concurrent.Future

class ConfirmAddressControllerSpec extends SpecBase {

  "ConfirmAddress Controller" - {

    "must return OK and the correct view for a GET" in {
      val mockUserAnswersService = mock[UserAnswersService]

      when(mockUserAnswersService.get(any())(any())).thenReturn(Future.successful(userAnswersPostNoEmail))

      val application = applicationBuilder(userAnswers = Some(userAnswersPostNoEmail))
        .overrides(bind[UserAnswersService].toInstance(mockUserAnswersService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.ConfirmAddressController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmAddressView]

        val expectedAddress = userAnswersPostNoEmail.subscriptionSummary.correspondenceAddress.split("\n").toList

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(expectedAddress)(request, messages(application)).toString
      }
    }
  }
}
