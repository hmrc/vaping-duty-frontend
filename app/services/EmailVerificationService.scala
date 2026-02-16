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

import cats.data.EitherT
import com.google.inject.Singleton
import connectors.{EmailVerificationConnector, SubmitPreferencesConnector}
import models.UserAnswers
import models.contactPreference.PaperlessPreference.{Email, toValue}
import models.contactPreference.PerformSubmission
import models.emailverification.*
import models.requests.DataRequest
import play.api.Logging
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationService @Inject() (emailVerificationConnector: EmailVerificationConnector)
                                         (implicit ec: ExecutionContext) extends Logging {

  def retrieveAddressStatus(verificationDetails: VerificationDetails,
                            emailAddress: String,
                            userAnswers: UserAnswers)
                           (implicit hc: HeaderCarrier): EitherT[Future, ErrorModel, EmailVerificationDetails] =
    for {
      successResponse    <- emailVerificationConnector.getEmailVerification(verificationDetails)
      verificationDetails = handleSuccess(emailAddress, successResponse)
    } yield verificationDetails
  

  private def handleSuccess(emailAddress: String, successResponse: GetVerificationStatusResponse): EmailVerificationDetails = {

    val isEmailVerified: Boolean =
      successResponse.emails.exists(email => email.emailAddress.equalsIgnoreCase(emailAddress) && email.verified)
    val isEmailLocked: Boolean   =
      successResponse.emails.exists(email => email.emailAddress.equalsIgnoreCase(emailAddress) && email.locked)

    EmailVerificationDetails(emailAddress = emailAddress, isVerified = isEmailVerified, isLocked = isEmailLocked)

  }

  def redirectIfLocked(result: Future[Result], isLocked: Boolean): Future[Result] = {
    if (isLocked) {
      Future.successful(Redirect(controllers.contactPreference.routes.LockedEmailController.onPageLoad()))
    } else {
      result
    }
  }

  def submitVerifiedEmail(email: String, verified: Boolean, submitPreferencesConnector: SubmitPreferencesConnector)
                         (implicit hc: HeaderCarrier, request: DataRequest[?]): Future[Result] = {
    
    if (verified) {
      PerformSubmission(
        submitPreferencesConnector,
        PaperlessPreferenceSubmission(
          paperlessPreference = toValue(Email),
          emailAddress = Some(email),
          emailVerification = Some(verified),
          bouncedEmail = None
        ),
        Email
      ).getResult
    } else {
      // Should never enter this case
      logger.warn("[EmailVerificationService][submit] Unverified email attempted to submit")
      Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
