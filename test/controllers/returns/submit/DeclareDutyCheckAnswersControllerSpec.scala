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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.returns.EnterDutyAmountPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{DutyRateService, ObligationService}
import viewmodels.returns.submit.DeclareDutyCheckAnswersViewModel
import views.html.returns.submit.DeclareDutyCheckAnswersView

import scala.concurrent.Future

class DeclareDutyCheckAnswersControllerSpec extends SpecBase {

  "DeclareDutyCheckAnswers Controller" - {

    "must return OK and the correct view for a GET" in {

      val ua = returnsUserAnswers.set(EnterDutyAmountPage, BigDecimal(100)).success.value

      val mockDutyRateService = mock[DutyRateService]

      when(mockDutyRateService.getDutyRate(any(), any())(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))

      val application = applicationBuilder(returnsUserAnswers = Some(ua))
        .overrides(bind[DutyRateService].toInstance(mockDutyRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.DeclareDutyCheckAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeclareDutyCheckAnswersView]
        val vm = DeclareDutyCheckAnswersViewModel(ua, testDutyRate, periodKey)(messages(application)).get

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, vm)(request, messages(application)).toString
      }
    }

    "must redirect to journey recovery when EnterDutyAmountPage is not answered" in {

      val mockDutyRateService = mock[DutyRateService]

      when(mockDutyRateService.getDutyRate(any(), any())(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[DutyRateService].toInstance(mockDutyRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.DeclareDutyCheckAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must fail when obligation service returns None" in {

      val mockDutyRateService = mock[DutyRateService]

      when(mockDutyRateService.getDutyRate(any(), any())(using any(), any()))
        .thenReturn(Future.failed(RuntimeException("No duty rate found")))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[DutyRateService].toInstance(mockDutyRateService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.DeclareDutyCheckAnswersController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustBe "No duty rate found"
        }
      }
    }

    "must redirect to TaskList for a POST" in {

      val mockObligationService = mock[ObligationService]

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {
        val request = FakeRequest(POST, controllers.returns.submit.routes.DeclareDutyCheckAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=${periodKey.value}"
      }
    }
  }
}