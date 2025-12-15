/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.enrolment

import base.SpecBase
import forms.enrolment.UserHasApprovalIdFormProvider
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.enrolment.UserHasApprovalIdView

class UserHasApprovalIdControllerSpec extends SpecBase with MockitoSugar {

  val formProvider        = new UserHasApprovalIdFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val UserHasApprovalIdRoute: String = controllers.enrolment.routes.UserHasApprovalIdController.onPageLoad().url

  "UserHasApprovalId Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, UserHasApprovalIdRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UserHasApprovalIdView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must redirect to EACD if user has approval id " in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, UserHasApprovalIdRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        // @todo update with real config value once EACD redirection URL received (VPD-1156)
        redirectLocation(result).value mustEqual "#"
      }
    }

    "must redirect to guidance page if user does not have approval id " in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, UserHasApprovalIdRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.enrolment.routes.UserDoesNotHaveApprovalIdController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, UserHasApprovalIdRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[UserHasApprovalIdView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm)(request, messages(application)).toString
      }
    }

  }
}
