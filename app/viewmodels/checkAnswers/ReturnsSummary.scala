/*
 * Copyright 2024 HM Revenue & Customs
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

package viewmodels.checkAnswers

import models.CheckMode
import models.identifiers.PeriodKey
import models.returns.{DutyRate, ReturnsUserAnswers}
import models.returns.adjustments.{AdjustmentList, AdjustmentType}
import pages.returns.{DeclareDutyPage, EnterDutyAmountPage}
import pages.returns.adjustments.{AdjustmentListPage, AdjustmentReasonPage, DeclareAdjustmentPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import utils.{CssConstants, CurrencyFormatter}
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object ReturnsSummary extends CurrencyFormatter {

  // scalafix:off DisableSyntax.throw
  private case class AdjustmentTotals(underDuty: BigDecimal, overDuty: BigDecimal) {
    def netAdjustment: BigDecimal = underDuty - overDuty
  }

  private def calculateAdjustmentTotals(
    adjustmentList: AdjustmentList,
    dutyRates: Map[PeriodKey, DutyRate]
  ): AdjustmentTotals = {
    val underDuty = adjustmentList.adjustments
      .filter(_.adjustmentType == AdjustmentType.UnderDeclared)
      .map(adj => dutyRates.get(adj.period).map(_.calculateDuty(adj.volumeInMl)).getOrElse(BigDecimal(0)))
      .sum
    
    val overDuty = adjustmentList.adjustments
      .filter(_.adjustmentType == AdjustmentType.OverDeclared)
      .map(adj => dutyRates.get(adj.period).map(_.calculateDuty(adj.volumeInMl)).getOrElse(BigDecimal(0)))
      .sum
    
    AdjustmentTotals(underDuty, overDuty)
  }

  def summaryList(
                   answers: ReturnsUserAnswers,
                   dutyRates: Map[PeriodKey, DutyRate],
                   periodKey: PeriodKey
  )(implicit messages: Messages): SummaryList = {
    val rows = Seq(
      buildDeclareDutyRow(answers, periodKey),
      buildDutyRow(answers, dutyRates.getOrElse(periodKey, throw new IllegalStateException(s"No duty rate found for period $periodKey")), periodKey),
      //      buildSpoiltRow(answers, periodKey),
      buildAdjustmentQuestionRow(answers, periodKey),
      buildCombinedAdjustmentsRow(answers, dutyRates, periodKey),
      buildAdjustmentReasonRow(answers, dutyRates, periodKey),
      buildTotalDutyRow(answers, dutyRates, periodKey)
    ).flatten

    SummaryList(rows = rows, classes = CssConstants.marginBottom9)
  }

  private def buildDeclareDutyRow(
    answers: ReturnsUserAnswers,
    periodKey: PeriodKey
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage).map { answer =>
      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.declareDuty.vaping",
        value = ValueViewModel(Text(messages(if (answer) "site.yes" else "site.no"))),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            s"${controllers.returns.submit.routes.DeclareDutyController.onPageLoad(CheckMode).url}?period=${periodKey.value}"
          ).withVisuallyHiddenText(messages("returns.CheckYourAnswers.declareDuty.vaping"))
        )
      )
    }

  private def dutyRow(dutyDue: String, periodKey: PeriodKey)(implicit messages: Messages) = {
    Option(SummaryListRowViewModel(
      key = "returns.CheckYourAnswers.dutySummary.vaping",
      value = ValueViewModel(Text(dutyDue)),
      actions = Seq(
        ActionItemViewModel(
          "site.change", 
          s"${controllers.returns.submit.routes.EnterDutyAmountController.onPageLoad(CheckMode).url}?period=${periodKey.value}"
        ).withVisuallyHiddenText(messages(""))
      )
    ))
  }

  private def buildDutyRow(
                            answers: ReturnsUserAnswers,
                            dutyRate: DutyRate,
                            periodKey: PeriodKey
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage) match {
      case Some(true) =>
        answers.get(EnterDutyAmountPage) match {
          case Some(volumeInMl) if volumeInMl == 0 => dutyRow(messages("returns.CheckYourAnswers.dutySummary.nothing"), periodKey)
          case Some(volumeInMl) => 
            val dutyDue = dutyRate.calculateDuty(volumeInMl)
            dutyRow(currencyFormat(dutyDue), periodKey)
          case None => dutyRow(messages("returns.CheckYourAnswers.dutySummary.nothing"), periodKey)
        }
      case _ => None
    }

  private def buildSpoiltRow(answers: ReturnsUserAnswers, periodKey: PeriodKey)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage).map { answer =>
      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.dutySummary.spoilt",
        value = ValueViewModel(""),
        actions = Seq(
          ActionItemViewModel("site.change", controllers.returns.submit.routes.BeforeYouStartController.onPageLoad().url)
            .withVisuallyHiddenText(messages(""))
        )
      )
    }

  private def totalDutyRow(value: String)(implicit messages: Messages) = {
    Option(SummaryListRowViewModel(
      key = "returns.CheckYourAnswers.dutySummary.total",
      value = ValueViewModel(Text(value)).withCssClass("govuk-!-font-weight-bold"),
      actions = Seq.empty
    ))
  }

  private def buildTotalDutyRow(
                                 answers: ReturnsUserAnswers,
                                 dutyRates: Map[PeriodKey, DutyRate],
                                 periodKey: PeriodKey
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage) match {
      case Some(true) =>
        answers.get(EnterDutyAmountPage) match {
          case Some(volumeInMl) => 
            val currentPeriodRate = dutyRates.getOrElse(periodKey, throw new IllegalStateException(s"No duty rate found for period $periodKey"))
            val vapingProductsDuty = currentPeriodRate.calculateDuty(volumeInMl)
            
            // Calculate adjustment totals
            val adjustmentTotal = answers.get(AdjustmentListPage).map { list =>
              val totals = calculateAdjustmentTotals(list, dutyRates)
              totals.netAdjustment
            }.getOrElse(BigDecimal(0))
            
            val totalDuty = vapingProductsDuty + adjustmentTotal
            totalDutyRow(currencyFormat(totalDuty))
          case None => totalDutyRow(messages("returns.CheckYourAnswers.dutySummary.total.nil"))
        }
      case _ => None
    }

  private def buildAdjustmentQuestionRow(
    answers: ReturnsUserAnswers,
    periodKey: PeriodKey
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareAdjustmentPage).map { answer =>
      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.adjustments.question",
        value = ValueViewModel(Text(messages(if (answer) "site.yes" else "site.no"))),
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            s"${controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad(CheckMode).url}?period=${periodKey.value}"
          ).withVisuallyHiddenText(messages("returns.CheckYourAnswers.adjustments.question.change.hidden"))
        )
      )
    }

  private def buildCombinedAdjustmentsRow(
    answers: ReturnsUserAnswers,
    dutyRates: Map[PeriodKey, DutyRate],
    periodKey: PeriodKey
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AdjustmentListPage).flatMap { list =>
      if (list.adjustments.nonEmpty) {
        val totals = calculateAdjustmentTotals(list, dutyRates)
        
        Some(SummaryListRowViewModel(
          key = "returns.CheckYourAnswers.adjustments.combined",
          value = ValueViewModel(Text(currencyFormatWithLeadingSign(totals.netAdjustment))),
          actions = Seq(
            ActionItemViewModel(
              "site.change",
              s"${controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad(CheckMode).url}?period=${periodKey.value}"
            ).withVisuallyHiddenText(messages("returns.CheckYourAnswers.adjustments.combined.change.hidden"))
          )
        ))
      } else None
    }

  private def buildAdjustmentReasonRow(
    answers: ReturnsUserAnswers,
    dutyRates: Map[PeriodKey, DutyRate],
    periodKey: PeriodKey
  )(implicit messages: Messages): Option[SummaryListRow] = {
    val shouldShowReason = answers.get(AdjustmentListPage).exists { list =>
      val totals = calculateAdjustmentTotals(list, dutyRates)
      totals.underDuty >= AdjustmentType.dutyThreshold || totals.overDuty >= AdjustmentType.dutyThreshold
    }

    if (shouldShowReason) {
      answers.get(AdjustmentReasonPage).map { reason =>
        SummaryListRowViewModel(
          key = "returns.CheckYourAnswers.adjustments.reason",
          value = ValueViewModel(Text(reason)),
          actions = Seq(
            ActionItemViewModel(
              "site.change",
              s"${controllers.returns.submit.routes.AdjustmentReasonController.onPageLoad(CheckMode).url}?period=${periodKey.value}"
            ).withVisuallyHiddenText(messages("returns.CheckYourAnswers.adjustments.reason.change.hidden"))
          )
        )
      }
    } else None
  }
}
