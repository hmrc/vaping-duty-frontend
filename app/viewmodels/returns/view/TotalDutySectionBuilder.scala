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

import models.returns.TotalDutyDue
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.{CssConstants, CurrencyFormatter}

final case class TotalDutySectionBuilder(totalDutyDue: Option[TotalDutyDue]) extends CurrencyFormatter {

  def build()(implicit messages: Messages): SummaryList = {
    val zeroValue = BigDecimal("0")

    totalDutyDue match {
      case Some(tdd) =>
        val rows = Seq(
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.totals.totalDutyDueVapingProducts")), classes = CssConstants.boldFontWeight),
            value = Value(content = Text(currencyFormat(tdd.totalDutyDueVapingProducts)))
          ),
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.totals.totalDutySpoiltProduct")), classes = CssConstants.boldFontWeight),
            value = Value(content = Text(
              if (tdd.totalDutySpoiltProduct == zeroValue) currencyFormat(tdd.totalDutySpoiltProduct)
              else currencyFormat(tdd.totalDutySpoiltProduct.abs).replace("£", "-£")
            ))
          ),
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.totals.totalDutyUnderDeclaration")), classes = CssConstants.boldFontWeight),
            value = Value(content = Text(currencyFormat(tdd.totalDutyUnderDeclaration)))
          ),
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.totals.totalDutyOverDeclaration")), classes = CssConstants.boldFontWeight),
            value = Value(content = Text(
              if (tdd.totalDutyOverDeclaration == zeroValue) currencyFormat(tdd.totalDutyOverDeclaration)
              else currencyFormat(tdd.totalDutyOverDeclaration.abs).replace("£", "-£")
            ))
          ),
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.totals.totalDue")), classes = CssConstants.boldFontWeight),
            value = Value(
              content = Text(
                if (tdd.totalDue < zeroValue) currencyFormat(tdd.totalDue.abs).replace("£", "-£")
                else currencyFormat(tdd.totalDue)
              ),
              classes = CssConstants.boldFontWeight
            )
          )
        )
        SummaryList(rows = rows)
      case None =>
        SummaryList(rows = Seq.empty)
    }
  }
}