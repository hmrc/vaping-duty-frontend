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

import cats.data.ValidatedNec
import cats.implicits.*
import models.returns.ReturnsUserAnswers
import models.returns.validation.ValidationError
import pages.returns.{DeclareDutyPage, EnterDutyAmountPage}

import javax.inject.Inject

class UserAnswersValidationService @Inject()() {

  def validateDeclareDuty(answers: ReturnsUserAnswers): ValidatedNec[ValidationError, Unit] = {
    val declareDutyValidation = validateDeclareDutyAnswer(answers)
    val amountValidation = validateDutyAmount(answers)

    (declareDutyValidation, amountValidation).mapN((_, _) => ())
  }

  private def validateDeclareDutyAnswer(answers: ReturnsUserAnswers): ValidatedNec[ValidationError, Boolean] =
    answers.get(DeclareDutyPage) match {
      case Some(value) => value.validNec
      case None        => ValidationError.DeclareDutyAnswerMissing.invalidNec
    }

  private def validateDutyAmount(answers: ReturnsUserAnswers): ValidatedNec[ValidationError, Unit] =
    answers.get(DeclareDutyPage) match {
      case Some(true) =>
        answers.get(EnterDutyAmountPage) match {
          case Some(amount) if amount > 0  => ().validNec
          case Some(amount)                => ValidationError.DutyAmountInvalid(amount).invalidNec
          case None                        => ValidationError.DutyAmountMissing.invalidNec
        }
      case _ => ().validNec // Not declaring duty, so amount not required
    }

  // Helper methods for convenience
  def isDeclareDutyValid(answers: ReturnsUserAnswers): Boolean =
    validateDeclareDuty(answers).isValid

  def getDeclareDutyErrors(answers: ReturnsUserAnswers): List[ValidationError] =
    validateDeclareDuty(answers).fold(_.toList, _ => Nil)
}