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

import models.returns.{ConfigDutyRate, DutyRateValidationError}
import DutyRateValidationError.*

import java.time.{Clock, LocalDate}
import javax.inject.Inject

class DutyRateValidator @Inject()(clock: Clock) {

  def validateNonEmpty(rates: Seq[ConfigDutyRate]): Either[List[DutyRateValidationError], Seq[ConfigDutyRate]] =
    if (rates.nonEmpty) Right(rates)
    else Left(List(EmptyRates))

  def validatePositiveRates(rates: Seq[ConfigDutyRate]): Either[List[DutyRateValidationError], Seq[ConfigDutyRate]] = {
    val invalidRates = rates.collect {
      case rate if rate.ratePencePer10Ml <= 0 => NegativeRate(rate)
    }
    invalidRates match {
      case Nil    => Right(rates)
      case errors => Left(errors.toList)
    }
  }

  def validateDateRanges(rates: Seq[ConfigDutyRate]): Either[List[DutyRateValidationError], Seq[ConfigDutyRate]] = {
    val invalidRanges = rates.collect {
      case rate if rate.period.end.isBefore(rate.period.start) => InvalidDateRange(rate)
    }
    invalidRanges match {
      case Nil    => Right(rates)
      case errors => Left(errors.toList)
    }
  }

  def validateNoGapsOrOverlaps(rates: Seq[ConfigDutyRate]): Either[List[DutyRateValidationError], Seq[ConfigDutyRate]] =
    if (rates.size <= 1) {
      Right(rates)
    } else {
      val sortedRates = rates.sortBy(_.period.start)
      val gaps = sortedRates.sliding(2).collect {
        case Seq(current, next) if !current.period.end.plusDays(1).isEqual(next.period.start) =>
          GapOrOverlap(current, next)
      }.toList
      
      if (gaps.isEmpty) Right(sortedRates)
      else Left(gaps)
    }

  def validateCurrentDateCovered(
                                  rates: Seq[ConfigDutyRate],
                                  date: LocalDate = LocalDate.now(clock)
  ): Either[List[DutyRateValidationError], Seq[ConfigDutyRate]] =
    if (rates.exists(_.isValidFor(date))) Right(rates)
    else Left(List(CurrentDateNotCovered(date)))

  def validate(rates: Seq[ConfigDutyRate]): Either[List[DutyRateValidationError], Seq[ConfigDutyRate]] = {
    val allErrors = List(
      validateNonEmpty(rates),
      validatePositiveRates(rates),
      validateDateRanges(rates),
      validateNoGapsOrOverlaps(rates),
      validateCurrentDateCovered(rates)
    ).collect { case Left(errors) => errors }.flatten
    
    if (allErrors.isEmpty) {
      Right(rates.sortBy(_.period.start))
    } else {
      Left(allErrors)
    }
  }
}