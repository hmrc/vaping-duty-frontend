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

package viewmodels.returns.submit.spoilt

import models.obligations.ObligationDetails
import models.returns.{DutyRate, SpoiltVolumeByPeriod}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.{CurrencyFormatter, ReturnsDateUtils}

case class RemoveSpoiltAdjustmentViewModel(rows: Seq[SummaryListRow])

object RemoveSpoiltAdjustmentViewModel {

  def apply(
             entry: SpoiltVolumeByPeriod,
             obligationDetails: Seq[ObligationDetails],
             dutyRate: DutyRate,
             returnsDateUtils: ReturnsDateUtils
           )(implicit messages: Messages): RemoveSpoiltAdjustmentViewModel = {

    val periodDisplay = returnsDateUtils.formatPeriodDisplay(entry.periodKey, obligationDetails)
    val formattedDuty = CurrencyFormatter.currencyFormatWithLeadingSign(-dutyRate.calculateDuty(entry.volume))

    val rows = Seq(
      row("returns.removeSpoiltAdjustment.month", periodDisplay),
      row("returns.spoiltVolumeByPeriod.hint", entry.volume.toString),
      row("returns.spoiltCheckYourAnswers.duty", formattedDuty)
    )

    RemoveSpoiltAdjustmentViewModel(rows)
  }

  private def row(keyMessage: String, value: String)(implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      key = Key(content = Text(messages(keyMessage))),
      value = Value(content = Text(value))
    )
}
