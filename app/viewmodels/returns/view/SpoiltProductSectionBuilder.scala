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

import models.obligations.ObligationsResponse
import models.returns.ConvertToMl
import models.returns.view.SpoiltProduct
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.{CssConstants, CurrencyFormatter, ReturnsDateUtils}

final case class SpoiltProductSectionBuilder(
  spoiltProduct: Option[SpoiltProduct],
  nilReturn: Boolean,
  totalDutySpoiltProducts: String,
  obligations: ObligationsResponse,
  returnsDateUtils: ReturnsDateUtils
) extends CurrencyFormatter {

  def build()(implicit messages: Messages): Seq[SummaryList] = {
    spoiltProduct match {
      case Some(sp) =>
        val yesNoText = if (sp.spoiltProductFilled == "1") {
          messages("viewIndividualReturn.spoiltProducts.yes")
        } else {
          messages("viewIndividualReturn.spoiltProducts.no")
        }

        val questionRow = SummaryListRow(
          key = Key(content = Text(messages("viewIndividualReturn.spoiltProducts.question"))),
          value = Value(content = Text(yesNoText))
        )

        if (sp.spoiltProductFilled == "1") {
          val items = sp.spoiltProducts.getOrElse(Seq.empty)
          
          if (items.isEmpty) {
            Seq(SummaryList(rows = Seq(questionRow)))
          } else {
            // First list includes question + first item
            val firstItem = items.head
            val firstDutyDue = currencyFormat(firstItem.dutyDue.abs).replace("£", "-£")
            val firstList = SummaryList(rows = Seq(
              questionRow,
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.spoiltProducts.month"))),
                value = Value(content = Text(lookupPeriodKey(firstItem.returnPeriodAffected)))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.spoiltProducts.spoiltProducts"))),
                value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(firstItem.amountSpoilt).toMl))))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.totalDutySpoiltProducts"))),
                value = Value(content = Text(firstDutyDue), classes = CssConstants.boldFontWeight)
              )
            ))
            
            // Remaining items get their own lists
            val remainingLists = items.tail.map { item =>
              val dutyDue = currencyFormat(item.dutyDue.abs).replace("£", "-£")
              SummaryList(rows = Seq(
                SummaryListRow(
                  key = Key(content = Text(messages("viewIndividualReturn.spoiltProducts.month"))),
                  value = Value(content = Text(lookupPeriodKey(item.returnPeriodAffected)))
                ),
                SummaryListRow(
                  key = Key(content = Text(messages("viewIndividualReturn.spoiltProducts.spoiltProducts"))),
                  value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(item.amountSpoilt).toMl))))
                ),
                SummaryListRow(
                  key = Key(content = Text(messages("viewIndividualReturn.totalDutySpoiltProducts"))),
                  value = Value(content = Text(dutyDue), classes = CssConstants.boldFontWeight)
                )
              ))
            }
            Seq(firstList) ++ remainingLists
          }
        } else {
          Seq(SummaryList(rows = Seq(questionRow)))
        }
      case None => Seq.empty
    }
  }

  private def lookupPeriodKey(periodKey: String)(implicit messages: Messages): String = {
    obligations.obligation
      .map(_.obligationDetails)
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