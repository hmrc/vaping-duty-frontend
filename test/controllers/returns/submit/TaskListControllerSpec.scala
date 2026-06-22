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
import models.obligations.{ObligationDetails, ObligationItem, ObligationStatus, ObligationsResponse}
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{ObligationService, ReturnsUserAnswersService}
import viewmodels.returns.submit.TaskListPageViewModel
import views.html.returns.submit.TaskListView

import java.time.LocalDate
import scala.concurrent.Future

class TaskListControllerSpec extends SpecBase with MockitoSugar {

  private val testFromDate = LocalDate.of(2024, 10, 1)
  private val testToDate = LocalDate.of(2024, 10, 31)
  private val testDueDate = LocalDate.of(2024, 11, 30)

  private def createObligation(status: ObligationStatus): ObligationItem = {
    ObligationItem(
      identification = None,
      obligationDetails = ObligationDetails(
        openOrFulfilledStatus = status.toString,
        iCFromDate = testFromDate,
        iCToDate = testToDate,
        iCDateReceived = None,
        iCDueDate = testDueDate,
        periodKey = periodKey.value
      )
    )
  }

  "TaskList Controller" - {

    "must return OK and the correct view for a GET with fulfilled obligations" in {
      val mockObligationService = mock[ObligationService]
      val mockRepository = mock[ReturnsUserAnswersService]
      val fulfilledObligation = createObligation(ObligationStatus.F)
      
      when(mockObligationService.getObligations(any())(using any()))
        .thenReturn(Future.successful(ObligationsResponse(Seq(fulfilledObligation))))

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

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          TaskListPageViewModel(returnsUserAnswers, Seq(fulfilledObligation), periodKey)
        )(request).toString
      }
    }
    
    "must return OK and the correct view for a GET with no fulfilled obligations" in {
      val mockObligationService = mock[ObligationService]
      val mockRepository = mock[ReturnsUserAnswersService]
      val openObligation = createObligation(ObligationStatus.O)
      
      when(mockObligationService.getObligations(any())(using any()))
        .thenReturn(Future.successful(ObligationsResponse(Seq(openObligation))))

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

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          TaskListPageViewModel(returnsUserAnswers, Seq(openObligation), periodKey)
        )(request).toString
      }
    }
  }
}
