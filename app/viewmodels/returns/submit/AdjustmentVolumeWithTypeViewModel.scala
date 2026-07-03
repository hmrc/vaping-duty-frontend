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
import models.obligations.ObligationsResponse
import play.api.i18n.Messages
import utils.ReturnsDateUtils

case class AdjustmentVolumeWithTypeViewModel(periodDisplay: String)

object AdjustmentVolumeWithTypeViewModel {

  def apply(obligations: ObligationsResponse, adjustmentPeriodKey: PeriodKey)(implicit messages: Messages): AdjustmentVolumeWithTypeViewModel = {
    val periodDisplay = obligations.obligation
      .map(_.obligationDetails)
      .find(_.periodKey == adjustmentPeriodKey.toString)
      .map { obligation =>
        val monthKey = ReturnsDateUtils.getMonthMessageKey(obligation.iCFromDate.getMonthValue)
        s"${messages(monthKey)} ${obligation.iCFromDate.getYear}"
      }
      .getOrElse(adjustmentPeriodKey.toString)

    AdjustmentVolumeWithTypeViewModel(periodDisplay)
  }
}
