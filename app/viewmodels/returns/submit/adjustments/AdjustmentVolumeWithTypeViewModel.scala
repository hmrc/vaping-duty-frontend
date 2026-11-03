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

package viewmodels.returns.submit.adjustments

import models.Mode
import models.identifiers.PeriodKey
import models.obligations.ObligationDetails
import play.api.i18n.Messages
import play.api.mvc.Call
import utils.ReturnsDateUtils

case class AdjustmentVolumeWithTypeViewModel(periodDisplay: String, submitAction: Call)

object AdjustmentVolumeWithTypeViewModel {

  def apply(
    obligationDetails: Seq[ObligationDetails],
    periodKey: PeriodKey,
    adjustmentPeriodKey: PeriodKey,
    mode: Mode,
    returnsDateUtils: ReturnsDateUtils
  )(implicit messages: Messages): AdjustmentVolumeWithTypeViewModel = {
    val periodDisplay = returnsDateUtils.formatPeriodDisplay(adjustmentPeriodKey, obligationDetails)
    val submitAction = Call(
      "POST",
      s"${controllers.returns.submit.adjustments.routes.AdjustmentVolumeWithTypeController.onSubmit(mode).url}?period=${periodKey.value}&adjustmentPeriod=${adjustmentPeriodKey.value}"
    )
    AdjustmentVolumeWithTypeViewModel(periodDisplay, submitAction)
  }
}
