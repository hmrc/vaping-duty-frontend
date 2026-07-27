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
import models.{CheckMode, NormalMode}
import models.returns.DutySuspenseVolumes
import pages.returns.{DeclareDutySuspensePage, EnterDutySuspensePage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.returns.submit.DutySuspenseCheckAnswersViewModel
import views.html.returns.submit.DutySuspenseCheckAnswersView

class DutySuspenseCheckAnswersControllerSpec extends SpecBase {

  private val dutySuspenseVolumes = DutySuspenseVolumes(volumeReceived = 1000, volumeMoved = 500)

  "DutySuspenseCheckAnswers Controller" - {
    "must return OK and the correct view for a GET in NormalMode" in {

      val ua = returnsUserAnswers
        .set(DeclareDutySuspensePage, true).success.value
        .set(EnterDutySuspensePage, dutySuspenseVolumes).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.DutySuspenseCheckAnswersController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DutySuspenseCheckAnswersView]
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey, NormalMode)(messages(application)).get

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, vm, NormalMode)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET in CheckMode" in {

      val ua = returnsUserAnswers
        .set(DeclareDutySuspensePage, true).success.value
        .set(EnterDutySuspensePage, dutySuspenseVolumes).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.DutySuspenseCheckAnswersController.onPageLoad(CheckMode).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DutySuspenseCheckAnswersView]
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey, CheckMode)(messages(application)).get

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, vm, CheckMode)(request, messages(application)).toString
      }
    }

    "must redirect to journey recovery when EnterDutySuspensePage is not answered" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.DutySuspenseCheckAnswersController.onPageLoad(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to TaskList for a POST in NormalMode" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.returns.submit.routes.DutySuspenseCheckAnswersController.onSubmit(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=${periodKey.value}"
      }
    }

    "must redirect to CheckYourAnswers for a POST in CheckMode" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.returns.submit.routes.DutySuspenseCheckAnswersController.onSubmit(CheckMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe s"${controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url}?period=${periodKey.value}"
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(returnsUserAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, controllers.returns.submit.routes.DutySuspenseCheckAnswersController.onSubmit(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}