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

import models.NormalMode
import models.identifiers.PeriodKey
import models.obligations.ObligationDetails
import models.returns.SpoiltVolumeByPeriod
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.{CurrencyFormatter, ReturnsDateUtils}
import viewmodels.returns.submit.PeriodSelectionHelper

case class SpoiltCheckYourAnswersViewModel(
                                            summaryCards: Seq[SpoiltSummaryCard],
                                            hasSpoiltProducts: Boolean,
                                            totalSpoiltDuty: BigDecimal,
                                            formattedTotalSpoiltDuty: String,
                                            hasAvailablePeriodsToAdd: Boolean
                                          )

object SpoiltCheckYourAnswersViewModel {

  def apply(
             declareSpoiltProducts: Option[Boolean],
             spoiltList: Option[List[SpoiltVolumeByPeriod]],
             obligationDetails: Seq[ObligationDetails],
             periodKey: PeriodKey,
             dutyRates: Map[String, BigDecimal],
             returnsDateUtils: ReturnsDateUtils
           )(implicit messages: Messages): SpoiltCheckYourAnswersViewModel = {

    val spoiltEntries = spoiltList.getOrElse(List.empty)

    val summaryCards = declareSpoiltProducts match {
      case Some(false) =>
        Seq(buildNoSpoiltProductsCard(periodKey))
      case _ =>
        spoiltEntries.reverse.map { entry =>
          buildSummaryCard(entry, obligationDetails, periodKey, dutyRates, returnsDateUtils)
        }
    }

    val totalSpoiltDuty = spoiltEntries.map { entry =>
      calculateDuty(entry.volume, dutyRates.getOrElse(entry.periodKey.toString, BigDecimal(0)))
    }.sum

    SpoiltCheckYourAnswersViewModel(
      summaryCards = summaryCards,
      hasSpoiltProducts = spoiltEntries.nonEmpty,
      totalSpoiltDuty = totalSpoiltDuty,
      formattedTotalSpoiltDuty = CurrencyFormatter.currencyFormat(totalSpoiltDuty),
      hasAvailablePeriodsToAdd = hasAvailablePeriodsToAdd(obligationDetails, periodKey, spoiltList)
    )
  }

  def hasAvailablePeriodsToAdd(
                                obligationDetails: Seq[ObligationDetails],
                                periodKey: PeriodKey,
                                spoiltList: Option[List[SpoiltVolumeByPeriod]]
                              ): Boolean =
    calculateAvailablePeriods(obligationDetails, periodKey, spoiltList.getOrElse(List.empty)).nonEmpty

  private def calculateAvailablePeriods(
                                         obligationDetails: Seq[ObligationDetails],
                                         currentReturnPeriod: PeriodKey,
                                         spoiltEntries: List[SpoiltVolumeByPeriod]
                                       ): Seq[ObligationDetails] = {
    val fulfilledObligations = PeriodSelectionHelper.filterFulfilledWithinThreeYears(obligationDetails)
      .filter(_.periodKey != currentReturnPeriod.toString)

    val existingSpoiltPeriods = spoiltEntries.map(_.periodKey.toString).toSet

    fulfilledObligations
      .filterNot(ob => existingSpoiltPeriods.contains(ob.periodKey))
  }

  private def buildSpoiltUrl(baseUrl: String, currentPeriod: PeriodKey, spoiltPeriod: Option[PeriodKey] = None): String = {
    spoiltPeriod match {
      case Some(period) => s"$baseUrl?period=${currentPeriod.value}&spoiltPeriod=${period.value}"
      case None => s"$baseUrl?period=${currentPeriod.value}"
    }
  }

