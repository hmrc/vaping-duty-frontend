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
import connectors.returns.ObligationsConnector
import models.emailverification.ErrorModel
import models.identifiers.PeriodKey
import models.obligations.{ObligationDetails, ObligationsResponse}
import models.returns.{DeclarationDetails, ReturnsUserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.returns.yourdetails.{YourBusinessEmailPage, YourFullNamePage, YourRolePage}
import org.apache.pekko.http.scaladsl.model.HttpResponse
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import queries.returns.ObligationsQuery
import services.returns.{ObligationService, ReturnsUserAnswersService, SubmitReturnService}
import views.html.returns.submit.DeclarationView

import scala.concurrent.Future

class DeclarationControllerSpec extends SpecBase with MockitoSugar {

  private val validDeclaration = DeclarationDetails(
    fullName = "John Smith",
    capacityInWhichSigned = "Director",
    signeesEmailAddress = "john.smith@example.com"
  )

  private val june2026 = PeriodKey("26KF")

  private val yourDetailsAnswers: ReturnsUserAnswers = returnsUserAnswers
    .copy(periodKey = june2026.value)
    .set(YourFullNamePage, validDeclaration.fullName).success.value
    .set(YourRolePage, validDeclaration.capacityInWhichSigned).success.value
    .set(YourBusinessEmailPage, validDeclaration.signeesEmailAddress).success.value

  private val yourDetailsIncomplete: ReturnsUserAnswers = returnsUserAnswers
    .copy(periodKey = june2026.value)
    .set(YourFullNamePage, validDeclaration.fullName).success.value


  private val obligationDataSingleOpen: ObligationsResponse = ObligationsResponse(
    obligation = obligations(Seq(openObligation(june2026)))
  )

  private val obligationDetailsSingleOpen: Seq[ObligationDetails] =
    obligationDataSingleOpen.obligation.flatMap(_.obligationDetails)

  private def stubbedSessionRepository(): ReturnsUserAnswersService = {
    val mockSessionRepository = mock[ReturnsUserAnswersService]
    when(mockSessionRepository.set(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK))))
    mockSessionRepository
  }

  private def stubLiveObligations(mockObligationService: ObligationService): Unit = {
    when(mockObligationService.getObligations(any())(using any()))
      .thenReturn(Future.successful(obligationDetailsSingleOpen))
    ()
  }

  "DeclarationController" - {

    "onPageLoad" - {

      "must return OK and the correct view for a GET" in {

        val mockObligationService = mock[ObligationService]
        stubLiveObligations(mockObligationService)

        val application = applicationBuilder(returnsUserAnswers = Some(yourDetailsAnswers))
          .overrides(
            bind[ObligationService].toInstance(mockObligationService),
            bind[ReturnsUserAnswersService].toInstance(stubbedSessionRepository())
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, controllers.returns.submit.routes.DeclarationController.onPageLoad().url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[DeclarationView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(june2026, "June", "2026")(request, messages(application)).toString
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

      "must save the obligations in the user answers when none are saved" in {

        val mockSessionRepository = stubbedSessionRepository()
        val mockObligationService = mock[ObligationService]
        stubLiveObligations(mockObligationService)

        val application = applicationBuilder(returnsUserAnswers = Some(yourDetailsAnswers))
          .overrides(
            bind[ObligationService].toInstance(mockObligationService),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, controllers.returns.submit.routes.DeclarationController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK

          verify(mockSessionRepository).set(any())(any())
        }
      }

      "must save the obligations even when the same obligations are already saved" in {

        val mockSessionRepository = stubbedSessionRepository()
        val mockObligationService = mock[ObligationService]
        stubLiveObligations(mockObligationService)

        val savedAnswers = yourDetailsAnswers
          .set(ObligationsQuery, obligationDetailsSingleOpen).success.value

        val application = applicationBuilder(returnsUserAnswers = Some(savedAnswers))
          .overrides(
            bind[ObligationService].toInstance(mockObligationService),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, controllers.returns.submit.routes.DeclarationController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK

          verify(mockSessionRepository).set(any())(any())
        }
      }

      "must save the obligations when the saved obligations are different" in {

        val mockSessionRepository = stubbedSessionRepository()
        val mockObligationService = mock[ObligationService]
        stubLiveObligations(mockObligationService)

        val staleAnswers = yourDetailsAnswers
          .set(ObligationsQuery, Seq(fulfilledObligation(june2026))).success.value

        val application = applicationBuilder(returnsUserAnswers = Some(staleAnswers))
          .overrides(
            bind[ObligationService].toInstance(mockObligationService),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, controllers.returns.submit.routes.DeclarationController.onPageLoad().url)

          val result = route(application, request).value

          status(result) mustEqual OK

          verify(mockSessionRepository).set(any())(any())
        }
      }

      "must still return OK when saving the obligations returns an error" in {

        val mockSessionRepository = mock[ReturnsUserAnswersService]
        when(mockSessionRepository.set(any())(any()))
          .thenReturn(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, "failed"))))

        val mockObligationService = mock[ObligationService]
        stubLiveObligations(mockObligationService)

        val application = applicationBuilder(returnsUserAnswers = Some(yourDetailsAnswers))
          .overrides(
            bind[ObligationService].toInstance(mockObligationService),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, controllers.returns.submit.routes.DeclarationController.onPageLoad().url)

          status(route(application, request).value) mustEqual OK
        }
      }

      "must allow the framework to handle the error when saving the obligations fails" in {

        val mockSessionRepository = mock[ReturnsUserAnswersService]
        when(mockSessionRepository.set(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val mockObligationService = mock[ObligationService]
        stubLiveObligations(mockObligationService)

        val application = applicationBuilder(returnsUserAnswers = Some(yourDetailsAnswers))
          .overrides(
            bind[ObligationService].toInstance(mockObligationService),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, controllers.returns.submit.routes.DeclarationController.onPageLoad().url)

          val result = route(application, request).value

          whenReady(result.failed) { exception =>
            exception mustBe a[RuntimeException]
          }
        }
      }
    }

    "onSubmit" - {

      "must submit the return once with the user answers, clear them and redirect to Confirmation page" in {

        val mockSessionRepository = mock[ReturnsUserAnswersService]
        val mockSubmitReturnService = mock[SubmitReturnService]
        val mockObligationService = mock[ObligationService]

        when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))
        when(mockSubmitReturnService.submit(any(), any())(any())) thenReturn Future.successful(testReturnSubmissionResponse)
        when(mockSessionRepository.clear(any(), any())(any()))
          .thenReturn(Future.successful(Right(())))
        when(mockObligationService.getObligations(any(), any())(using any()))
          .thenReturn(Future.successful(obligationDetailsSingleOpen))

        val application =
          applicationBuilder(returnsUserAnswers = Some(yourDetailsAnswers))
            .overrides(
              bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
              bind[SubmitReturnService].toInstance(mockSubmitReturnService),
              bind[ObligationService].toInstance(mockObligationService)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.returns.submit.routes.DeclarationController.onSubmit().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value must include(controllers.returns.submit.routes.ConfirmationController.onPageLoad().url)
          redirectLocation(result).value must include(s"period=${june2026.value}")

          verify(mockSubmitReturnService, times(1)).submit(eqTo(yourDetailsAnswers), any())(any())
          verify(mockSessionRepository, times(1)).clear(any(), any())(any())
        }
      }

      "must not call the Obligations API when the obligations are cached in the user answers" in {

        val mockSessionRepository = mock[ReturnsUserAnswersService]
        val mockSubmitReturnService = mock[SubmitReturnService]
        val mockObligationsConnector = mock[ObligationsConnector]

        when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))
        when(mockSubmitReturnService.submit(any(), any())(any())) thenReturn Future.successful(testReturnSubmissionResponse)
        when(mockSessionRepository.clear(any(), any())(any()))
          .thenReturn(Future.successful(Right(())))

        val cachedAnswers = yourDetailsAnswers
          .set(ObligationsQuery, obligationDetailsSingleOpen).success.value

        val application =
          applicationBuilder(returnsUserAnswers = Some(cachedAnswers))
            .overrides(
              bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
              bind[SubmitReturnService].toInstance(mockSubmitReturnService),
              bind[ObligationsConnector].toInstance(mockObligationsConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.returns.submit.routes.DeclarationController.onSubmit().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value must include(controllers.returns.submit.routes.ConfirmationController.onPageLoad().url)

          verify(mockObligationsConnector, never()).getObligations(any())(using any())
          verify(mockSubmitReturnService).submit(any(), eqTo(obligationDetailsSingleOpen))(any())
        }
      }

      "must fall back to the Obligations API when the obligations are not cached in the user answers" in {

        val mockSessionRepository = mock[ReturnsUserAnswersService]
        val mockSubmitReturnService = mock[SubmitReturnService]
        val mockObligationsConnector = mock[ObligationsConnector]

        when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))
        when(mockSubmitReturnService.submit(any(), any())(any())) thenReturn Future.successful(testReturnSubmissionResponse)
        when(mockSessionRepository.clear(any(), any())(any()))
          .thenReturn(Future.successful(Right(())))
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(obligationDataSingleOpen))

        val application =
          applicationBuilder(returnsUserAnswers = Some(yourDetailsAnswers))
            .overrides(
              bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
              bind[SubmitReturnService].toInstance(mockSubmitReturnService),
              bind[ObligationsConnector].toInstance(mockObligationsConnector)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.returns.submit.routes.DeclarationController.onSubmit().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER

          verify(mockObligationsConnector).getObligations(any())(using any())
          verify(mockSubmitReturnService).submit(any(), any())(any())
        }
      }

      "must redirect to Journey Recovery and not clear the answers when submission fails because your details is not complete" in {

        val mockSessionRepository = mock[ReturnsUserAnswersService]
        val mockSubmitReturnService = mock[SubmitReturnService]

        when(mockSubmitReturnService.submit(any(), any())(any()))
          .thenReturn(Future.failed(new IllegalStateException("Declaration details are required for submission")))

        val cachedAnswers = yourDetailsIncomplete
          .set(ObligationsQuery, obligationDetailsSingleOpen).success.value

        val application = applicationBuilder(returnsUserAnswers = Some(cachedAnswers))
          .overrides(
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[SubmitReturnService].toInstance(mockSubmitReturnService)
          )
          .build()

        running(application) {
          val request = FakeRequest(POST, controllers.returns.submit.routes.DeclarationController.onSubmit().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          verify(mockSessionRepository, never()).clear(any(), any())(any())
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(returnsUserAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, controllers.returns.submit.routes.DeclarationController.onSubmit().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to journey recovery when there is an issue submitting" in {

        val mockService = mock[SubmitReturnService]
        val mockSessionRepository = mock[ReturnsUserAnswersService]
        val mockObligationService = mock[ObligationService]

        when(mockService.submit(any(), any())(any())).thenReturn(Future.successful(Left(ErrorModel(BAD_GATEWAY, "Bad gateway"))))
        when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))
        when(mockObligationService.getObligations(any(), any())(using any()))
          .thenReturn(Future.successful(obligationDetailsSingleOpen))

        val application = applicationBuilder(returnsUserAnswers = Some(yourDetailsAnswers))
          .overrides(
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[SubmitReturnService].to(mockService),
            bind[ObligationService].toInstance(mockObligationService)
          )
          .build()


        running(application) {
          val request = FakeRequest(POST, controllers.returns.submit.routes.DeclarationController.onSubmit().url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
