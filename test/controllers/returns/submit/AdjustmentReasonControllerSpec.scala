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

package controllers.returns.submit

import base.SpecBase
import controllers.routes
import forms.returns.AdjustmentReasonFormProvider
import models.NormalMode
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.returns.submit.AdjustmentReasonView

class AdjustmentReasonControllerSpec extends SpecBase {

  val formProvider = new AdjustmentReasonFormProvider()
  val form: Form[String] = formProvider()

  val validAnswer = "This is a valid reason for adjustment"

  lazy val adjustmentReasonRoute: String = controllers.returns.submit.routes.AdjustmentReasonController.onPageLoad(NormalMode).url

  "AdjustmentReason Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, adjustmentReasonRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AdjustmentReasonView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, form, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to task list when valid data is submitted" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentReasonRoute)
            .withFormUrlEncodedBody(("adjustmentReason", validAnswer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.returns.submit.routes.TaskListController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentReasonRoute)
            .withFormUrlEncodedBody(("adjustmentReason", ""))

        val boundForm = form.bind(Map("adjustmentReason" -> ""))

        val view = application.injector.instanceOf[AdjustmentReasonView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(periodKey, boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return a Bad Request and errors when data exceeds max length" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val tooLongString = "a" * 251

        val request =
          FakeRequest(POST, adjustmentReasonRoute)
            .withFormUrlEncodedBody(("adjustmentReason", tooLongString))

        val boundForm = form.bind(Map("adjustmentReason" -> tooLongString))

        val view = application.injector.instanceOf[AdjustmentReasonView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(periodKey, boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(returnsUserAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, adjustmentReasonRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(returnsUserAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, adjustmentReasonRoute)
            .withFormUrlEncodedBody(("adjustmentReason", validAnswer))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
