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

import models.obligations.ObligationDetails
import models.returns.DutyRate
import models.returns.adjustments.{AdjustmentEntry, AdjustmentType}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.{CurrencyFormatter, ReturnsDateUtils}

case class RemoveAdjustmentViewModel(rows: Seq[SummaryListRow])

object RemoveAdjustmentViewModel {

  def apply(
             entry: AdjustmentEntry,
             obligationDetails: Seq[ObligationDetails],
             dutyRate: DutyRate,
             returnsDateUtils: ReturnsDateUtils
           )(implicit messages: Messages): RemoveAdjustmentViewModel = {

    val periodDisplay = returnsDateUtils.formatPeriodDisplay(entry.period, obligationDetails)
    val dutyAmount = dutyRate.calculateDuty(entry.volumeInMl)

    val (typeMessageKey, volumeLabelKey, signedDuty) = entry.adjustmentType match {
      case AdjustmentType.UnderDeclared =>
        ("returns.adjustmentCheckYourAnswers.type.underDeclared", "returns.adjustmentVolumeWithType.underDeclared.label", dutyAmount)
      case AdjustmentType.OverDeclared =>
        ("returns.adjustmentCheckYourAnswers.type.overDeclared", "returns.adjustmentVolumeWithType.overDeclared.label", -dutyAmount)
    }

    val rows = Seq(
      row("returns.removeAdjustment.month", periodDisplay),
      row("returns.adjustmentCheckYourAnswers.type", messages(typeMessageKey)),
      row(volumeLabelKey, entry.volumeInMl.toString),
      row("returns.adjustmentCheckYourAnswers.duty", CurrencyFormatter.currencyFormatWithLeadingSign(signedDuty))
    )

    RemoveAdjustmentViewModel(rows)
  }

  private def row(keyMessage: String, value: String)(implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      key = Key(content = Text(messages(keyMessage))),
      value = Value(content = Text(value))
    )
}
