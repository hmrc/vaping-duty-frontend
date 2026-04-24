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

import config.CurrencyFormatter
import models.returns.ReturnsUserAnswers
import pages.returns.EnterDutyAmountPage
import play.api.i18n.Messages
import utils.ReturnsDateUtils

case class ConfirmationViewModel(email: String, date: String, totalDue: String, currentMonth: String)

object ConfirmationViewModel extends CurrencyFormatter {

  def apply(ua: ReturnsUserAnswers, email: String)(implicit messages: Messages): ConfirmationViewModel =
    confirmationViewModel(email, ua)

  private def confirmationViewModel(email: String, ua: ReturnsUserAnswers)(implicit messages: Messages) = {
    val monthMessage = ReturnsDateUtils.getCurrentMonthMessage(ReturnsDateUtils.month)

    val amountInMl = ua.get(EnterDutyAmountPage) match {
      case Some(value) => value
      case None => 0
    }
    new ConfirmationViewModel(email, makeDateString(monthMessage), totalDue(amountInMl), monthMessage)
  }

  private def makeDateString(monthMessage: String)(implicit messages: Messages) = {
    s"${ReturnsDateUtils.getCurrentDay} $monthMessage ${ReturnsDateUtils.getYear}"
  }

  private def totalDue(valueInMl: Int) =
    currencyFormat(calculateDuty(valueInMl))

}
