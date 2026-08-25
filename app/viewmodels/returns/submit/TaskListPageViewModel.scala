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
import models.obligations.ObligationDetails
import models.returns.{AdjustmentsEligibility, ReturnsUserAnswers}
import play.api.i18n.Messages
import utils.ReturnsDateUtils

case class TaskListPageViewModel(
                                  sections: Seq[TaskListSection],
                                  returnPeriod: String,
                                  year: String,
                                  dueYear: String,
                                  dueDate: String,
                                  dayDue: String
                                )

object TaskListPageViewModel {

  def apply(userAnswers: ReturnsUserAnswers, obligations: Seq[ObligationDetails], periodKey: PeriodKey, returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): TaskListPageViewModel = {
    
    val adjustmentsEligibility = AdjustmentsEligibility.fromObligations(obligations)

    val currentObligation = obligations
      .find(obligation => obligation.periodKey == periodKey.toString)
      // scalafix:off DisableSyntax.throw
      .getOrElse(throw new IllegalStateException(s"No obligation found for period key: ${periodKey.toString}."))

    val monthOfObligation = currentObligation.iCFromDate.getMonth
    val dayDue            = currentObligation.iCDueDate.getDayOfMonth.toString
    val monthDue          = currentObligation.iCDueDate.getMonth
    val yearOfObligation  = currentObligation.iCFromDate.getYear.toString
    val yearDue           = currentObligation.iCDueDate.getYear.toString

    TaskListPageViewModel(
      sections     = TaskList.sections(userAnswers, adjustmentsEligibility),
      returnPeriod = returnsDateUtils.getReturnMonth(monthOfObligation),
      year         = yearOfObligation,
      dueYear      = yearDue,
      dueDate      = returnsDateUtils.getDueDate(monthDue),
      dayDue       = dayDue
    )
  }
}
