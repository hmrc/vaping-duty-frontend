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
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{ObligationService, SubmitReturnService}
import viewmodels.returns.submit.CheckYourAnswersViewModel
import views.html.returns.submit.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  private val testDutyRate = BigDecimal("3.15")

  "ReturnsCheckYourAnswers Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getDutyRateForPeriod(eqTo(vpdId), eqTo(periodKey))(any()))
        .thenReturn(Future.successful(Some(testDutyRate)))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]
        val vm = CheckYourAnswersViewModel(returnsUserAnswers, testDutyRate)(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }

    "must use zero duty rate when obligation service returns None" in {

      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getDutyRateForPeriod(eqTo(vpdId), eqTo(periodKey))(any()))
        .thenReturn(Future.successful(None))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckYourAnswersView]
        val vm = CheckYourAnswersViewModel(returnsUserAnswers, BigDecimal(0))(messages(application))

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }

    "must submit and return the correct response for a POST" in {

      val mockService = mock[SubmitReturnService]
      val mockObligationService = mock[ObligationService]

      when(mockService.submit(any())(any())).thenReturn(Future.successful(testReturnSubmissionResponse))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[SubmitReturnService].to(mockService),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()


      running(application) {
        val request = FakeRequest(POST, controllers.returns.submit.routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.returns.submit.routes.ConfirmationController.onPageLoad(Some("vpdReferenceNumber")).url
      }
    }

    "must redirect to journey recovery when there is an issue submitting" in {

      val mockService = mock[SubmitReturnService]
      val mockObligationService = mock[ObligationService]

      when(mockService.submit(any())(any())).thenReturn(Future.successful(Left(ErrorModel(BAD_GATEWAY, "Bad gateway"))))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(
          bind[SubmitReturnService].to(mockService),
          bind[ObligationService].toInstance(mockObligationService)
        )
        .build()


      running(application) {
        val request = FakeRequest(POST, controllers.returns.submit.routes.CheckYourAnswersController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to journey recovery when obligation service fails" in {

      val mockObligationService = mock[ObligationService]

      when(mockObligationService.getDutyRateForPeriod(eqTo(vpdId), eqTo(periodKey))(any()))
        .thenReturn(Future.failed(new RuntimeException("Obligation not found")))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].toInstance(mockObligationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}