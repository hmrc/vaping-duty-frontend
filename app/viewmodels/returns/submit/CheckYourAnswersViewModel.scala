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

import models.CheckMode
import models.identifiers.PeriodKey
import models.returns.adjustments.AdjustmentType
import models.returns.{DutyRate, ReturnsUserAnswers}
import pages.returns.adjustments.{AdjustmentListPage, AdjustmentReasonPage, DeclareAdjustmentPage}
import pages.returns.*
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.{CurrencyFormatter, ReturnsDateUtils}
import views.html.components.Paragraph

case class CheckYourAnswersViewModel(
                                      declareDutyCard: (String, SummaryList, Option[Seq[ActionItem]]),
                                      spoiltProductsCard: (String, SummaryList, Option[Seq[ActionItem]]),
                                      adjustmentsCard: (String, SummaryList, Option[Seq[ActionItem]]),
                                      dutySuspendedCard: (String, SummaryList, Option[Seq[ActionItem]]),
                                      totalDuty: BigDecimal,
                                      formattedTotalDuty: String,
                                      hasDutySuspended: Boolean,
                                      dutyCalculationParagraph: HtmlFormat.Appendable,
                                      nilReturn: Boolean,
                                      returnPeriod: String,
                                      year: String
                                    )

object CheckYourAnswersViewModel extends CurrencyFormatter {

  private val ZERO = "0"
  private val PLACEHOLDER_URL = "#"

  def apply(userAnswers: ReturnsUserAnswers, dutyRates: Map[PeriodKey, DutyRate], periodKey: PeriodKey, returnsDateUtils: ReturnsDateUtils)(implicit messages: Messages): CheckYourAnswersViewModel = {
    // scalafix:off DisableSyntax.throw
    val returnPeriod = userAnswers.returnPeriod
      .map(month => returnsDateUtils.getReturnMonth(month))
      .getOrElse(throw new IllegalStateException("Return period not found in user answers"))
    
    val year = userAnswers.year
      .getOrElse(throw new IllegalStateException("Return year not found in user answers"))

    val hasDutySuspended = userAnswers.get(DeclareDutySuspensePage).getOrElse(false)

    val declareDutyAmount = calculateDeclareDutyAmount(userAnswers, dutyRate)
    val spoiltAmount = calculateSpoiltAmount(userAnswers, dutyRate)
    val adjustmentAmount = calculateAdjustmentAmount(userAnswers, dutyRate)
    val totalDuty = declareDutyAmount + spoiltAmount + adjustmentAmount
    val nilReturn = totalDuty == BigDecimal(ZERO)

    CheckYourAnswersViewModel(
      declareDutyCard = buildDeclareDutyCard(userAnswers, declareDutyAmount, periodKey),
      spoiltProductsCard = buildSpoiltProductsCard(userAnswers, spoiltAmount, periodKey),
      adjustmentsCard = buildAdjustmentsCard(userAnswers, adjustmentAmount, periodKey),
      dutySuspendedCard = buildDutySuspendedCard(userAnswers, periodKey),
      totalDuty = totalDuty,
      formattedTotalDuty = currencyFormat(totalDuty),
      hasDutySuspended = hasDutySuspended,
      dutyCalculationParagraph = dutyCalculationParagraph(dutyRate),
      nilReturn = nilReturn,
      returnPeriod = returnPeriod,
      year = year
    )
  }

  private def calculateDeclareDutyAmount(userAnswers: ReturnsUserAnswers, dutyRate: DutyRate): BigDecimal = {
    userAnswers.get(DeclareDutyPage) match {
      case Some(true) =>
        userAnswers.get(EnterDutyAmountPage) match {
          case Some(volumeInMl) => dutyRate.calculateDuty(volumeInMl)
          case None => BigDecimal(ZERO)
        }
      case _ => BigDecimal(ZERO)
    }
  }

