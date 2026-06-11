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
import models.returns.{AdjustmentsEligibility, ReturnsUserAnswers}
import play.api.i18n.Messages
import utils.ReturnsDateUtils.*

case class TaskListPageViewModel(
                                  sections: Seq[TaskListSection],
                                  returnPeriod: String,
                                  year: String,
                                  dueYear: String,
                                  dueDate: String
                                )

object TaskListPageViewModel {

  def apply(userAnswers: ReturnsUserAnswers, obligations: Seq[ObligationItem], periodKey: PeriodKey)(implicit messages: Messages): TaskListPageViewModel = {
    
    val adjustmentsEligibility = AdjustmentsEligibility.fromObligations(obligations)

    val currentObligation = obligations
      .find(obligation => obligation.obligationDetails.periodKey == periodKey.toString)
      // scalafix:off DisableSyntax.throw
      .getOrElse(throw new IllegalStateException(s"No obligation found for period key: ${periodKey.toString}."))

    val monthOfObligation = currentObligation.obligationDetails.iCFromDate.getMonth
    val monthDue          = currentObligation.obligationDetails.iCDueDate.getMonth
    val yearOfObligation  = currentObligation.obligationDetails.iCFromDate.getYear.toString
    val yearDue           = currentObligation.obligationDetails.iCDueDate.getYear.toString

    TaskListPageViewModel(
      sections     = TaskList.sections(userAnswers, adjustmentsEligibility),
      returnPeriod = getReturnMonth(monthOfObligation),
      year         = yearOfObligation,
      dueYear      = yearDue,
      dueDate      = getDueDate(monthDue)
    )
  }
}
