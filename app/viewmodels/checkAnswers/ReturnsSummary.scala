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

  def summaryList(
                   answers: ReturnsUserAnswers,
                   dutyRate: DutyRate,
                   periodKey: PeriodKey
  )(implicit messages: Messages): SummaryList = {
    val rows = Seq(
      buildDeclareDutyRow(answers, periodKey),
      buildDutyRow(answers, dutyRate, periodKey),
      //      buildSpoiltRow(answers, periodKey),
      buildAdjustmentQuestionRow(answers, periodKey),
      buildUnderDeclaredRow(answers, dutyRate, periodKey),
      buildOverDeclaredRow(answers, dutyRate, periodKey),
      buildAdjustmentReasonRow(answers, dutyRate, periodKey),
      buildTotalDutyRow(answers, dutyRate)
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

  private def buildSpoiltRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
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

  private def buildOverRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage).map { answer =>
      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.dutySummary.over",
        value = ValueViewModel(""),
        actions = Seq(
          ActionItemViewModel("site.change", controllers.returns.submit.routes.BeforeYouStartController.onPageLoad().url)
            .withVisuallyHiddenText(messages(""))
        )
      )
    }

  private def buildUnderRow(answers: ReturnsUserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage).map { answer =>
      SummaryListRowViewModel(
        key = "returns.CheckYourAnswers.dutySummary.under",
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
                                 dutyRate: DutyRate
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DeclareDutyPage) match {
      case Some(true) =>
        answers.get(EnterDutyAmountPage) match {
          case Some(volumeInMl) => 
            val vapingProductsDuty = dutyRate.calculateDuty(volumeInMl)
            
            // Calculate adjustment totals
            val adjustmentTotal = answers.get(AdjustmentListPage).map { list =>
              val underDuty = list.adjustments
                .filter(_.adjustmentType == AdjustmentType.UnderDeclared)
                .map(adj => dutyRate.calculateDuty(adj.volumeInMl))
                .sum
              val overDuty = list.adjustments
                .filter(_.adjustmentType == AdjustmentType.OverDeclared)
                .map(adj => dutyRate.calculateDuty(adj.volumeInMl))
                .sum
              underDuty - overDuty
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
            s"${controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad().url}?period=${periodKey.value}"
          ).withVisuallyHiddenText(messages("returns.CheckYourAnswers.adjustments.question.change.hidden"))
        )
      )
    }

  private def buildUnderDeclaredRow(
    answers: ReturnsUserAnswers,
    dutyRate: DutyRate,
    periodKey: PeriodKey
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AdjustmentListPage).flatMap { list =>
      val underAdjustments = list.adjustments.filter(_.adjustmentType == AdjustmentType.UnderDeclared)
      if (underAdjustments.nonEmpty) {
        val totalDuty = underAdjustments.map(adj => dutyRate.calculateDuty(adj.volumeInMl)).sum
        Some(SummaryListRowViewModel(
          key = "returns.CheckYourAnswers.adjustments.underDeclared",
          value = ValueViewModel(Text(currencyFormatWithLeadingSign(totalDuty))),
          actions = Seq(
            ActionItemViewModel(
              "site.change",
              s"${controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad().url}?period=${periodKey.value}"
            ).withVisuallyHiddenText(messages("returns.CheckYourAnswers.adjustments.underDeclared.change.hidden"))
          )
        ))
      } else None
    }

  private def buildOverDeclaredRow(
    answers: ReturnsUserAnswers,
    dutyRate: DutyRate,
    periodKey: PeriodKey
  )(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AdjustmentListPage).flatMap { list =>
      val overAdjustments = list.adjustments.filter(_.adjustmentType == AdjustmentType.OverDeclared)
      if (overAdjustments.nonEmpty) {
        val totalDuty = overAdjustments.map(adj => dutyRate.calculateDuty(adj.volumeInMl)).sum
        Some(SummaryListRowViewModel(
          key = "returns.CheckYourAnswers.adjustments.overDeclared",
          value = ValueViewModel(Text(currencyFormatWithLeadingSign(-totalDuty))),
          actions = Seq(
            ActionItemViewModel(
              "site.change",
              s"${controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad().url}?period=${periodKey.value}"
            ).withVisuallyHiddenText(messages("returns.CheckYourAnswers.adjustments.overDeclared.change.hidden"))
          )
        ))
      } else None
    }

  private def buildAdjustmentReasonRow(
    answers: ReturnsUserAnswers,
    dutyRate: DutyRate,
    periodKey: PeriodKey
  )(implicit messages: Messages): Option[SummaryListRow] = {
    val shouldShowReason = answers.get(AdjustmentListPage).exists { list =>
      val underDuty = list.adjustments
        .filter(_.adjustmentType == AdjustmentType.UnderDeclared)
        .map(adj => dutyRate.calculateDuty(adj.volumeInMl))
        .sum
      val overDuty = list.adjustments
        .filter(_.adjustmentType == AdjustmentType.OverDeclared)
        .map(adj => dutyRate.calculateDuty(adj.volumeInMl))
        .sum
      
      underDuty >= AdjustmentType.dutyThreshold || overDuty >= AdjustmentType.dutyThreshold
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
