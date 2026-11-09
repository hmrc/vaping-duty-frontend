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

package controllers.returns.submit.yourdetails

import base.SpecBase
import pages.returns.yourdetails.{YourBusinessEmailPage, YourFullNamePage, YourRolePage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.returns.submit.YourDetailsCheckAnswersViewModel
import views.html.returns.submit.yourdetails.YourDetailsCheckAnswersView

class YourDetailsCheckAnswersControllerSpec extends SpecBase {

  private val completedAnswers = returnsUserAnswers
    .set(YourFullNamePage, "John Smith").success.value
    .set(YourRolePage, "Director").success.value
    .set(YourBusinessEmailPage, "john@example.com").success.value

  lazy val checkAnswersRoute: String = routes.YourDetailsCheckAnswersController.onPageLoad().url

  "YourDetailsCheckAnswersController" - {

    "must return OK and the correct view for a GET when all questions are answered" in {
      val application = applicationBuilder(returnsUserAnswers = Some(completedAnswers), yourDetailsEnabled = true).build()

      running(application) {
        val request = FakeRequest(GET, checkAnswersRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[YourDetailsCheckAnswersView]
        val summaryList = YourDetailsCheckAnswersViewModel(completedAnswers, periodKey)(messages(application)).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, summaryList)(request, messages(application)).toString
      }
    }

    "must show each answer with a change link to its check mode page" in {
      val application = applicationBuilder(returnsUserAnswers = Some(completedAnswers), yourDetailsEnabled = true).build()

      running(application) {
        val request = FakeRequest(GET, checkAnswersRoute)

        val content = contentAsString(route(application, request).value)

        content must include("John Smith")
        content must include("Director")
        content must include("john@example.com")
        content must include(routes.YourFullNameController.onPageLoad(models.CheckMode).url)
        content must include(routes.YourRoleController.onPageLoad(models.CheckMode).url)
        content must include(routes.YourBusinessEmailController.onPageLoad(models.CheckMode).url)
      }
    }

    "must redirect to return submission recovery when the task is not complete" in {
      val incompleteAnswers = returnsUserAnswers.set(YourFullNamePage, "John Smith").success.value

      val application = applicationBuilder(returnsUserAnswers = Some(incompleteAnswers), yourDetailsEnabled = true).build()

      running(application) {
        val request = FakeRequest(GET, checkAnswersRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe s"${controllers.returns.submit.routes.ReturnSubmissionRecoveryController.onPageLoad().url}?period=${periodKey.value}"
      }
    }

    "must redirect to the task list for a POST" in {
      val application = applicationBuilder(returnsUserAnswers = Some(completedAnswers), yourDetailsEnabled = true).build()

      running(application) {
        val request = FakeRequest(POST, routes.YourDetailsCheckAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=${periodKey.value}"
      }
    }

    "must redirect to Journey Recovery when the your details feature is disabled" in {
      val application = applicationBuilder(returnsUserAnswers = Some(completedAnswers), yourDetailsEnabled = false).build()

      running(application) {
        val request = FakeRequest(GET, checkAnswersRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
