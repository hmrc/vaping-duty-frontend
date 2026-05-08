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

package utils

import java.time.Month
import java.time.Month._
import java.time.format.TextStyle
import java.util.Locale

object PeriodKeys {
  def fromDisplayName(name: String): Option[Month] =
    Month.values().find(_.getDisplayName(TextStyle.FULL, Locale.ENGLISH) == name)

  def toEtmpMonthString(month: Month): String = month match {
    case JANUARY   => "AA"
    case FEBRUARY  => "AB"
    case MARCH     => "AC"
    case APRIL     => "AD"
    case MAY       => "AE"
    case JUNE      => "AF"
    case JULY      => "AG"
    case AUGUST    => "AH"
    case SEPTEMBER => "AI"
    case OCTOBER   => "AJ"
    case NOVEMBER  => "AK"
    case DECEMBER  => "AL"
  }

  def fromEtmpMonthString(monthKey: String): Option[Month] = monthKey match {
    case "AA" => Some(JANUARY)
    case "AB" => Some(FEBRUARY)
    case "AC" => Some(MARCH)
    case "AD" => Some(APRIL)
    case "AE" => Some(MAY)
    case "AF" => Some(JUNE)
    case "AG" => Some(JULY)
    case "AH" => Some(AUGUST)
    case "AI" => Some(SEPTEMBER)
    case "AJ" => Some(OCTOBER)
    case "AK" => Some(NOVEMBER)
    case "AL" => Some(DECEMBER)
    case _    => None
  }

  def toDisplayName(month: Month): String =
    month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
}