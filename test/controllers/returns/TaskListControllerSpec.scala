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

package controllers.returns

import base.SpecBase
import controllers.routes
import models.enrolment.EnrolmentUserAnswers
import play.api.i18n.Messages
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.returns.TaskListViewModel
import views.html.TaskListView

import java.time.Instant

class TaskListControllerSpec extends SpecBase {

  "TaskList Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()
      given Messages = messages(application)

      running(application) {
        val request = FakeRequest(GET, controllers.returns.routes.TaskListController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[TaskListView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(TaskListViewModel.sections(returnsUserAnswers))(request).toString
      }
    }
  }
}