/*
 * Copyright 2025 HM Revenue & Customs
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

package services

import base.SpecBase
import cats.data.EitherT
import connectors.{EmailVerificationConnector, SubmitPreferencesConnector}
import models.*
import models.emailverification.*
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.{Ok, Redirect}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class EmailVerificationServiceSpec extends SpecBase {

  val mockAuditService: AuditService = mock[AuditService]

  "retrieveAddressStatusAndAddToCache" - {
    "when the call to get a user's verification details fails, must return an error" in new Setup {
      when(mockEmailVerificationConnector.getEmailVerification(any())(any()))
        .thenReturn(
          EitherT(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, "test error"))))
        )

      whenReady(
        testService.retrieveAddressStatus(testVerificationDetails, emailAddress2, userAnswers).value
      ) {
        _ mustBe Left(ErrorModel(INTERNAL_SERVER_ERROR, "test error"))
      }
    }

    "when get verification details is successful" - {
      "and the new email address is verified" in new Setup {
        when(mockEmailVerificationConnector.getEmailVerification(any())(any()))
          .thenReturn(
            EitherT(Future.successful(Right(testGetVerificationStatusResponse)))
          )

        whenReady(
          testService.retrieveAddressStatus(testVerificationDetails, emailAddress4, userAnswers).value
        ) {
          _ mustBe Right(EmailVerificationDetails(emailAddress4, isVerified = true, isLocked = false))
        }
      }
    }

    "if any email records matching what the user entered are verified, return the email as verified" in new Setup {
      val verifiedDetails: GetVerificationStatusResponseEmailAddressDetails =
        GetVerificationStatusResponseEmailAddressDetails(emailAddress = emailAddress, verified = true, locked = false)

      val verifiedDetails2: GetVerificationStatusResponseEmailAddressDetails = verifiedDetails.copy(verified = false)

      val verifiedDetailsResponse: GetVerificationStatusResponse =
        GetVerificationStatusResponse(List(verifiedDetails, verifiedDetails2))

      when(mockEmailVerificationConnector.getEmailVerification(any())(any()))
        .thenReturn(
          EitherT(Future.successful(Right(verifiedDetailsResponse)))
        )

      whenReady(
        testService.retrieveAddressStatus(testVerificationDetails, emailAddress, userAnswers).value
      ) {
        _ mustBe Right(EmailVerificationDetails(emailAddress, isVerified = true, isLocked = false))
      }
    }

    "if all email records matching what the user entered are unverified, return the email as unverified" in new Setup {
      val verifiedDetails: GetVerificationStatusResponseEmailAddressDetails =
        GetVerificationStatusResponseEmailAddressDetails(emailAddress = emailAddress, verified = false, locked = false)

      val verifiedDetails2: GetVerificationStatusResponseEmailAddressDetails =
        GetVerificationStatusResponseEmailAddressDetails(emailAddress = emailAddress, verified = false, locked = false)

      val verifiedDetailsResponse: GetVerificationStatusResponse =
        GetVerificationStatusResponse(List(verifiedDetails, verifiedDetails2))

      when(mockEmailVerificationConnector.getEmailVerification(any())(any()))
        .thenReturn(
          EitherT(Future.successful(Right(verifiedDetailsResponse)))
        )

      when(mockUserAnswersService.set(any())(any()))
        .thenReturn(
          Future.successful(Right(HttpResponse(status = OK, body = "test body")))
        )

      whenReady(
        testService.retrieveAddressStatus(testVerificationDetails, emailAddress, userAnswers).value
      ) {
        _ mustBe Right(EmailVerificationDetails(emailAddress, isVerified = false, isLocked = false))
      }
    }

    "if any email records matching what the user entered are locked, return the email as locked" in new Setup {
      val verifiedDetails: GetVerificationStatusResponseEmailAddressDetails =
        GetVerificationStatusResponseEmailAddressDetails(emailAddress = emailAddress, verified = false, locked = true)

      val verifiedDetails2: GetVerificationStatusResponseEmailAddressDetails = verifiedDetails.copy(locked = false)

      val verifiedDetailsResponse: GetVerificationStatusResponse =
        GetVerificationStatusResponse(List(verifiedDetails, verifiedDetails2))

      when(mockEmailVerificationConnector.getEmailVerification(any())(any()))
        .thenReturn(
          EitherT(Future.successful(Right(verifiedDetailsResponse)))
        )

      when(mockUserAnswersService.set(any())(any()))
        .thenReturn(
          Future.successful(Right(HttpResponse(status = OK, body = "test body")))
        )

      whenReady(
        testService.retrieveAddressStatus(testVerificationDetails, emailAddress, userAnswers).value
      ) {
        _ mustBe Right(EmailVerificationDetails(emailAddress, isVerified = false, isLocked = true))
      }
    }

    "if all email records matching what the user entered are not locked, return the email as not locked" in new Setup {
      val verifiedDetails: GetVerificationStatusResponseEmailAddressDetails =
        GetVerificationStatusResponseEmailAddressDetails(emailAddress = emailAddress, verified = false, locked = false)

      val verifiedDetails2: GetVerificationStatusResponseEmailAddressDetails =
        GetVerificationStatusResponseEmailAddressDetails(emailAddress = emailAddress, verified = false, locked = false)

      val verifiedDetailsResponse: GetVerificationStatusResponse =
        GetVerificationStatusResponse(List(verifiedDetails, verifiedDetails2))

      when(mockEmailVerificationConnector.getEmailVerification(any())(any()))
        .thenReturn(
          EitherT(Future.successful(Right(verifiedDetailsResponse)))
        )

      when(mockUserAnswersService.set(any())(any()))
        .thenReturn(
          Future.successful(Right(HttpResponse(status = OK, body = "test body")))
        )

      whenReady(
        testService.retrieveAddressStatus(testVerificationDetails, emailAddress, userAnswers).value
      ) {
        _ mustBe Right(EmailVerificationDetails(emailAddress, isVerified = false, isLocked = false))
      }
    }
  }

  "redirectIfLocked" - {

    "must redirect to locked email page when address returns locked flag" in new Setup {

      whenReady(testService.redirectIfLocked(Future.successful(Ok("Okay")), isLocked = true)) {
        _ mustBe Redirect(controllers.contactPreference.routes.LockedEmailController.onPageLoad())
      }
    }

    "must render result page when address is not locked" in new Setup {

      whenReady(testService.redirectIfLocked(Future.successful(Ok("Okay")), isLocked = false)) {
        _ mustBe Ok("Okay")
      }
    }
  }

  "submitVerifiedEmail" - {

    "must redirect to email confirmation page when successful" in new Setup {

      when(mockSubmitPreferencesConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Right(testSubmissionResponse)))

      whenReady(testService.submitVerifiedEmail(
          emailAddress,
          verified = true,
          mockSubmitPreferencesConnector,
        mockAuditService
        )(hc, DataRequest(FakeRequest(), vpdId, userId, credId, userAnswers))) {

        _ mustBe Redirect(controllers.contactPreference.routes.ConfirmationController.onPageLoad())
      }
    }

    "must redirect to journey recovery if trying to submit unverified email address" in new Setup {

      when(mockSubmitPreferencesConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Right(testSubmissionResponse)))

      whenReady(testService.submitVerifiedEmail(
        emailAddress,
        verified = false,
        mockSubmitPreferencesConnector,
        mockAuditService
      )(hc, DataRequest(FakeRequest(), vpdId, userId, credId, userAnswers))) {

        _ mustBe Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }

  class Setup {
    val mockEmailVerificationConnector: EmailVerificationConnector = mock[EmailVerificationConnector]
    val mockUserAnswersService: UserAnswersService                 = mock[UserAnswersService]
    val mockSubmitPreferencesConnector: SubmitPreferencesConnector = mock[SubmitPreferencesConnector]
    val testService                                                = new EmailVerificationService(mockEmailVerificationConnector)
  }
}
