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

import models.returns.{DutyRate, DutyRateValidationError}
import DutyRateValidationError._

import java.time.LocalDate

object DutyRateValidator {

  def validateNonEmpty(rates: Seq[DutyRate]): Either[List[DutyRateValidationError], Seq[DutyRate]] =
    if (rates.nonEmpty) Right(rates)
    else Left(List(EmptyRates))

  def validatePositiveRates(rates: Seq[DutyRate]): Either[List[DutyRateValidationError], Seq[DutyRate]] = {
    val invalidRates = rates.collect {
      case rate if rate.ratePencePerMl <= 0 => NegativeRate(rate)
    }
    invalidRates match {
      case Nil    => Right(rates)
      case errors => Left(errors.toList)
    }
  }

  def validateDateRanges(rates: Seq[DutyRate]): Either[List[DutyRateValidationError], Seq[DutyRate]] = {
    val invalidRanges = rates.collect {
      case rate if rate.endDate.isBefore(rate.startDate) => InvalidDateRange(rate)
    }
    invalidRanges match {
      case Nil    => Right(rates)
      case errors => Left(errors.toList)
    }
  }

  def validateNoGapsOrOverlaps(rates: Seq[DutyRate]): Either[List[DutyRateValidationError], Seq[DutyRate]] =
    if (rates.size <= 1) {
      Right(rates)
    } else {
      val sortedRates = rates.sortBy(_.startDate)
      val gaps = sortedRates.sliding(2).collect {
        case Seq(current, next) if !current.endDate.plusDays(1).isEqual(next.startDate) =>
          GapOrOverlap(current, next)
      }.toList
      
      if (gaps.isEmpty) Right(sortedRates)
      else Left(gaps)
    }

  def validateCurrentDateCovered(
    rates: Seq[DutyRate],
    date: LocalDate = LocalDate.now()
  ): Either[List[DutyRateValidationError], Seq[DutyRate]] =
    if (rates.exists(_.isValidFor(date))) Right(rates)
    else Left(List(CurrentDateNotCovered(date)))

  def validate(rates: Seq[DutyRate]): Either[List[DutyRateValidationError], Seq[DutyRate]] = {
    val allErrors = List(
      validateNonEmpty(rates),
      validatePositiveRates(rates),
      validateDateRanges(rates),
      validateNoGapsOrOverlaps(rates),
      validateCurrentDateCovered(rates)
    ).collect { case Left(errors) => errors }.flatten
    
    if (allErrors.isEmpty) {
      Right(rates.sortBy(_.startDate))
    } else {
      Left(allErrors)
    }
  }
}