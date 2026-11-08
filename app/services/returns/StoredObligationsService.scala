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

import com.google.inject.Inject
import models.identifiers.VpdId
import models.obligations.ObligationDetails
import models.returns.ReturnsUserAnswers
import play.api.Logging
import queries.returns.ObligationsQuery
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class StoredObligationsService @Inject() (
                                           obligationService: ObligationService,
                                           sessionRepository: ReturnsUserAnswersService,
                                         )(using ExecutionContext) extends Logging {

  // Saves the obligations in the user answers so onSubmit does not have to wait on the Obligations API.
  // An error response is logged and the page still renders; any other failure is left to the framework's error handler.
  def savingObligations[T](vpdId: VpdId,
                           userAnswers: ReturnsUserAnswers)
                          (block: Seq[ObligationDetails] => T)
                          (using HeaderCarrier): Future[T] = {
    obligationService.getObligations(vpdId).flatMap { obligations =>
      saveObligations(userAnswers, obligations).map { _ =>
        block(obligations)
      }
    }
  }

  private def saveObligations(userAnswers: ReturnsUserAnswers, obligations: Seq[ObligationDetails])
                             (using HeaderCarrier): Future[Unit] =
    Future.fromTry(userAnswers.set(ObligationsQuery, obligations))
      .flatMap(sessionRepository.set)
      .map {
        case Right(_) => ()
        case Left(error) => logger.warn(s"Failed to save obligations in user answers: ${error.message}")
      }

  // Uses the obligations saved by onPageLoad so the submit doesn't wait on the Obligations API.
  // Falls back to the live call if nothing has been saved.
  def getObligations(vpdId: VpdId,
                     userAnswers: ReturnsUserAnswers)
                    (using HeaderCarrier): Future[Seq[ObligationDetails]] =
    userAnswers.get(ObligationsQuery) match {
      case Some(obligations) => Future.successful(obligations)
      case None              => obligationService.getObligations(vpdId)
    }

}