  private def calculateSpoiltAmount(userAnswers: ReturnsUserAnswers, dutyRate: DutyRate): BigDecimal = {
    userAnswers.get(DeclareSpoiltProductsPage) match {
      case Some(true) =>
        val spoiltList = userAnswers.get(SpoiltVolumeByPeriodPage).map(_.map(_.volume)).getOrElse(List.empty)
        -spoiltList.map(entry => dutyRate.calculateDuty(entry)).sum
      case _ => BigDecimal(ZERO)
    }
  }

  private def calculateAdjustmentAmount(userAnswers: ReturnsUserAnswers, dutyRate: DutyRate): BigDecimal = {
    userAnswers.get(DeclareAdjustmentPage) match {
      case Some(true) =>
        val adjustmentList = userAnswers.get(AdjustmentListPage).map(_.adjustments).getOrElse(Seq.empty)
        val underDeclared = adjustmentList
          .filter(_.adjustmentType == AdjustmentType.UnderDeclared)
          .map(adj => dutyRate.calculateDuty(adj.volumeInMl))
          .sum
        val overDeclared = adjustmentList
          .filter(_.adjustmentType == AdjustmentType.OverDeclared)
          .map(adj => dutyRate.calculateDuty(adj.volumeInMl))
          .sum
        underDeclared - overDeclared
      case _ => BigDecimal(ZERO)
    }
  }

  private def formatDutyAmount(amount: BigDecimal): String =
    if (amount < 0) {
      currencyFormat(amount.abs).replace("£", "-£")
    } else {
      currencyFormat(amount)
    }

  private def buildDeclareDutyCard(
                                     userAnswers: ReturnsUserAnswers,
                                     dutyAmount: BigDecimal,
                                     periodKey: PeriodKey
                                   )(implicit messages: Messages): (String, SummaryList, Option[Seq[ActionItem]]) = {
    val declareDuty = userAnswers.get(DeclareDutyPage).getOrElse(false)

    val rows = Seq(
      Some(SummaryListRow(
        key = Key(content = Text(messages("returns.CheckYourAnswers.card.declareDuty.question"))),
        value = Value(content = Text(messages(if (declareDuty) "site.yes" else "site.no"))),
        actions = None
      )),
      if (declareDuty) {
        Some(SummaryListRow(
          key = Key(content = Text(messages("returns.CheckYourAnswers.card.declareDuty.duty"))),
          value = Value(content = Text(currencyFormat(dutyAmount))),
          actions = None
        ))
      } else None
    ).flatten

    val cardActions = Some(Seq(
      ActionItem(
        href = s"${controllers.returns.submit.routes.DeclareDutyCheckAnswersController.onPageLoad(CheckMode).url}?period=${periodKey.value}",
        content = Text(messages("site.change")),
        visuallyHiddenText = Some(messages("returns.CheckYourAnswers.card.declareDuty.change.hidden"))
      )
    ))

    (messages("returns.CheckYourAnswers.card.declareDuty.title"), SummaryList(rows = rows), cardActions)
  }

  private def buildSpoiltProductsCard(
                                       userAnswers: ReturnsUserAnswers,
                                       spoiltAmount: BigDecimal,
                                       periodKey: PeriodKey
                                     )(implicit messages: Messages): (String, SummaryList, Option[Seq[ActionItem]]) = {
    val declareSpoilt = userAnswers.get(DeclareSpoiltProductsPage).getOrElse(false)

    val rows = Seq(
      Some(SummaryListRow(
        key = Key(content = Text(messages("returns.CheckYourAnswers.card.spoilt.question"))),
        value = Value(content = Text(messages(if (declareSpoilt) "site.yes" else "site.no"))),
        actions = None
      )),
      if (declareSpoilt) {
        Some(SummaryListRow(
          key = Key(content = Text(messages("returns.CheckYourAnswers.card.spoilt.total"))),
          value = Value(content = Text(currencyFormatWithLeadingSign(spoiltAmount))),
          actions = None
        ))
      } else None
    ).flatten

    val cardActions = Some(Seq(
      ActionItem(
        href = PLACEHOLDER_URL,
        content = Text(messages("site.change")),
        visuallyHiddenText = Some(messages("returns.CheckYourAnswers.card.spoilt.change.hidden"))
      )
    ))

    (messages("returns.CheckYourAnswers.card.spoilt.title"), SummaryList(rows = rows), cardActions)
  }

