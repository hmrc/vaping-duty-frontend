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
import forms.returns.DeclarationFormProvider
import models.emailverification.ErrorModel
import models.identifiers.PeriodKey
import models.obligations.{ObligationDetails, ObligationsResponse}
import models.returns.DeclarationDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.returns.DeclarationPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.{ObligationService, ReturnsUserAnswersService, SubmitReturnService}
import views.html.returns.submit.DeclarationView

import scala.concurrent.Future

class DeclarationControllerSpec extends SpecBase with MockitoSugar {

  private val formProvider = new DeclarationFormProvider()
  private val form = formProvider()

  private val validDeclaration = DeclarationDetails(
    fullName = "John Smith",
    capacityInWhichSigned = "Director",
    signeesEmailAddress = "john.smith@example.com"
  )

  private val june2026 = PeriodKey("26AF")

  private val obligationDataSingleOpen: ObligationsResponse = ObligationsResponse(
    obligation = obligations(Seq(openObligation(june2026)))
  )

  private val obligationDetailsSingleOpen: Seq[ObligationDetails] =
    obligationDataSingleOpen.obligation.map(_.obligationDetails)

  "DeclarationController" - {

    "onPageLoad" - {

      "must return OK and the correct view for a GET" in {

        val mockObligationService = mock[ObligationService]
        when(mockObligationService.getObligationsDirectly(any())(using any()))
          .thenReturn(Future.successful(obligationDetailsSingleOpen))

        val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers.copy(periodKey = june2026.value)))
          .overrides(bind[ObligationService].toInstance(mockObligationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, controllers.returns.submit.routes.DeclarationController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DeclarationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(june2026, form, "June", "2026")(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers = returnsUserAnswers.set(DeclarationPage, validDeclaration).success.value

        val mockObligationService = mock[ObligationService]
        when(mockObligationService.getObligationsDirectly(any())(using any()))
          .thenReturn(Future.successful(obligationDetailsSingleOpen))

        val application = applicationBuilder(returnsUserAnswers = Some(userAnswers.copy(periodKey = june2026.value)))
          .overrides(bind[ObligationService].toInstance(mockObligationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, controllers.returns.submit.routes.DeclarationController.onPageLoad().url)

          val view = application.injector.instanceOf[DeclarationView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(june2026, form.fill(validDeclaration), "June", "2026")(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(returnsUserAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, controllers.returns.submit.routes.DeclarationController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must save the answer, submit the return and redirect to Confirmation page when valid data is submitted" in {

        val mockSessionRepository = mock[ReturnsUserAnswersService]
        val mockSubmitReturnService = mock[SubmitReturnService]
        val mockObligationService = mock[ObligationService]

        when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))
        when(mockSubmitReturnService.submit(any())(any())) thenReturn Future.successful(testReturnSubmissionResponse)
        when(mockSessionRepository.clear(any(), any())(any()))
          .thenReturn(Future.successful(Right(())))
        when(mockObligationService.getObligationsDirectly(any())(using any()))
          .thenReturn(Future.successful(obligationDetailsSingleOpen))

        val application =
          applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers.copy(periodKey = june2026.value)))
            .overrides(
              bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
              bind[SubmitReturnService].toInstance(mockSubmitReturnService),
              bind[ObligationService].toInstance(mockObligationService)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.returns.submit.routes.DeclarationController.onSubmit().url)
              .withFormUrlEncodedBody(
                (formProvider.FULL_NAME, "John Smith"),
                (formProvider.CAPACITY, "Director"),
                (formProvider.EMAIL, "john.smith@example.com")
              )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value must include(controllers.returns.submit.routes.ConfirmationController.onPageLoad().url)
          redirectLocation(result).value must include(s"period=${june2026.value}")

          verify(mockSessionRepository).set(any())(any())
          verify(mockSubmitReturnService).submit(any())(any())
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val mockObligationService = mock[ObligationService]
        when(mockObligationService.getObligationsDirectly(any())(using any()))
          .thenReturn(Future.successful(obligationDetailsSingleOpen))

        val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers.copy(periodKey = june2026.value)))
          .overrides(bind[ObligationService].toInstance(mockObligationService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.returns.submit.routes.DeclarationController.onSubmit().url)
              .withFormUrlEncodedBody(("fullName", ""))

          val boundForm = form.bind(Map("fullName" -> ""))

          val view = application.injector.instanceOf[DeclarationView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(june2026, boundForm, "June", "2026")(request, messages(application)).toString
        }
      }

      "must return a Bad Request and errors when invalid email is submitted" in {

        val mockObligationService = mock[ObligationService]
        when(mockObligationService.getObligationsDirectly(any())(using any()))
          .thenReturn(Future.successful(obligationDetailsSingleOpen))

        val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers.copy(periodKey = june2026.value)))
          .overrides(bind[ObligationService].toInstance(mockObligationService))
          .build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.returns.submit.routes.DeclarationController.onSubmit().url)
              .withFormUrlEncodedBody(
                ("fullName", "John Smith"),
                ("capacityInWhichSigned", "Director"),
                ("signeesEmailAddress", "invalid-email")
              )

          val boundForm = form.bind(Map(
            "fullName" -> "John Smith",
            "capacityInWhichSigned" -> "Director",
            "signeesEmailAddress" -> "invalid-email"
          ))

          val view = application.injector.instanceOf[DeclarationView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(june2026, boundForm, "June", "2026")(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(returnsUserAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.returns.submit.routes.DeclarationController.onSubmit().url)
              .withFormUrlEncodedBody(
                ("fullName", "John Smith"),
                ("capacityInWhichSigned", "Director"),
                ("signeesEmailAddress", "john.smith@example.com")
              )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
      "must redirect to journey recovery when there is an issue submitting" in {

        val mockService = mock[SubmitReturnService]
        val mockSessionRepository = mock[ReturnsUserAnswersService]
        val mockObligationService = mock[ObligationService]

        when(mockService.submit(any())(any())).thenReturn(Future.successful(Left(ErrorModel(BAD_GATEWAY, "Bad gateway"))))
        when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))
        when(mockObligationService.getObligationsDirectly(any())(using any()))
          .thenReturn(Future.successful(obligationDetailsSingleOpen))

        val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[SubmitReturnService].to(mockService),
            bind[ObligationService].toInstance(mockObligationService)
          )
          .build()


        running(application) {
          val request = FakeRequest(POST, controllers.returns.submit.routes.DeclarationController.onSubmit().url)
            .withFormUrlEncodedBody(
              (formProvider.FULL_NAME, "John Smith"),
              (formProvider.CAPACITY, "Director"),
              (formProvider.EMAIL, "john.smith@example.com")
            )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}