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

package config

import base.SpecBase
import models.returns.{DutyRate, DutyRateValidationError}
import DutyRateValidationError.*

import java.time.LocalDate

class DutyRateValidatorSpec extends SpecBase {

  val validator = new DutyRateValidator(clock)
  
  private val validRate1 = DutyRate(
    period = models.returns.DateRange(
      start = LocalDate.of(2026, 1, 1),
      end = LocalDate.of(2026, 12, 31)
    ),
    ratePencePer10Ml = 220
  )

  private val validRate2 = DutyRate(
    period = models.returns.DateRange(
      start = LocalDate.of(2027, 1, 1),
      end = LocalDate.of(9999, 12, 31)
    ),
    ratePencePer10Ml = 300
  )

  "validateNonEmpty" - {

    "must return Right for a non-empty sequence" in {
      val rates = Seq(validRate1)

      val result = validator.validateNonEmpty(rates)

      result mustBe Right(rates)
    }

    "must return Left(EmptyRates) for an empty sequence" in {
      val rates = Seq.empty[DutyRate]

      val result = validator.validateNonEmpty(rates)

      result mustBe Left(List(EmptyRates))
    }
  }

  "validatePositiveRates" - {

    "must return Right when all rates are positive" in {
      val rates = Seq(validRate1, validRate2)

      val result = validator.validatePositiveRates(rates)

      result mustBe Right(rates)
    }

    "must return Left(NegativeRate) when a rate is zero" in {
      val zeroRate = validRate1.copy(ratePencePer10Ml = 0)
      val rates = Seq(zeroRate)

      val result = validator.validatePositiveRates(rates)

      result mustBe Left(List(NegativeRate(zeroRate)))
    }

    "must return Left(NegativeRate) when a rate is negative" in {
      val negativeRate = validRate1.copy(ratePencePer10Ml = -100)
      val rates = Seq(negativeRate)

      val result = validator.validatePositiveRates(rates)

      result mustBe Left(List(NegativeRate(negativeRate)))
    }

    "must return Left with all invalid rates when multiple rates are invalid" in {
      val zeroRate = validRate1.copy(ratePencePer10Ml = 0)
      val negativeRate = validRate2.copy(ratePencePer10Ml = -50)
      val rates = Seq(zeroRate, negativeRate)

      val result = validator.validatePositiveRates(rates)

      result mustBe Left(List(NegativeRate(zeroRate), NegativeRate(negativeRate)))
    }
  }

  "validateDateRanges" - {

    "must return Right when all date ranges are valid" in {
      val rates = Seq(validRate1, validRate2)

      val result = validator.validateDateRanges(rates)

      result mustBe Right(rates)
    }

    "must return Right when end date equals start date" in {
      val sameDate = LocalDate.of(2026, 6, 15)
      val rate = validRate1.copy(period = models.returns.DateRange(start = sameDate, end = sameDate))
      val rates = Seq(rate)

      val result = validator.validateDateRanges(rates)

      result mustBe Right(rates)
    }

    "must return Left(InvalidDateRange) when end date is before start date" in {
      val invalidRate = validRate1.copy(
        period = models.returns.DateRange(
          start = LocalDate.of(2026, 12, 31),
          end = LocalDate.of(2026, 1, 1)
        )
      )
      val rates = Seq(invalidRate)

      val result = validator.validateDateRanges(rates)

      result mustBe Left(List(InvalidDateRange(invalidRate)))
    }
  }

  "validateNoGapsOrOverlaps" - {

    "must return Right for a single rate" in {
      val rates = Seq(validRate1)

      val result = validator.validateNoGapsOrOverlaps(rates)

      result mustBe Right(rates)
    }

    "must return Right for consecutive rates with no gaps" in {
      val rates = Seq(validRate1, validRate2)

      val result = validator.validateNoGapsOrOverlaps(rates)

      result mustBe Right(rates)
    }

    "must return Right and sort rates by start date" in {
      val rates = Seq(validRate2, validRate1)

      val result = validator.validateNoGapsOrOverlaps(rates)

      result mustBe Right(Seq(validRate1, validRate2))
    }

    "must return Left(GapOrOverlap) when there is a gap between periods" in {
      val rate1 = validRate1.copy(period = validRate1.period.copy(end = LocalDate.of(2026, 6, 30)))
      val rate2 = validRate2.copy(period = validRate2.period.copy(start = LocalDate.of(2026, 7, 2)))
      val rates = Seq(rate1, rate2)

      val result = validator.validateNoGapsOrOverlaps(rates)

      result mustBe Left(List(GapOrOverlap(rate1, rate2)))
    }

    "must return Left(GapOrOverlap) when there is an overlap between periods" in {
      val rate1 = validRate1.copy(period = validRate1.period.copy(end = LocalDate.of(2026, 7, 1)))
      val rate2 = validRate2.copy(period = validRate2.period.copy(start = LocalDate.of(2026, 7, 1)))
      val rates = Seq(rate1, rate2)

      val result = validator.validateNoGapsOrOverlaps(rates)

      result mustBe Left(List(GapOrOverlap(rate1, rate2)))
    }
  }

