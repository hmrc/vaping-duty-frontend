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

import models.returns.{AdjustmentsEligibility, ReturnsUserAnswers}
import pages.returns.DeclareSpoiltProductsPage
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaskListPreparationService @Inject()(repository: ReturnsUserAnswersService)(using ExecutionContext) {

  def prepareUserAnswers(
                          userAnswers: ReturnsUserAnswers,
                          adjustmentsEligibility: AdjustmentsEligibility
                        )(using HeaderCarrier): Future[ReturnsUserAnswers] = {

    val updatedUa = updateUserAnswers(userAnswers, adjustmentsEligibility)

    storeUserAnswersIfChanged(userAnswers, updatedUa)
  }

  def storeUserAnswersIfChanged(userAnswers: ReturnsUserAnswers,
                                updatedUa: ReturnsUserAnswers)(using HeaderCarrier): Future[ReturnsUserAnswers] = {
    if (updatedUa != userAnswers)
      repository.set(updatedUa).map(_ => updatedUa)
    else
      Future.successful(userAnswers)
  }

  def updateUserAnswers(userAnswers: ReturnsUserAnswers,
                        adjustmentsEligibility: AdjustmentsEligibility)
                       (using HeaderCarrier): ReturnsUserAnswers = {
    adjustmentsEligibility match {
      case AdjustmentsEligibility.NotEligible => autoSetDeclareSpoiltProducts(userAnswers, answer = false)
      case AdjustmentsEligibility.Eligible => userAnswers
    }
  }

  private def autoSetDeclareSpoiltProducts(
                                            userAnswers: ReturnsUserAnswers,
                                            answer: Boolean
                                          )(using HeaderCarrier): ReturnsUserAnswers = {

    userAnswers.get(DeclareSpoiltProductsPage) match {
      case Some(value) if value == answer => userAnswers
      case _ =>
        userAnswers.set(DeclareSpoiltProductsPage, answer).fold(
          _  => userAnswers,  // Failure to marshall to JSON, unlikely to happen
          ua => ua
        )
    }
  }
}
