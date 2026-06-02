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
import play.api.i18n.Messages

import java.time.format.TextStyle
import java.util.Locale

case class SpoiltVolumeByPeriodViewModel(
  monthName: String,
  year: String,
  periodKey: PeriodKey,
  currentReturnPeriod: PeriodKey
)

object SpoiltVolumeByPeriodViewModel {
  def apply(obligation: ObligationDetails, spoiltPeriodKey: PeriodKey, currentReturnPeriod: PeriodKey)(implicit messages: Messages): SpoiltVolumeByPeriodViewModel = {
    val month = obligation.iCFromDate.getMonth
    val monthName = month.getDisplayName(TextStyle.FULL, Locale.UK)
    val year = obligation.iCFromDate.getYear.toString

    SpoiltVolumeByPeriodViewModel(
      monthName = monthName,
      year = year,
      periodKey = spoiltPeriodKey,
      currentReturnPeriod = currentReturnPeriod
    )
  }
}