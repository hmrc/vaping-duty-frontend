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

import models.obligations.ObligationItem
import models.returns.AdjustmentsEligibility
import play.api.i18n.Messages
import utils.ReturnsDateUtils.*

import java.time.{LocalDate, Month}

case class BeforeYouStartViewModel(
  returnPeriod: String,
  dueDate: String,
  monthLength: Int,
  year: Int,
  adjustmentsEligibility: AdjustmentsEligibility
)

object BeforeYouStartViewModel {

  val month: Month = LocalDate.now().getMonth

  def apply(obligations: Seq[ObligationItem])(implicit messages: Messages): BeforeYouStartViewModel = {
    val adjustmentsEligibility = AdjustmentsEligibility.fromObligations(obligations)
    beforeYouStartViewModel(adjustmentsEligibility)
  }

  private def beforeYouStartViewModel(adjustmentsEligibility: AdjustmentsEligibility)(implicit messages: Messages) =
    new BeforeYouStartViewModel(
      getReturnPeriod(month),
      getDueDate(month),
      getMonthLength(month),
      getYear,
      adjustmentsEligibility
    )
  
}