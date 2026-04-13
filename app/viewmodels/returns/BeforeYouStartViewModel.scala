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

package viewmodels.returns

import play.api.i18n.Messages

import java.time.{LocalDate, Month, Year}

case class BeforeYouStartViewModel(returnPeriod: String, dueDate: String, monthLength: Int, year: Int)

object BeforeYouStartViewModel {

  val month: Month = LocalDate.now().getMonth

  def apply()(implicit messages: Messages): BeforeYouStartViewModel =
    beforeYouStartViewModel()

  private def beforeYouStartViewModel()(implicit messages: Messages) =
    new BeforeYouStartViewModel(getReturnPeriod(), getDueDate(), monthLength, getYear)

  private def getReturnPeriod()(implicit messages: Messages): String = {
    getCurrentMonthMessage(month)
  }

  private def getDueDate()(implicit messages: Messages): String = {
    getCurrentMonthMessage(month.plus(1))
  }

  private def getYear: Int =
    LocalDate.now().getYear

  private def monthLength: Int =
    val isLeapYear = Year.of(getYear).isLeap
    month.length(isLeapYear)

  private def getCurrentMonthMessage(month: Month)(implicit messages: Messages): String =
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
}
