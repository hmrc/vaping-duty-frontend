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

package services.contactPreference

import base.SpecBase
import cats.data.EitherT
import models.contactPreference.{EnterEmailOutcome, PreferenceUserAnswers}
import models.emailverification.{EmailVerificationDetails, ErrorModel}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.HttpResponse
import utils.StartEmailVerificationJourneyHelper

import scala.concurrent.Future

class EnterEmailServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val REDIRECT_BASE_URL = "http://localhost:9890"
  private val ERROR_MESSAGE = "There was a problem"

  private val mockEmailVerificationService: EmailVerificationService = mock[EmailVerificationService]
  private val mockPreferenceUserAnswersService: PreferenceUserAnswersService = mock[PreferenceUserAnswersService]
  private val mockStartEmailVerificationJourneyHelper: StartEmailVerificationJourneyHelper = mock[StartEmailVerificationJourneyHelper]

  private val service = new EnterEmailService(
    mockEmailVerificationService,
    mockPreferenceUserAnswersService,
    mockEmailVerificationConnector,
    mockStartEmailVerificationJourneyHelper,
    mockAppConfig
  )

  private val updatedAnswers: PreferenceUserAnswers = userAnswers.copy(emailAddress = Some(emailAddress))
  private val error: ErrorModel = ErrorModel(INTERNAL_SERVER_ERROR, ERROR_MESSAGE)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEmailVerificationService, mockPreferenceUserAnswersService, mockEmailVerificationConnector, mockStartEmailVerificationJourneyHelper, mockAppConfig)
    when(mockAppConfig.emailVerificationRedirectBaseUrl).thenReturn(REDIRECT_BASE_URL)
    when(mockStartEmailVerificationJourneyHelper.createRequest(any(), any())(using any()))
      .thenReturn(testEmailVerificationRequest)
    ()
  }

  private def stubStatus(isVerified: Boolean, isLocked: Boolean): Unit = {
    when(mockEmailVerificationService.retrieveAddressStatus(any(), any(), any())(any()))
      .thenReturn(EitherT.rightT[Future, ErrorModel](EmailVerificationDetails(emailAddress, isVerified, isLocked)))
    ()
  }

  private def stubSet(result: Either[ErrorModel, HttpResponse]): Unit = {
    when(mockPreferenceUserAnswersService.set(any())(any())).thenReturn(Future.successful(result))
    ()
  }

  private def submit(): Either[ErrorModel, EnterEmailOutcome] =
    service.submitEmail(emailAddress, userAnswers, credId).futureValue

  "EnterEmailService" - {

    "submitEmail" - {

      "must return Locked without saving the answers or starting verification when the email is locked" in {
        stubStatus(isVerified = false, isLocked = true)

        submit() mustBe Right(EnterEmailOutcome.Locked)

        verify(mockPreferenceUserAnswersService, never()).set(any())(any())
        verify(mockEmailVerificationConnector, never()).startEmailVerification(any())(any())
      }

      "must save the answers and return Verified without starting verification when the email is verified" in {
        stubStatus(isVerified = true, isLocked = false)
        stubSet(Right(HttpResponse(OK)))

        submit() mustBe Right(EnterEmailOutcome.Verified(updatedAnswers))

        verify(mockPreferenceUserAnswersService).set(eqTo(updatedAnswers))(any())
        verify(mockEmailVerificationConnector, never()).startEmailVerification(any())(any())
      }

      "must save the answers, start verification and return the redirect URL when the email is unverified" in {
        stubStatus(isVerified = false, isLocked = false)
        stubSet(Right(HttpResponse(OK)))
        when(mockEmailVerificationConnector.startEmailVerification(any())(any()))
          .thenReturn(Future.successful(Right(testRedirectUri)))

        submit() mustBe Right(EnterEmailOutcome.VerificationStarted(s"$REDIRECT_BASE_URL${testRedirectUri.redirectUri}"))

        verify(mockPreferenceUserAnswersService).set(eqTo(updatedAnswers))(any())
        verify(mockStartEmailVerificationJourneyHelper).createRequest(eqTo(credId.value), eqTo(emailAddress))(using any())
        verify(mockEmailVerificationConnector).startEmailVerification(eqTo(testEmailVerificationRequest))(any())
      }

      "must return Left without saving the answers when retrieving the email status fails" in {
        when(mockEmailVerificationService.retrieveAddressStatus(any(), any(), any())(any()))
          .thenReturn(EitherT.leftT[Future, EmailVerificationDetails](error))

        submit() mustBe Left(error)

        verify(mockPreferenceUserAnswersService, never()).set(any())(any())
      }

      "must return Left without starting verification when saving the answers fails" in {
        stubStatus(isVerified = false, isLocked = false)
        stubSet(Left(error))

        submit() mustBe Left(error)

        verify(mockEmailVerificationConnector, never()).startEmailVerification(any())(any())
      }

      "must return Left when starting email verification fails" in {
        stubStatus(isVerified = false, isLocked = false)
        stubSet(Right(HttpResponse(OK)))
        when(mockEmailVerificationConnector.startEmailVerification(any())(any()))
          .thenReturn(Future.successful(Left(error)))

        submit() mustBe Left(error)
      }
    }
  }
}