  private def buildSummaryCard(
                                entry: SpoiltVolumeByPeriod,
                                obligationDetails: Seq[ObligationDetails],
                                currentPeriodKey: PeriodKey,
                                dutyRates: Map[String, BigDecimal],
                                returnsDateUtils: ReturnsDateUtils
                              )(implicit messages: Messages): SpoiltSummaryCard = {

    val periodDisplay = returnsDateUtils.formatPeriodDisplay(entry.periodKey, obligationDetails)
    val dutyAmount = calculateDuty(entry.volume, dutyRates.getOrElse(entry.periodKey.toString, BigDecimal(0)))

    val rows = Seq(
      buildDeclareSpoiltProductsRow(currentPeriodKey, declared = true),
      buildVolumeRow(entry.volume, entry.periodKey, currentPeriodKey),
      buildDutyRow(dutyAmount)
    )

    val cardActions = Seq(
      ActionItem(
        href = buildSpoiltUrl(
          controllers.returns.submit.spoilt.routes.RemoveSpoiltAdjustmentController.onPageLoad().url,
          currentPeriodKey,
          Some(entry.periodKey)
        ),
        content = Text(messages("site.remove")),
        visuallyHiddenText = Some(messages("returns.spoiltCheckYourAnswers.card.remove.hidden", periodDisplay))
      )
    )

    SpoiltSummaryCard(
      rows = rows,
      card = Card(
        title = Some(CardTitle(content = Text(periodDisplay))),
        actions = Some(Actions(items = cardActions))
      )
    )
  }

  private def buildNoSpoiltProductsCard(currentPeriodKey: PeriodKey)(implicit messages: Messages): SpoiltSummaryCard = {
    val row = buildDeclareSpoiltProductsRow(currentPeriodKey, declared = false)

    SpoiltSummaryCard(
      rows = Seq(row),
      card = Card(
        title = Some(CardTitle(content = Text("Summary")))
      )
    )
  }

  private def buildDeclareSpoiltProductsRow(currentPeriodKey: PeriodKey, declared: Boolean)(implicit messages: Messages): SummaryListRow = {
    SummaryListRow(
      key = Key(content = Text(messages("returns.declareSpoiltProducts.question"))),
      value = Value(content = Text(messages(if (declared) "site.yes" else "site.no"))),
      actions = Some(Actions(items = Seq(
        ActionItem(
          href = buildSpoiltUrl(
            controllers.returns.submit.spoilt.routes.DeclareSpoiltProductsController.onPageLoad(NormalMode).url,
            currentPeriodKey
          ),
          content = Text(messages("site.change")),
          visuallyHiddenText = Some(messages("returns.declareSpoiltProducts.change.hidden"))
        )
      )))
    )
  }

  private def buildVolumeRow(
                              volume: BigDecimal,
                              spoiltPeriod: PeriodKey,
                              currentPeriodKey: PeriodKey
                            )(implicit messages: Messages): SummaryListRow = {
    SummaryListRow(
      key = Key(content = Text(messages("returns.spoiltCheckYourAnswers.volume"))),
      value = Value(content = HtmlContent(s"${volume.toString} ml")),
      actions = Some(Actions(items = Seq(
        ActionItem(
          href = buildSpoiltUrl(
            controllers.returns.submit.spoilt.routes.SpoiltVolumeByPeriodController.onPageLoad().url,
            currentPeriodKey,
            Some(spoiltPeriod)
          ),
          content = Text(messages("site.change")),
          visuallyHiddenText = Some(messages("returns.spoiltCheckYourAnswers.volume.change.hidden"))
        )
      )))
    )
  }

  private def buildDutyRow(dutyAmount: BigDecimal)(implicit messages: Messages): SummaryListRow = {
    SummaryListRow(
      key = Key(content = Text(messages("returns.spoiltCheckYourAnswers.duty"))),
      value = Value(content = Text(CurrencyFormatter.currencyFormat(dutyAmount))),
      actions = None
    )
  }

  private def calculateDuty(volumeInMl: BigDecimal, dutyRate: BigDecimal): BigDecimal = {
    (volumeInMl * dutyRate).setScale(2, BigDecimal.RoundingMode.DOWN)
  }
}
