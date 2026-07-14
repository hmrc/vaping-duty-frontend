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

package services.returns

import connectors.returns.SubmitReturnConnector
import models.obligations.ObligationDetails
import models.requests.returns.ReturnsDataRequest
import models.returns.*
import models.returns.submit.ReturnSubmittedResponse
import services.contactPreference.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitReturnService @Inject()(
  submitReturnConnector: SubmitReturnConnector,
  dutyRateService: DutyRateService,
  obligationService: ObligationService,
  buildReturnSubmissionService: BuildReturnSubmissionService,
  auditService: AuditService,
)(using ExecutionContext) {

  def submit(ua: ReturnsUserAnswers)(implicit request: ReturnsDataRequest[?]): Future[ReturnSubmittedResponse] = {

    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(session = request.session, request = request.request)

    for {
      obligations <- obligationService.getObligationsDirectly(request.enrolmentVpdId)
      periodKeyToDutyRate = dutyRateService.getDutyRatesForPeriodKeys(obligations)
      obligationOpt = obligations.find(_.periodKey == request.periodKey.toString)
      obligation <- obligationOpt match {
        case Some(obl) => Future.successful(obl)
        case None => Future.failed(new IllegalStateException(s"No obligation found for period key: ${ua.periodKey}"))
      }
      submission = buildReturnSubmissionService.buildSubmission(ua, obligation, request.enrolmentVpdId, periodKeyToDutyRate)
      result <- submitReturnConnector.submitReturn(submission, request.enrolmentVpdId)
    } yield {
      auditService.auditReturnSubmitted(
        SubmitReturnAuditEvent.buildExplicitAuditEvent(submission, result, request.identifiers, obligations))
 
      result
    }
  }
}
