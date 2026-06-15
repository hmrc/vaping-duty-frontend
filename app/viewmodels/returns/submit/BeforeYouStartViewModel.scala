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

package viewmodels.returns.submit

import models.identifiers.PeriodKey
import models.obligations.ObligationItem
import models.returns.AdjustmentsEligibility
import play.api.i18n.Messages
import utils.ReturnsDateUtils.*

import java.time.Month

case class BeforeYouStartViewModel(
                                    returnPeriod: String,
                                    yearOfReturn: Int,
                                    dueDate: String,
                                    dueYear: Int,
                                    adjustmentsEligibility: AdjustmentsEligibility
                                  )

object BeforeYouStartViewModel {

  def apply(obligations: Seq[ObligationItem], periodKey: PeriodKey)(implicit messages: Messages): Option[BeforeYouStartViewModel] = {
    val adjustmentsEligibility = AdjustmentsEligibility.fromObligations(obligations)

    obligations
      .map(_.obligationDetails)
      .find(_.periodKey == periodKey.toString)
      .map { details =>
        val dayDue: Int = details.iCDueDate.getDayOfMonth
        val monthDue: Month = details.iCDueDate.getMonth
        val yearDue: Int = details.iCDueDate.getYear

        val returnMonth: Month = details.iCFromDate.getMonth
        val returnYear: Int = details.iCFromDate.getYear

        beforeYouStartViewModel(adjustmentsEligibility, dayDue, monthDue, returnMonth, returnYear, yearDue)
      }
  }

  private def beforeYouStartViewModel(
                                       adjustmentsEligibility: AdjustmentsEligibility,
                                       dayDue: Int,
                                       monthDue: Month,
                                       returnMonth: Month,
                                       returnYear: Int,
                                       dueYear: Int)
                                     (implicit messages: Messages) = {

    val dueMonthWithDay = s"${dayDue.toString} ${getDueDate(monthDue)}"

    new BeforeYouStartViewModel(
      getMonthMessage(returnMonth),
      returnYear,
      dueMonthWithDay,
      dueYear,
      adjustmentsEligibility
    )
  }
}