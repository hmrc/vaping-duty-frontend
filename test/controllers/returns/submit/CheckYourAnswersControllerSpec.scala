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
import models.emailverification.ErrorModel
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{ObligationService, ReturnsUserAnswersService, SubmitReturnService}
import uk.gov.hmrc.http.InternalServerException
import viewmodels.returns.submit.CheckYourAnswersViewModel
import views.html.returns.submit.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  "ReturnsCheckYourAnswers Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getDutyRateForPeriod(eqTo(vpdId), eqTo(periodKey))(using any()))
        .thenReturn(Future.successful(Some(testDutyRate)))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]
        val vm = CheckYourAnswersViewModel(returnsUserAnswers, testDutyRate, periodKey)(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, vm)(request, messages(application)).toString
      }
    }

    "must send the user to the error page if no duty rate is found for the month of the return" in {

      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getDutyRateForPeriod(eqTo(vpdId), eqTo(periodKey))(using any()))
        .thenReturn(Future.successful(None))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustBe "No duty rate found"
        }
      }
    }

    "must redirect to Declaration page for a POST" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(POST, controllers.returns.submit.routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe s"${controllers.returns.submit.routes.DeclarationController.onPageLoad().url}?period=${periodKey.value}"
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(returnsUserAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, controllers.returns.submit.routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return INTERNAL_SERVER_ERROR when obligation service fails" in {

      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getDutyRateForPeriod(eqTo(vpdId), eqTo(periodKey))(using any()))
        .thenReturn(Future.failed(new RuntimeException("Obligation not found")))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        whenReady(result.failed) { exception =>
          exception mustBe a[RuntimeException]
          exception.getMessage mustBe "Obligation not found"
        }
      }
    }
  }
}