  private def buildAdjustmentsCard(
                                     userAnswers: ReturnsUserAnswers,
                                     adjustmentAmount: BigDecimal,
                                     periodKey: PeriodKey
                                   )(implicit messages: Messages): (String, SummaryList, Option[Seq[ActionItem]]) = {
    val declareAdjustment = userAnswers.get(DeclareAdjustmentPage).getOrElse(false)
    val adjustmentReason = userAnswers.get(AdjustmentReasonPage)

    val rows = Seq(
      Some(SummaryListRow(
        key = Key(content = Text(messages("returns.CheckYourAnswers.card.adjustments.question"))),
        value = Value(content = Text(messages(if (declareAdjustment) "site.yes" else "site.no"))),
        actions = None
      )),
      if (declareAdjustment) {
        Some(SummaryListRow(
          key = Key(content = Text(messages("returns.CheckYourAnswers.card.adjustments.total"))),
          value = Value(content = Text(currencyFormatWithLeadingSign(adjustmentAmount))),
          actions = None
        ))
      } else None,
      if (declareAdjustment && adjustmentReason.isDefined) {
        Some(SummaryListRow(
          key = Key(content = Text(messages("returns.CheckYourAnswers.card.adjustments.reason"))),
          value = Value(content = Text(adjustmentReason.getOrElse(""))),
          actions = Some(Actions(items = Seq(
            ActionItem(
              href = PLACEHOLDER_URL,
              content = Text(messages("site.change")),
              visuallyHiddenText = Some(messages("returns.CheckYourAnswers.card.adjustments.reason.change.hidden"))
            )
          )))
        ))
      } else None
    ).flatten

    val cardActions = Some(Seq(
      ActionItem(
        href = PLACEHOLDER_URL,
        content = Text(messages("site.change")),
        visuallyHiddenText = Some(messages("returns.CheckYourAnswers.card.adjustments.change.hidden"))
      )
    ))

    (messages("returns.CheckYourAnswers.card.adjustments.title"), SummaryList(rows = rows), cardActions)
  }

  private def buildDutySuspendedCard(
                                       userAnswers: ReturnsUserAnswers,
                                       periodKey: PeriodKey
                                     )(implicit messages: Messages): (String, SummaryList, Option[Seq[ActionItem]]) = {
    val declareDutySuspense = userAnswers.get(DeclareDutySuspensePage).getOrElse(false)

    val rows = Seq(
      Some(SummaryListRow(
        key = Key(content = Text(messages("returns.CheckYourAnswers.card.dutySuspended.question"))),
        value = Value(content = Text(messages(if (declareDutySuspense) "site.yes" else "site.no"))),
        actions = None
      )),
      if (declareDutySuspense) {
        Some(SummaryListRow(
          key = Key(content = Text(messages("returns.CheckYourAnswers.card.dutySuspended.declared"))),
          value = Value(content = Text(messages("returns.CheckYourAnswers.dutySuspended.declared.value"))),
          actions = None
        ))
      } else None
    ).flatten

    val cardActions = Some(Seq(
      ActionItem(
        href = s"${controllers.returns.submit.routes.DutySuspenseCheckAnswersController.onPageLoad(CheckMode).url}?period=${periodKey.value}",
        content = Text(messages("site.change")),
        visuallyHiddenText = Some(messages("returns.CheckYourAnswers.card.dutySuspended.change.hidden"))
      )
    ))

    (messages("returns.CheckYourAnswers.card.dutySuspended.title"), SummaryList(rows = rows), cardActions)
  }

  private def dutyCalculationParagraph(dutyRate: DutyRate)(implicit messages: Messages): HtmlFormat.Appendable = {
    val p = new Paragraph()
    p(Seq(Text(messages("returns.CheckYourAnswers.dutyCalculation", currencyFormat(dutyRate.dutyRateInPoundsPer10Ml)))))
  }
}
