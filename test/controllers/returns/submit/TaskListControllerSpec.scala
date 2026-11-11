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
import models.obligations.ObligationDetails
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{ObligationService, ReturnsUserAnswersService}
import utils.ReturnsDateUtils
import viewmodels.returns.submit.TaskListPageViewModel
import views.html.returns.submit.TaskListView

import scala.concurrent.Future

class TaskListControllerSpec extends SpecBase with MockitoSugar {

  "TaskList Controller" - {

    "must return OK and the correct view for a GET with fulfilled obligations" in {
      val mockObligationService = mock[ObligationService]
      val mockRepository = mock[ReturnsUserAnswersService]

      when(mockObligationService.getObligations(any())(using any()))
        .thenReturn(Future.successful(Seq(fulfilledObligation(june2026))))

      when(mockRepository.set(any())(using any()))
        .thenReturn(Future.successful(Right(HttpResponse(OK))))
      
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .overrides(bind[ReturnsUserAnswersService].toInstance(mockRepository))
        .build()
      
      given Messages = messages(application)

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.TaskListController.onPageLoad().url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[TaskListView]
        val returnsDateUtils = application.injector.instanceOf[ReturnsDateUtils]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          TaskListPageViewModel(returnsUserAnswers, Seq(fulfilledObligation(june2026)), periodKey, returnsDateUtils)
        )(request).toString
      }
    }
    
    "must return OK and the correct view for a GET with no fulfilled obligations" in {
      val mockObligationService = mock[ObligationService]
      val mockRepository = mock[ReturnsUserAnswersService]

      when(mockObligationService.getObligations(any())(using any()))
        .thenReturn(Future.successful(Seq(openObligation(june2026))))

      when(mockRepository.set(any())(using any()))
        .thenReturn(Future.successful(Right(HttpResponse(OK))))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .overrides(bind[ReturnsUserAnswersService].toInstance(mockRepository))
        .build()
      
      given Messages = messages(application)

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.TaskListController.onPageLoad().url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[TaskListView]
        val returnsDateUtils = application.injector.instanceOf[ReturnsDateUtils]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          TaskListPageViewModel(returnsUserAnswers, Seq(openObligation(june2026)), periodKey, returnsDateUtils)
        )(request).toString
      }
    }
  }
}
