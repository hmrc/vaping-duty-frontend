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

import config.FrontendAppConfig
import connectors.returns.SubmitReturnConnector
import models.obligations.ObligationDetails
import models.requests.returns.ReturnsDataRequest
import models.returns.*
import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import play.api.Logging
import play.api.libs.json.Json
import services.contactPreference.AuditService
import services.email.ReturnSubmittedEmailService
import services.nrs.NrsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitReturnService @Inject()(
                                     submitReturnConnector: SubmitReturnConnector,
                                     dutyRateService: DutyRateService,
                                     buildReturnSubmissionService: BuildReturnSubmissionService,
                                     auditService: AuditService,
                                     returnSubmittedEmailService: ReturnSubmittedEmailService,
                                     nrsService: NrsService,
                                     appConfig: FrontendAppConfig,
                                   )(using ExecutionContext) extends Logging {

  def submit(ua: ReturnsUserAnswers, obligations: Seq[ObligationDetails])
            (implicit request: ReturnsDataRequest[?]): Future[ReturnSubmittedResponse] = {

    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(session = request.session, request = request.request)

    val periodKeyToDutyRate = dutyRateService.getDutyRatesForPeriodKeys(obligations)

    obligations.find(_.periodKey == request.periodKey.toString) match {
      case Some(obligation) =>
        val submission = buildReturnSubmissionService.buildSubmission(ua, obligation, request.enrolmentVpdId, periodKeyToDutyRate)

        submitReturnConnector.submitReturn(submission, request.enrolmentVpdId).map { result =>

          submitNrs(submission)

          auditService.auditReturnSubmitted(
            SubmitReturnAuditEvent.buildExplicitAuditEvent(submission, result, request.identifiers, obligations)
          )

          if (appConfig.returnSubmittedEmailEnabled) {
            returnSubmittedEmailService.sendReturnSubmittedEmail(request.enrolmentVpdId, submission, result, obligation)
          }

          result
        }

      case None =>
        Future.failed(new IllegalStateException(s"No obligation found for period key: ${ua.periodKey}"))
    }
  }

  private def submitNrs(submission: ReturnCreateRequest)(using request: ReturnsDataRequest[?], hc: HeaderCarrier) = {
    nrsService.makeWorkItemAndQueue(
      Json.toJson(submission),
      "vpd-return-submitted",
      request.periodKey.value,
      request.enrolmentVpdId
    ).recover { ex =>
      logger.error("[SubmitReturnService] Failed to prepare or queue NRS submission", ex)
      ()
    }
  }
}
