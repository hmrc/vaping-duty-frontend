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

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import connectors.contactPreference.EmailVerificationConnector
import models.contactPreference.{EnterEmailOutcome, PreferenceUserAnswers}
import models.emailverification.{EmailVerificationDetails, ErrorModel, VerificationDetails}
import models.identifiers.CredentialId
import play.api.Logging
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import utils.StartEmailVerificationJourneyHelper

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnterEmailService @Inject()(
                                   emailVerificationService: EmailVerificationService,
                                   sessionService: PreferenceUserAnswersService,
                                   emailVerificationConnector: EmailVerificationConnector,
                                   startEmailVerificationJourneyHelper: StartEmailVerificationJourneyHelper,
                                   config: FrontendAppConfig
                                 )(using ExecutionContext) extends Logging {

  private val RETRIEVE_STATUS_FAILED_MESSAGE    = "[EnterEmailService][submitEmail] Error retrieving verified email list."
  private val SAVE_ANSWERS_FAILED_MESSAGE       = "[EnterEmailService][saveAndContinue] Error setting user answers."
  private val START_VERIFICATION_FAILED_MESSAGE = "[EnterEmailService][startEmailVerification] Error starting email verification."

  // A locked email is neither saved nor sent for verification.
  // A verified email is saved; an unverified one is saved and handed off to the email verification journey.
  def submitEmail(email: String, userAnswers: PreferenceUserAnswers, credId: CredentialId)
                 (using HeaderCarrier, Messages): Future[Either[ErrorModel, EnterEmailOutcome]] = {

    val updatedAnswers = userAnswers.copy(emailAddress = Some(email))

    emailVerificationService.retrieveAddressStatus(VerificationDetails(credId), email, updatedAnswers).value.flatMap {
      case Left(error) =>
        Future.successful(logError(RETRIEVE_STATUS_FAILED_MESSAGE, error))
      case Right(details) if details.isLocked =>
        Future.successful(Right(EnterEmailOutcome.Locked))
      case Right(details) =>
        saveAndContinue(updatedAnswers, details, credId)
    }
  }

  private def saveAndContinue(updatedAnswers: PreferenceUserAnswers, details: EmailVerificationDetails, credId: CredentialId)
                             (using HeaderCarrier, Messages): Future[Either[ErrorModel, EnterEmailOutcome]] =
    sessionService.set(updatedAnswers).flatMap {
      case Left(error) =>
        Future.successful(logError(SAVE_ANSWERS_FAILED_MESSAGE, error))
      case Right(_) if details.isVerified =>
        Future.successful(Right(EnterEmailOutcome.Verified(updatedAnswers)))
      case Right(_) =>
        startEmailVerification(details.emailAddress, credId)
    }

  private def startEmailVerification(email: String, credId: CredentialId)
                                    (using HeaderCarrier, Messages): Future[Either[ErrorModel, EnterEmailOutcome]] = {
    val evRequest = startEmailVerificationJourneyHelper.createRequest(credId.value, email)

    emailVerificationConnector.startEmailVerification(evRequest).map {
      case Left(error) =>
        logError(START_VERIFICATION_FAILED_MESSAGE, error)
      case Right(redirectUri) =>
        Right(EnterEmailOutcome.VerificationStarted(s"${config.emailVerificationRedirectBaseUrl}${redirectUri.redirectUri}"))
    }
  }

  private def logError(message: String, error: ErrorModel): Either[ErrorModel, EnterEmailOutcome] = {
    logger.warn(s"$message Status: ${error.status} Message: ${error.message}")
    Left(error)
  }
}
