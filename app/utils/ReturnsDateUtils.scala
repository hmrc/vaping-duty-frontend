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

import play.api.i18n.Messages

import java.time.{LocalDate, Month, Year}

object ReturnsDateUtils {

  val month: Month = LocalDate.now().getMonth

  def getYear: Int =
    LocalDate.now().getYear

  def getMonthLength(month: Month): Int =
    val isLeapYear = Year.of(getYear).isLeap
    month.length(isLeapYear)

  def getReturnMonth(month: Month)(implicit messages: Messages): String = {
    getMonthMessage(month)
  }

  def getDueDate(month: Month)(implicit messages: Messages): String = {
    getMonthMessage(month)
  }

  def getCurrentDay(implicit messages: Messages): String = {
    LocalDate.now().getDayOfMonth.toString
  }

  def getMonthMessage(month: Month)(implicit messages: Messages): String =
    month match {
      case Month.JANUARY => messages("month.jan")
      case Month.FEBRUARY => messages("month.feb")
      case Month.MARCH => messages("month.mar")
      case Month.APRIL => messages("month.apr")
      case Month.MAY => messages("month.may")
      case Month.JUNE => messages("month.jun")
      case Month.JULY => messages("month.jul")
      case Month.AUGUST => messages("month.aug")
      case Month.SEPTEMBER => messages("month.sep")
      case Month.OCTOBER => messages("month.oct")
      case Month.NOVEMBER => messages("month.nov")
      case Month.DECEMBER => messages("month.dec")
    }

  def getMonthMessageKey(month: Int): String = month match {
    case 1 => "month.jan"
    case 2 => "month.feb"
    case 3 => "month.mar"
    case 4 => "month.apr"
    case 5 => "month.may"
    case 6 => "month.jun"
    case 7 => "month.jul"
    case 8 => "month.aug"
    case 9 => "month.sep"
    case 10 => "month.oct"
    case 11 => "month.nov"
    case 12 => "month.dec"
    case _ =>
      // scalafix:off DisableSyntax.throw
      throw new IllegalArgumentException(s"Invalid month number: $month. Must be between 1 and 12.")
  }
}
