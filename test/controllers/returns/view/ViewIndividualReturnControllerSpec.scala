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

package controllers.returns.view

import base.SpecBase
import connectors.returns.GetReturnsConnector
import controllers.returns
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.returns.view.ViewIndividualReturnViewModel
import views.html.returns.view.ViewIndividualReturnView

import scala.concurrent.Future

class ViewIndividualReturnControllerSpec extends SpecBase {

  "ViewIndividualReturn Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockConnector = mock[GetReturnsConnector]

      when(mockConnector.getReturn(any(), any())(using any())).thenReturn(Future.successful(
        createReturnDisplayResponse()
      ))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(inject.bind[GetReturnsConnector].to(mockConnector))
        .build()

      running(application) {
        val request = FakeRequest(GET, returns.view.routes.ViewIndividualReturnController.onPageLoad(periodKey).url)

        val result = route(application, request).value

        val vm = ViewIndividualReturnViewModel(createReturnDisplayResponse())

        val view = application.injector.instanceOf[ViewIndividualReturnView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }
  }
}
