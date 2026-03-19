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

package services

import connectors.SubmitPreferencesConnector
import models.audit.JourneyOutcome
import models.contactPreference
import models.contactPreference.PaperlessPreference
import models.emailverification.{PaperlessPreferenceSubmission, PaperlessPreferenceSubmittedResponse}
import models.requests.DataRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PerformSubmissionService @Inject()(submitPreferencesConnector: SubmitPreferencesConnector,
                                         auditService: AuditService)
                                        (implicit ec: ExecutionContext) {

  def submit(preferenceSubmission: PaperlessPreferenceSubmission, request: DataRequest[?]): Future[ResponseStatus] = {

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(session = request.session, request = request.request)

    submitPreferencesConnector.submitContactPreferences(preferenceSubmission, request.enrolmentVpdId).map {
      case Left(error)     => new Failure
      case Right(response) =>
        sendExplicitEvent(preferenceSubmission, auditService)(hc, request)
        new Success
    }
  }
}

private def sendExplicitEvent(preferenceSubmission: PaperlessPreferenceSubmission, auditService: AuditService)
                             (implicit hc: HeaderCarrier, request: DataRequest[?]): Unit = {

  val address = request.userAnswers.subscriptionSummary.correspondenceAddress.replace("\n", ", ")

  auditService.audit(
    JourneyOutcome.buildEvent(
      preferenceSubmission,
      PaperlessPreference(request.userAnswers.subscriptionSummary.paperlessPreference),
      address
    )
  )
}

class ResponseStatus

class Success extends ResponseStatus

class Failure extends ResponseStatus
