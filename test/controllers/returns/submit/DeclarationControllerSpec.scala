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
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.DeclarationService
import viewmodels.returns.submit.DeclarationViewModel
import views.html.returns.submit.DeclarationView

import scala.concurrent.Future

class DeclarationControllerSpec extends SpecBase with MockitoSugar {

  private val JUNE = "June"
  private val YEAR_2026 = "2026"
  private val SUBMIT_ERROR_MESSAGE = "failed"

  private lazy val declarationRoute = controllers.returns.submit.routes.DeclarationController.onPageLoad().url
  private lazy val confirmationRoute = controllers.returns.submit.routes.ConfirmationController.onPageLoad().url
  private lazy val journeyRecoveryRoute = controllers.routes.JourneyRecoveryController.onPageLoad().url

  private val declarationViewModel = DeclarationViewModel(periodKey, JUNE, YEAR_2026)

  private def applicationWith(mockDeclarationService: DeclarationService) =
    applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
      .overrides(bind[DeclarationService].toInstance(mockDeclarationService))
      .build()

  "DeclarationController" - {

    "onPageLoad" - {

      "must return OK and the correct view for a GET" in {
        val mockDeclarationService: DeclarationService = mock[DeclarationService]
        when(mockDeclarationService.buildViewModel(eqTo(vpdId), eqTo(periodKey), eqTo(returnsUserAnswers))(using any(), any()))
          .thenReturn(Future.successful(declarationViewModel))

        val application = applicationWith(mockDeclarationService)

        running(application) {
          val request = FakeRequest(GET, declarationRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DeclarationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(declarationViewModel)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(returnsUserAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, declarationRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual journeyRecoveryRoute
        }
      }
    }

    "onSubmit" - {

      "must redirect to the Confirmation page when the return is submitted" in {
        val mockDeclarationService: DeclarationService = mock[DeclarationService]
        when(mockDeclarationService.submit(eqTo(vpdId), eqTo(periodKey), eqTo(returnsUserAnswers))(using any(), any()))
          .thenReturn(Future.successful(Right(())))

        val application = applicationWith(mockDeclarationService)

        running(application) {
          val request = FakeRequest(POST, declarationRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual s"$confirmationRoute?period=${periodKey.value}"
        }
      }

      "must redirect to Journey Recovery when the submission fails" in {
        val mockDeclarationService: DeclarationService = mock[DeclarationService]
        when(mockDeclarationService.submit(eqTo(vpdId), eqTo(periodKey), eqTo(returnsUserAnswers))(using any(), any()))
          .thenReturn(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, SUBMIT_ERROR_MESSAGE))))

        val application = applicationWith(mockDeclarationService)

        running(application) {
          val request = FakeRequest(POST, declarationRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual journeyRecoveryRoute
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(returnsUserAnswers = None).build()

        running(application) {
          val request = FakeRequest(POST, declarationRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual journeyRecoveryRoute
        }
      }
    }
  }
}
