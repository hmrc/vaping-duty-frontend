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
import connectors.EmailVerificationConnector
import models.*
import models.emailverification._
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class EmailVerificationServiceSpec extends SpecBase {

  "retrieveAddressStatusAndAddToCache" - {
    "when the call to get a user's verification details fails, must return an error" in new Setup {
      when(mockEmailVerificationConnector.getEmailVerification(any())(any()))
        .thenReturn(
          EitherT(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, "test error"))))
        )

      whenReady(
        testService.retrieveAddressStatusAndAddToCache(testVerificationDetails, emailAddress2, userAnswers).value
      ) {
        _ mustBe Left(ErrorModel(INTERNAL_SERVER_ERROR, "test error"))
      }
    }

    "when the call to set user answers fails, must return an error" in new Setup {
      when(mockEmailVerificationConnector.getEmailVerification(any())(any()))
        .thenReturn(EitherT(Future.successful(Right(testGetVerificationStatusResponse))))

      when(mockUserAnswersService.set(any())(any()))
        .thenReturn(
          Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, "test message of an error")))
        )

      whenReady(
        testService.retrieveAddressStatusAndAddToCache(testVerificationDetails, emailAddress2, userAnswers).value
      ) {
        _ mustBe Left(ErrorModel(INTERNAL_SERVER_ERROR, "test message of an error"))
      }
    }

    "when get verification details and user answers set are successful" - {
      "and the new email address is verified and not in the cache, must add the new email to the cache and return it" in new Setup {
        val updatedUserAnswers: ContactPreferenceUserAnswers =
          userAnswers.copy(verifiedEmailAddresses = userAnswers.verifiedEmailAddresses ++ Set(emailAddress4))

        when(mockEmailVerificationConnector.getEmailVerification(any())(any()))
          .thenReturn(
            EitherT(Future.successful(Right(testGetVerificationStatusResponse)))
          )

        when(mockUserAnswersService.set(eqTo(updatedUserAnswers))(any()))
          .thenReturn(
            Future.successful(Right(HttpResponse(status = OK, body = "test body")))
          )

        whenReady(
          testService.retrieveAddressStatusAndAddToCache(testVerificationDetails, emailAddress4, userAnswers).value
        ) {
          _ mustBe Right(EmailVerificationDetails(emailAddress4, isVerified = true, isLocked = false))
        }

        verify(mockUserAnswersService, times(1)).set(eqTo(updatedUserAnswers))(any())
      }

      "and the new email address is verified but is in the cache, must return the email details" in new Setup {
        when(mockEmailVerificationConnector.getEmailVerification(any())(any()))
          .thenReturn(
            EitherT(Future.successful(Right(testGetVerificationStatusResponse)))
          )

        when(mockUserAnswersService.set(eqTo(userAnswers))(any()))
          .thenReturn(
            Future.successful(Right(HttpResponse(status = OK, body = "test body")))
          )

        whenReady(
          testService.retrieveAddressStatusAndAddToCache(testVerificationDetails, emailAddress2, userAnswers).value
        ) {
          _ mustBe Right(EmailVerificationDetails(emailAddress2, isVerified = true, isLocked = true))
        }

        verify(mockUserAnswersService, times(1)).set(eqTo(userAnswers))(any())
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

      when(mockUserAnswersService.set(any())(any()))
        .thenReturn(
          Future.successful(Right(HttpResponse(status = OK, body = "test body")))
        )

      whenReady(
        testService.retrieveAddressStatusAndAddToCache(testVerificationDetails, emailAddress, userAnswers).value
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
        testService.retrieveAddressStatusAndAddToCache(testVerificationDetails, emailAddress, userAnswers).value
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
        testService.retrieveAddressStatusAndAddToCache(testVerificationDetails, emailAddress, userAnswers).value
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
        testService.retrieveAddressStatusAndAddToCache(testVerificationDetails, emailAddress, userAnswers).value
      ) {
        _ mustBe Right(EmailVerificationDetails(emailAddress, isVerified = false, isLocked = false))
      }
    }

  }

  class Setup {
    val mockEmailVerificationConnector: EmailVerificationConnector = mock[EmailVerificationConnector]
    val mockUserAnswersService: UserAnswersService                 = mock[UserAnswersService]
    val testService                                                = new EmailVerificationService(mockEmailVerificationConnector, mockUserAnswersService)
  }
}
