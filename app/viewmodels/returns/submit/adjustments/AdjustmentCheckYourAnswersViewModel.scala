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

import models.NormalMode
import models.identifiers.PeriodKey
import models.obligations.ObligationsResponse
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.{CurrencyFormatter, ReturnsDateUtils}

case class AdjustmentCheckYourAnswersViewModel(
                                                summaryCards: Seq[AdjustmentSummaryCard],
                                                hasAdjustments: Boolean,
                                                totalAdjustment: BigDecimal
                                              )

object AdjustmentCheckYourAnswersViewModel {

  def apply(
             adjustmentList: Option[AdjustmentList],
             obligations: ObligationsResponse,
             periodKey: PeriodKey,
             dutyRates: Map[String, BigDecimal]
           )(implicit messages: Messages): AdjustmentCheckYourAnswersViewModel = {

    val adjustments = adjustmentList.map(_.adjustments).getOrElse(Seq.empty)

    val summaryCards = adjustments.map { adjustment =>
      buildSummaryCard(adjustment, obligations, periodKey, dutyRates)
    }

    val totalAdjustment = adjustments.map { adjustment =>
      val dutyAmount = calculateDuty(adjustment.volumeInMl, dutyRates.getOrElse(adjustment.period.toString, BigDecimal(0)))
      adjustment.adjustmentType match {
        case AdjustmentType.OverDeclared => -dutyAmount
        case AdjustmentType.UnderDeclared => dutyAmount
      }
    }.sum

    AdjustmentCheckYourAnswersViewModel(
      summaryCards = summaryCards,
      hasAdjustments = adjustments.nonEmpty,
      totalAdjustment = totalAdjustment
    )
  }

  private def buildSummaryCard(
                                adjustment: AdjustmentEntry,
                                obligations: ObligationsResponse,
                                currentPeriodKey: PeriodKey,
                                dutyRates: Map[String, BigDecimal]
                              )(implicit messages: Messages): AdjustmentSummaryCard = {

    val periodDisplay = formatPeriod(adjustment.period, obligations)
    val dutyAmount = calculateDuty(adjustment.volumeInMl, dutyRates.getOrElse(adjustment.period.toString, BigDecimal(0)))

    val rows = Seq(
      buildTypeRow(adjustment.adjustmentType, adjustment.period, currentPeriodKey),
      buildVolumeRow(adjustment.volumeInMl, adjustment.period, currentPeriodKey),
      buildDutyRow(dutyAmount, adjustment.adjustmentType)
    )

    val cardActions = Seq(
      ActionItem(
        href = s"${controllers.returns.submit.adjustments.routes.SelectAdjustmentPeriodController.onPageLoad(None).url}?period=${currentPeriodKey.value}",
        content = Text(messages("site.change")),
        visuallyHiddenText = Some(messages("returns.adjustmentCheckYourAnswers.card.change.hidden", periodDisplay))
      ),
      ActionItem(
        href = "#",
        content = Text(messages("site.remove")),
        visuallyHiddenText = Some(messages("returns.adjustmentCheckYourAnswers.card.remove.hidden", periodDisplay))
      )
    )

    AdjustmentSummaryCard(
      rows = rows,
      card = Card(
        title = Some(CardTitle(content = Text(periodDisplay))),
        actions = Some(Actions(items = cardActions))
      )
    )
  }

  private def buildTypeRow(
                            adjustmentType: AdjustmentType,
                            adjustmentPeriod: PeriodKey,
                            currentPeriodKey: PeriodKey
                          )(implicit messages: Messages): SummaryListRow = {
    val typeText = adjustmentType match {
      case AdjustmentType.UnderDeclared => messages("returns.adjustmentCheckYourAnswers.type.underDeclared")
      case AdjustmentType.OverDeclared => messages("returns.adjustmentCheckYourAnswers.type.overDeclared")
    }

    SummaryListRow(
      key = Key(content = Text(messages("returns.adjustmentCheckYourAnswers.type"))),
      value = Value(content = Text(typeText)),
      actions = Some(Actions(items = Seq(
        ActionItem(
          href = s"${controllers.returns.submit.adjustments.routes.AdjustmentVolumeWithTypeController.onPageLoad(NormalMode).url}?period=${currentPeriodKey.value}&adjustmentPeriod=${adjustmentPeriod.value}",
          content = Text(messages("site.change")),
          visuallyHiddenText = Some(messages("returns.adjustmentCheckYourAnswers.type.change.hidden"))
        )
      )))
    )
  }

  private def buildVolumeRow(
                              volume: BigDecimal,
                              adjustmentPeriod: PeriodKey,
                              currentPeriodKey: PeriodKey
                            )(implicit messages: Messages): SummaryListRow = {
    SummaryListRow(
      key = Key(content = Text(messages("returns.adjustmentCheckYourAnswers.volume"))),
      value = Value(content = HtmlContent(s"${volume.toString} ml")),
      actions = Some(Actions(items = Seq(
        ActionItem(
          href = s"${controllers.returns.submit.adjustments.routes.AdjustmentVolumeWithTypeController.onPageLoad(NormalMode).url}?period=${currentPeriodKey.value}&adjustmentPeriod=${adjustmentPeriod.value}",
          content = Text(messages("site.change")),
          visuallyHiddenText = Some(messages("returns.adjustmentCheckYourAnswers.volume.change.hidden"))
        )
      )))
    )
  }

  private def buildDutyRow(dutyAmount: BigDecimal, adjustmentType: AdjustmentType)(implicit messages: Messages): SummaryListRow = {
    val signedAmount = adjustmentType match {
      case AdjustmentType.OverDeclared => -dutyAmount
      case AdjustmentType.UnderDeclared => dutyAmount
    }

    SummaryListRow(
      key = Key(content = Text(messages("returns.adjustmentCheckYourAnswers.duty"))),
      value = Value(content = Text(CurrencyFormatter.currencyFormat(signedAmount))),
      actions = None
    )
  }

  private def formatPeriod(periodKey: PeriodKey, obligations: ObligationsResponse)(implicit messages: Messages): String = {
    obligations.obligation
      .map(_.obligationDetails)
      .find(_.periodKey == periodKey.toString)
      .map { obligation =>
        val month = obligation.iCFromDate.getMonthValue
        val year = obligation.iCFromDate.getYear
        val monthKey = ReturnsDateUtils.getMonthMessageKey(month)
        s"${messages(monthKey)} $year"
      }
      .getOrElse(
        // scalafix:off DisableSyntax.throw
        throw new IllegalStateException(
          s"Period key '${periodKey.value}' not found in obligations. " +
          s"Available period keys: ${obligations.obligation.map(_.obligationDetails.periodKey).mkString(", ")}"
        )
      )
  }

  private def calculateDuty(volumeInMl: BigDecimal, dutyRate: BigDecimal): BigDecimal = {
    val volumeInLitres = volumeInMl / 1000
    val volumeIn10ml = volumeInLitres * 100
    volumeIn10ml * dutyRate
  }
}
