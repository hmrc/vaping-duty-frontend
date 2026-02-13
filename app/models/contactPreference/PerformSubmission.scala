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

package models.contactPreference

import connectors.SubmitPreferencesConnector
import models.contactPreference
import models.contactPreference.PaperlessPreference.{Email, Post}
import models.emailverification.{PaperlessPreferenceSubmission, PaperlessPreferenceSubmittedResponse}
import models.requests.DataRequest
import play.api.i18n.Lang.logger
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

case class PerformSubmission(result: Future[Result]) {
  
  def getResult: Future[Result] = result
}

object PerformSubmission {
  
  def apply(submitPreferencesConnector: SubmitPreferencesConnector,
            preferenceSubmission: PaperlessPreferenceSubmission,
            updatedPreference: PaperlessPreference)
           (implicit ec: ExecutionContext, hc: HeaderCarrier, request: DataRequest[?]): PerformSubmission = {
    
    performSubmission(submitPreferencesConnector, preferenceSubmission, updatedPreference)
  }

  private def performSubmission(submitPreferencesConnector: SubmitPreferencesConnector,
                                preferenceSubmission: PaperlessPreferenceSubmission,
                                updatedPreference: PaperlessPreference)
                               (implicit ec: ExecutionContext, hc: HeaderCarrier, request: DataRequest[?])= {
    PerformSubmission(
      submitPreferencesConnector.submitContactPreferences(preferenceSubmission, request.vpdId).map {
        case Left(error) =>
          logger.info(s"[contactPreference.PerformSubmission] Error submitting preference: $error")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Right(response) =>
          updatedPreference match {
            case Email =>
              logSuccess(response)
              Redirect(controllers.contactPreference.routes.EmailConfirmationController.onPageLoad())
            case Post =>
              logSuccess(response)
              Redirect(controllers.contactPreference.routes.PostalConfirmationController.onPageLoad())
          }
      }
    )
  }
  
  private def logSuccess(response: PaperlessPreferenceSubmittedResponse): Unit = {
    logger.info(s"[PerformSubmission] Preference updated ${response.processingDate}")
  }
}