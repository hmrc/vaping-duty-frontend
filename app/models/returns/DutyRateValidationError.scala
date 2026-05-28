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

package models.returns

import java.time.LocalDate

sealed trait DutyRateValidationError {
  def message: String
}

object DutyRateValidationError {

  case object EmptyRates extends DutyRateValidationError {
    val message: String = "At least one duty rate must be configured"
  }

  final case class NegativeRate(rate: DutyRate) extends DutyRateValidationError {
    val message: String = s"Duty rate must be positive: ${rate.ratePencePerMl}"
  }

  final case class InvalidDateRange(rate: DutyRate) extends DutyRateValidationError {
    val message: String = s"End date must be after or equal to start date: ${rate.period.start} to ${rate.period.end}"
  }

  final case class GapOrOverlap(current: DutyRate, next: DutyRate) extends DutyRateValidationError {
    val message: String = s"Gap or overlap detected between periods: ${current.period.end} and ${next.period.start}"
  }

  final case class CurrentDateNotCovered(date: LocalDate) extends DutyRateValidationError {
    val message: String = s"Current date $date is not covered by any configured duty rate period"
  }
}