  "validateCurrentDateCovered" - {

    "must return Right when the current date is covered" in {
      val today = LocalDate.now(clock)
      val rate = validRate1.copy(
        period = models.returns.DateRange(
          start = today.minusDays(10),
          end = today.plusDays(10)
        )
      )
      val rates = Seq(rate)

      val result = validator.validateCurrentDateCovered(rates, today)

      result mustBe Right(rates)
    }

    "must return Left(CurrentDateNotCovered) when the current date is not covered" in {
      val today = LocalDate.now(clock)
      val rate = validRate1.copy(
        period = models.returns.DateRange(
          start = today.minusYears(2),
          end = today.minusYears(1)
        )
      )
      val rates = Seq(rate)

      val result = validator.validateCurrentDateCovered(rates, today)

      result mustBe Left(List(CurrentDateNotCovered(today)))
    }
  }

  "validate (composed)" - {

    "must return Right for fully valid rates" in {
      val today = LocalDate.now(clock)
      val rate1 = validRate1.copy(
        period = models.returns.DateRange(
          start = today.minusDays(100),
          end = today.plusDays(100)
        )
      )
      val rate2 = validRate2.copy(
        period = models.returns.DateRange(
          start = today.plusDays(101),
          end = today.plusYears(1)
        )
      )
      val rates = Seq(rate1, rate2)

      val result = validator.validate(rates)

      result.isRight mustBe true
    }

    "must return Left(EmptyRates) when rates are empty" in {
      val rates = Seq.empty[DutyRate]
      val today = LocalDate.now(clock)

      val result = validator.validate(rates)

      result.isLeft mustBe true
      result.left.map { errors =>
        errors must contain(EmptyRates)
        errors must contain(CurrentDateNotCovered(today))
      }

      result mustBe Left(List(EmptyRates, CurrentDateNotCovered(today)))

    }

    "must return Left(NegativeRate) when a rate is invalid" in {
      val today = LocalDate.now(clock)
      val invalidRate = validRate1.copy(
        period = models.returns.DateRange(
          start = today.minusDays(10),
          end = today.plusDays(10)
        ),
        ratePencePer10Ml = -50
      )
      val rates = Seq(invalidRate)

      val result = validator.validate(rates)

      result mustBe Left(List(NegativeRate(invalidRate)))
    }

    "must return Left(InvalidDateRange) when date range is invalid" in {
      val today = LocalDate.now(clock)
      val invalidRate = validRate1.copy(
        period = models.returns.DateRange(
          start = today.plusDays(10),
          end = today.minusDays(10)
        ),
        ratePencePer10Ml = 250
      )
      val rates = Seq(invalidRate)

      val result = validator.validate(rates)

      result.isLeft mustBe true

      result mustBe Left(List(InvalidDateRange(invalidRate), CurrentDateNotCovered(today)))
    }

    "must return Left(GapOrOverlap) when there is a gap" in {
      val today = LocalDate.now(clock)
      val rate1 = validRate1.copy(
        period = models.returns.DateRange(
          start = today.minusDays(100),
          end = today.minusDays(50)
        )
      )
      val rate2 = validRate2.copy(
        period = models.returns.DateRange(
          start = today.minusDays(48), // Gap of 1 day
          end = today.plusDays(100)
        )
      )
      val rates = Seq(rate1, rate2)

      val result = validator.validate(rates)

      result mustBe Left(List(GapOrOverlap(rate1, rate2)))
    }

    "must return Left(CurrentDateNotCovered) when current date is not covered" in {
      val today = LocalDate.now(clock)
      val rate = validRate1.copy(
        period = models.returns.DateRange(
          start = today.minusYears(2),
          end = today.minusYears(1)
        )
      )
      val rates = Seq(rate)

      val result = validator.validate(rates)

      result mustBe Left(List(CurrentDateNotCovered(today)))
    }

    "must return all errors when multiple validations fail" in {
      val today = LocalDate.now(clock)

      val rate1 = validRate1.copy(
        period = models.returns.DateRange(
          start = LocalDate.of(2026, 6, 1),
          end = LocalDate.of(2026, 6, 10)
        ),
        ratePencePer10Ml = -50
      )

      val rate2 = validRate2.copy(
        period = models.returns.DateRange(
          start = LocalDate.of(2026, 6, 15),
          end = LocalDate.of(2026, 6, 12)
        ),
        ratePencePer10Ml = 100
      )

      val rates = Seq(rate1, rate2)

      val result = validator.validate(rates)

      result.isLeft mustBe true

      result mustBe Left(List(
        NegativeRate(rate1),
        InvalidDateRange(rate2),
        GapOrOverlap(rate1, rate2),
        CurrentDateNotCovered(today)
      ))
    }
  }
}