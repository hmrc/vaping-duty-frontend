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

package viewmodels.returns.view

import models.obligations.ObligationDetails
import models.returns.ConvertToMl
import models.returns.view.{OverDeclaration, UnderDeclaration}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.{CssConstants, CurrencyFormatter, ReturnsDateUtils}

final case class AdjustmentsSectionBuilder(
  overDeclaration: Option[OverDeclaration],
  underDeclaration: Option[UnderDeclaration],
  obligationDetails: Seq[ObligationDetails],
  returnsDateUtils: ReturnsDateUtils
) extends CurrencyFormatter {

  def build()(implicit messages: Messages): Seq[SummaryList] = {
    val hasOverDeclarations = overDeclaration.exists(_.overDeclFilled == "1")
    val hasUnderDeclarations = underDeclaration.exists(_.underDeclFilled == "1")
    val hasAnyAdjustments = hasOverDeclarations || hasUnderDeclarations

    val questionRow = SummaryListRow(
      key = Key(content = Text(messages("viewIndividualReturn.adjustments.question"))),
      value = Value(content = Text(
        if (hasAnyAdjustments) {
          messages("viewIndividualReturn.adjustments.yes")
        } else {
          messages("viewIndividualReturn.adjustments.no")
        }
      ))
    )

    if (hasAnyAdjustments) {
      val underDeclarationItems = underDeclaration
        .flatMap(_.underDeclarationProducts)
        .getOrElse(Seq.empty)

      val overDeclarationItems = overDeclaration
        .flatMap(_.overDeclarationProducts)
        .getOrElse(Seq.empty)

      val allItems = underDeclarationItems ++ overDeclarationItems

      if (allItems.isEmpty) {
        Seq(SummaryList(rows = Seq(questionRow)))
      } else {
        // First list includes question + first item
        val firstList = if (underDeclarationItems.nonEmpty) {
          val firstItem = underDeclarationItems.head
          SummaryList(rows = Seq(
            questionRow,
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.returnPeriodAffected"))),
              value = Value(content = Text(lookupPeriodKey(firstItem.returnPeriodAffected)))
            ),
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.amountUnderDeclared"))),
              value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(firstItem.amountUnderDeclared).toMl))))
            ),
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.dutyDue"))),
              value = Value(content = Text(currencyFormat(firstItem.dutyDue)), classes = CssConstants.boldFontWeight)
            )
          ))
        } else {
          val firstItem = overDeclarationItems.head
          val dutyDueFormatted = currencyFormat(firstItem.dutyDue.abs).replace("£", "-£")
          SummaryList(rows = Seq(
            questionRow,
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.returnPeriodAffected"))),
              value = Value(content = Text(lookupPeriodKey(firstItem.returnPeriodAffected)))
            ),
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.amountOverDeclared"))),
              value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(firstItem.amountOverDeclared).toMl))))
            ),
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.dutyDue"))),
              value = Value(content = Text(dutyDueFormatted), classes = CssConstants.boldFontWeight)
            )
          ))
        }

        // Remaining under declarations
        val remainingUnderLists = if (underDeclarationItems.nonEmpty) {
          underDeclarationItems.tail.map { item =>
            SummaryList(rows = Seq(
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.returnPeriodAffected"))),
                value = Value(content = Text(lookupPeriodKey(item.returnPeriodAffected)))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.amountUnderDeclared"))),
                value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(item.amountUnderDeclared).toMl))))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.dutyDue"))),
                value = Value(content = Text(currencyFormat(item.dutyDue)), classes = CssConstants.boldFontWeight)
              )
            ))
          }
        } else {
          Seq.empty
        }

        // All over declarations (or remaining if first was over)
        val overLists = if (underDeclarationItems.isEmpty) {
          overDeclarationItems.tail.map { item =>
            val dutyDueFormatted = currencyFormat(item.dutyDue.abs).replace("£", "-£")
            SummaryList(rows = Seq(
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.returnPeriodAffected"))),
                value = Value(content = Text(lookupPeriodKey(item.returnPeriodAffected)))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.amountOverDeclared"))),
                value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(item.amountOverDeclared).toMl))))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.dutyDue"))),
                value = Value(content = Text(dutyDueFormatted), classes = CssConstants.boldFontWeight)
              )
            ))
          }
        } else {
          overDeclarationItems.map { item =>
            val dutyDueFormatted = currencyFormat(item.dutyDue.abs).replace("£", "-£")
            SummaryList(rows = Seq(
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.returnPeriodAffected"))),
                value = Value(content = Text(lookupPeriodKey(item.returnPeriodAffected)))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.amountOverDeclared"))),
                value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(item.amountOverDeclared).toMl))))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.dutyDue"))),
                value = Value(content = Text(dutyDueFormatted), classes = CssConstants.boldFontWeight)
              )
            ))
          }
        }

        val adjustmentReason = underDeclaration.flatMap(_.reasonForUnderDecl)
          .orElse(overDeclaration.flatMap(_.reasonForOverDecl))

        val reasonList = adjustmentReason.map { reason =>
          SummaryList(rows = Seq(
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.reason"))),
              value = Value(content = Text(reason))
            )
          ))
        }.toSeq

        Seq(firstList) ++ remainingUnderLists ++ overLists ++ reasonList
      }
    } else {
      Seq(SummaryList(rows = Seq(questionRow)))
    }
  }

  private def lookupPeriodKey(periodKey: String)(implicit messages: Messages): String = {
    obligationDetails
      .find(_.periodKey == periodKey)
      .map { obligation =>
        val month = obligation.iCFromDate.getMonth
        val year = obligation.iCFromDate.getYear
        s"${returnsDateUtils.getMonthMessage(month)} $year"
      }
      .getOrElse {
        // scalafix:off DisableSyntax.throw
        throw new IllegalStateException(s"Period key $periodKey not found in obligations. This indicates a data integrity issue.")
      }
  }
}