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

import com.google.inject.{Inject, Singleton}
import models.emailverification.ErrorModel
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.ObligationDetails
import models.requests.returns.ReturnsDataRequest
import models.returns.ReturnsUserAnswers
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.i18n.Messages
import queries.returns.ObligationsQuery
import uk.gov.hmrc.http.HeaderCarrier
import utils.ReturnsDateUtils
import viewmodels.returns.submit.DeclarationViewModel

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeclarationService @Inject()(
                                    obligationService: ObligationService,
                                    returnsUserAnswersService: ReturnsUserAnswersService,
                                    submitReturnService: SubmitReturnService,
                                    returnsDateUtils: ReturnsDateUtils
                                  )(using ExecutionContext) extends Logging {

  private val SAVE_OBLIGATIONS_FAILED_MESSAGE = "[DeclarationService][buildViewModel] Failed to save obligations in user answers."
  private val SUBMIT_RETURN_FAILED_MESSAGE    = "[DeclarationService][submit] Failed to submit return."
  
  // Saves the live obligations in the user answers so submit does not have to wait on the Obligations API.
  // An error response is logged and the page still renders; any other failure is left to the framework's error handler.
  def buildViewModel(vpdId: VpdId, periodKey: PeriodKey, userAnswers: ReturnsUserAnswers)
                    (using hc: HeaderCarrier, messages: Messages): Future[DeclarationViewModel] =
    for {
      obligations <- obligationService.getObligations(vpdId)
      _           <- saveObligations(obligations, userAnswers)
    } yield DeclarationViewModel(periodKey, returnsDateUtils.getPeriodDisplay(periodKey, obligations))

  // Uses the obligations saved by buildViewModel, falling back to the live call if nothing has been saved.
  // On success the user answers are cleared; any failure is logged and returned as a Left.
  def submit(vpdId: VpdId, periodKey: PeriodKey, userAnswers: ReturnsUserAnswers)
            (using request: ReturnsDataRequest[?], hc: HeaderCarrier): Future[Either[ErrorModel, Unit]] =
    (for {
      obligations <- obligationService.getObligations(userAnswers, vpdId)
      _           <- submitReturnService.submit(userAnswers, obligations)
      _           <- returnsUserAnswersService.clear(vpdId, periodKey)
    } yield Right(())).recover { case e =>
      logger.error(s"$SUBMIT_RETURN_FAILED_MESSAGE Message: ${e.getMessage}", e)
      Left(ErrorModel(INTERNAL_SERVER_ERROR, SUBMIT_RETURN_FAILED_MESSAGE))
    }

  private def saveObligations(obligations: Seq[ObligationDetails], userAnswers: ReturnsUserAnswers)
                             (using hc: HeaderCarrier): Future[Unit] =
    Future.fromTry(userAnswers.set(ObligationsQuery, obligations))
      .flatMap(returnsUserAnswersService.set)
      .map {
        case Right(_)    => ()
        case Left(error) => logger.warn(s"$SAVE_OBLIGATIONS_FAILED_MESSAGE Status: ${error.status} Message: ${error.message}")
      }
}
