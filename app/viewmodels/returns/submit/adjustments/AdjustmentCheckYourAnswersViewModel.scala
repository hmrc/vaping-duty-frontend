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
import models.obligations.ObligationDetails
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.{CurrencyFormatter, ReturnsDateUtils}

case class AdjustmentCheckYourAnswersViewModel(
                                                summaryCards: Seq[AdjustmentSummaryCard],
                                                hasAdjustments: Boolean,
                                                totalAdjustment: BigDecimal,
                                                formattedTotalAdjustment: String
                                              )

object AdjustmentCheckYourAnswersViewModel {

  def apply(
             declareAdjustment: Option[Boolean],
             adjustmentList: Option[AdjustmentList],
             obligationDetails: Seq[ObligationDetails],
             periodKey: PeriodKey,
             dutyRates: Map[String, BigDecimal],
             returnsDateUtils: ReturnsDateUtils
           )(implicit messages: Messages): AdjustmentCheckYourAnswersViewModel = {

    val adjustments = adjustmentList.map(_.adjustments).getOrElse(Seq.empty)

    val summaryCards = declareAdjustment match {
      case Some(false) =>
        Seq(buildNoAdjustmentCard(periodKey))
      case _ =>
        adjustments.map { adjustment =>
          buildSummaryCard(adjustment, obligationDetails, periodKey, dutyRates, returnsDateUtils)
        }
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
      totalAdjustment = totalAdjustment,
      formattedTotalAdjustment = CurrencyFormatter.currencyFormatWithLeadingSign(totalAdjustment)
    )
  }

  private def buildAdjustmentUrl(baseUrl: String, currentPeriod: PeriodKey, adjustmentPeriod: Option[PeriodKey] = None): String = {
    adjustmentPeriod match {
      case Some(adjPeriod) => s"$baseUrl?period=${currentPeriod.value}&adjustmentPeriod=${adjPeriod.value}"
      case None => s"$baseUrl?period=${currentPeriod.value}"
    }
  }

  private def buildSummaryCard(
                                adjustment: AdjustmentEntry,
                                obligationDetails: Seq[ObligationDetails],
                                currentPeriodKey: PeriodKey,
                                dutyRates: Map[String, BigDecimal],
                                returnsDateUtils: ReturnsDateUtils
                              )(implicit messages: Messages): AdjustmentSummaryCard = {

    val periodDisplay = formatPeriod(adjustment.period, obligationDetails, returnsDateUtils)
    val dutyAmount = calculateDuty(adjustment.volumeInMl, dutyRates.getOrElse(adjustment.period.toString, BigDecimal(0)))

    val rows = Seq(
      buildDeclareAdjustmentRow(currentPeriodKey, declaredAdjustment = true),
      buildTypeRow(adjustment.adjustmentType, adjustment.period, currentPeriodKey),
      buildVolumeRow(adjustment.volumeInMl, adjustment.period, currentPeriodKey),
      buildDutyRow(dutyAmount, adjustment.adjustmentType)
    )

    val cardActions = Seq(
      ActionItem(
        href = buildAdjustmentUrl(
          controllers.returns.submit.adjustments.routes.SelectAdjustmentPeriodController.onPageLoad(None).url,
          currentPeriodKey
        ),
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

  private def buildNoAdjustmentCard(currentPeriodKey: PeriodKey)(implicit messages: Messages): AdjustmentSummaryCard = {
    val row = buildDeclareAdjustmentRow(currentPeriodKey, declaredAdjustment = false)

    AdjustmentSummaryCard(
      rows = Seq(row),
      card = Card(
        title = Some(CardTitle(content = Text("Summary")))
      )
    )
  }

  private def buildDeclareAdjustmentRow(currentPeriodKey: PeriodKey, declaredAdjustment: Boolean)(implicit messages: Messages): SummaryListRow = {
    SummaryListRow(
      key = Key(content = Text(messages("returns.declareAdjustmentQuestion.checkYourAnswersLabel"))),
      value = Value(content = Text(messages(if (declaredAdjustment) "site.yes" else "site.no"))),
      actions = Some(Actions(items = Seq(
        ActionItem(
          href = buildAdjustmentUrl(
            controllers.returns.submit.adjustments.routes.DeclareAdjustmentQuestionController.onPageLoad(NormalMode).url,
            currentPeriodKey
          ),
          content = Text(messages("site.change")),
          visuallyHiddenText = Some(messages("returns.declareAdjustmentQuestion.change.hidden"))
        )
      )))
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
          href = buildAdjustmentUrl(
            controllers.returns.submit.adjustments.routes.AdjustmentVolumeWithTypeController.onPageLoad(NormalMode).url,
            currentPeriodKey,
            Some(adjustmentPeriod)
          ),
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
          href = buildAdjustmentUrl(
            controllers.returns.submit.adjustments.routes.AdjustmentVolumeWithTypeController.onPageLoad(NormalMode).url,
            currentPeriodKey,
            Some(adjustmentPeriod)
          ),
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
      value = Value(content = Text(CurrencyFormatter.currencyFormatWithLeadingSign(signedAmount))),
      actions = None
    )
  }

  private def formatPeriod(periodKey: PeriodKey, obligationDetails: Seq[ObligationDetails], returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): String = {
    returnsDateUtils.formatPeriodDisplay(periodKey, obligationDetails)
  }

  private def calculateDuty(volumeInMl: BigDecimal, dutyRate: BigDecimal): BigDecimal = {
    (volumeInMl * dutyRate).setScale(2, BigDecimal.RoundingMode.DOWN)
  }
}