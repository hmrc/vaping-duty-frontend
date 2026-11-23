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
import models.identifiers.VpdId
import models.obligations.ObligationDetails
import models.returns.ReturnsUserAnswers
import play.api.Logging
import queries.returns.ObligationsQuery
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

// Saves obligations in the user answers so later pages do not have to wait on the Obligations API.
@Singleton
class CachedObligationsService @Inject()(
                                          obligationService: ObligationService,
                                          returnsUserAnswersService: ReturnsUserAnswersService
                                        )(using ExecutionContext) extends Logging {

  private val SAVE_OBLIGATIONS_FAILED_MESSAGE = "[CachedObligationsService][saveObligations] Failed to save obligations in user answers."

  // Uses the obligations saved in the user answers when present, falling back to the Obligations API.
  def getObligations(userAnswers: ReturnsUserAnswers, vpdId: VpdId)(using HeaderCarrier): Future[Seq[ObligationDetails]] =
    userAnswers.get(ObligationsQuery) match {
      case Some(obligations) => Future.successful(obligations)
      case None              => obligationService.getObligations(vpdId)
    }

  // An error response is logged and treated as success; any other failure is returned as a failed Future.
  def saveObligations(obligations: Seq[ObligationDetails], userAnswers: ReturnsUserAnswers)(using HeaderCarrier): Future[Unit] =
    Future.fromTry(userAnswers.set(ObligationsQuery, obligations))
      .flatMap(returnsUserAnswersService.set)
      .map {
        case Right(_)    => ()
        case Left(error) => logger.warn(s"$SAVE_OBLIGATIONS_FAILED_MESSAGE Status: ${error.status} Message: ${error.message}")
      }
}
