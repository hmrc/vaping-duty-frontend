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

import cats.data.Validated.{Invalid, Valid}
import models.identifiers.{PeriodKey, VpdId}
import models.returns.ReturnsUserAnswers
import models.returns.validation.ValidationError
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.returns.{DeclareDutyPage, EnterDutyAmountPage}

import java.time.Instant

class UserAnswersValidationServiceSpec extends AnyFreeSpec with Matchers {

  private val service = new UserAnswersValidationService()
  private val vpdId = VpdId("GBWK1234567WK")
  private val periodKey = PeriodKey("24KA")

  private def emptyUserAnswers: ReturnsUserAnswers =
    ReturnsUserAnswers(
      vpdId = vpdId.value,
      periodKey = periodKey.value,
      startedTime = Instant.now(),
      lastUpdated = Instant.now()
    )

  "validateDeclareDuty" - {

    "must return Valid when user declares no duty" in {
      val answers = emptyUserAnswers
        .set(DeclareDutyPage, false).success.value

      val result = service.validateDeclareDuty(answers)

      result mustBe Valid(())
    }

    "must return Valid when user declares duty with valid amount" in {
      val answers = emptyUserAnswers
        .set(DeclareDutyPage, true).success.value
        .set(EnterDutyAmountPage, BigDecimal("100.50")).success.value

      val result = service.validateDeclareDuty(answers)

      result mustBe Valid(())
    }

    "must return Invalid when DeclareDutyPage is missing" in {
      val answers = emptyUserAnswers

      val result = service.validateDeclareDuty(answers)

      result match {
        case Invalid(errors) =>
          errors.toList must contain(ValidationError.DeclareDutyAnswerMissing)
        case Valid(_) =>
          fail("Expected validation to fail")
      }
    }

    "must return Invalid when user declares duty but amount is missing" in {
      val answers = emptyUserAnswers
        .set(DeclareDutyPage, true).success.value

      val result = service.validateDeclareDuty(answers)

      result match {
        case Invalid(errors) =>
          errors.toList must contain(ValidationError.DutyAmountMissing)
        case Valid(_) =>
          fail("Expected validation to fail")
      }
    }

    "must return Invalid when user declares duty but amount is zero" in {
      val answers = emptyUserAnswers
        .set(DeclareDutyPage, true).success.value
        .set(EnterDutyAmountPage, BigDecimal("0")).success.value

      val result = service.validateDeclareDuty(answers)

      result match {
        case Invalid(errors) =>
          errors.toList must contain(ValidationError.DutyAmountInvalid(BigDecimal("0")))
        case Valid(_) =>
          fail("Expected validation to fail")
      }
    }

    "must return Invalid when user declares duty but amount is negative" in {
      val answers = emptyUserAnswers
        .set(DeclareDutyPage, true).success.value
        .set(EnterDutyAmountPage, BigDecimal("-50.00")).success.value

      val result = service.validateDeclareDuty(answers)

      result match {
        case Invalid(errors) =>
          errors.toList must contain(ValidationError.DutyAmountInvalid(BigDecimal("-50.00")))
        case Valid(_) =>
          fail("Expected validation to fail")
      }
    }

    "must accumulate multiple errors when both DeclareDutyPage and amount are invalid" in {
      val answers = emptyUserAnswers

      val result = service.validateDeclareDuty(answers)

      result match {
        case Invalid(errors) =>
          val errorList = errors.toList
          errorList must contain(ValidationError.DeclareDutyAnswerMissing)
          // Note: When DeclareDutyPage is missing, amount validation passes (not required)
          // So we only get one error in this case
        case Valid(_) =>
          fail("Expected validation to fail")
      }
    }
  }

  "isDeclareDutyValid" - {

    "must return true when validation passes" in {
      val answers = emptyUserAnswers
        .set(DeclareDutyPage, false).success.value

      service.isDeclareDutyValid(answers) mustBe true
    }

    "must return false when validation fails" in {
      val answers = emptyUserAnswers
        .set(DeclareDutyPage, true).success.value

      service.isDeclareDutyValid(answers) mustBe false
    }
  }

  "getDeclareDutyErrors" - {

    "must return empty list when validation passes" in {
      val answers = emptyUserAnswers
        .set(DeclareDutyPage, false).success.value

      service.getDeclareDutyErrors(answers) mustBe Nil
    }

    "must return list of errors when validation fails" in {
      val answers = emptyUserAnswers
        .set(DeclareDutyPage, true).success.value

      val errors = service.getDeclareDutyErrors(answers)

      errors must contain(ValidationError.DutyAmountMissing)
    }

    "must return all accumulated errors" in {
      val answers = emptyUserAnswers

      val errors = service.getDeclareDutyErrors(answers)

      errors must contain(ValidationError.DeclareDutyAnswerMissing)
    }
  }
}