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

import models.identifiers.PeriodKey
import models.obligations.{ObligationDetails, ObligationsResponse}
import play.api.i18n.Messages

import java.time.{Clock, LocalDate, Month, Year}
import javax.inject.{Inject, Singleton}

@Singleton
class ReturnsDateUtils @Inject()(clock: Clock) {
  // scalafix:off DisableSyntax.throw
  def month: Month = LocalDate.now(clock).getMonth

  def getYear: Int =
    LocalDate.now(clock).getYear

  def getMonthLength(month: Month): Int = {
    val isLeapYear = Year.of(getYear).isLeap
    month.length(isLeapYear)
  }

  def getReturnMonth(month: Month)(implicit messages: Messages): String = {
    getMonthMessage(month)
  }

  def getDueDate(month: Month)(implicit messages: Messages): String = {
    getMonthMessage(month)
  }

  def getCurrentDay(implicit messages: Messages): String = {
    LocalDate.now(clock).getDayOfMonth.toString
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
      throw new IllegalArgumentException(s"Invalid month number: $month. Must be between 1 and 12.")
  }

  def formatPeriodDisplay(
                           periodKey: PeriodKey,
                           obligations: ObligationsResponse
                         )(implicit messages: Messages): String = {
    val obligationDetails = obligations.obligation.map(_.obligationDetails)
    formatPeriodDisplay(periodKey, obligationDetails)
  }

  def formatPeriodDisplay(
                           periodKey: PeriodKey,
                           obligationDetails: Seq[ObligationDetails]
                         )(implicit messages: Messages): String = {
    obligationDetails
      .find(_.periodKey == periodKey.toString)
      .map { obligation =>
        val month = obligation.iCFromDate.getMonthValue
        val year = obligation.iCFromDate.getYear
        val monthKey = getMonthMessageKey(month)
        s"${messages(monthKey)} $year"
      }
      .getOrElse {
        val availableKeys = if (obligationDetails.isEmpty) {
          "none"
        } else {
          obligationDetails.map(_.periodKey).mkString(", ")
        }
        throw new IllegalStateException(
          s"Period key '${periodKey.value}' not found in obligations. " +
            s"Available period keys: $availableKeys"
        )
      }
  }
}
