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

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.CssConstants

final case class DutyDeclarationSectionBuilder(
  hasVapingProductsDeclaration: Boolean,
  amountProducedLiquid: Option[String],
  dutyDue: Option[String]
) {

  def build()(implicit messages: Messages): Option[SummaryList] = {
    val detailRows = if (hasVapingProductsDeclaration) {
      Seq(
        amountProducedLiquid.map { amount =>
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.amountProducedLiquid"))),
            value = Value(content = Text(messages("viewIndividualReturn.millilitres", amount)))
          )
        },
        dutyDue.map { duty =>
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.dutyDue"))),
            value = Value(content = Text(duty), classes = CssConstants.boldFontWeight)
          )
        }
      ).flatten
    } else Seq.empty

    Some(SummaryList(
      rows = Seq(
        SummaryListRow(
          key = Key(content = Text(messages("viewIndividualReturn.vapingProductsDeclaration.question"))),
          value = Value(
            content = Text(
              if (hasVapingProductsDeclaration) {
                messages("viewIndividualReturn.vapingProductsDeclaration.yes")
              } else {
                messages("viewIndividualReturn.vapingProductsDeclaration.no")
              }
            )
          )
        )) ++ detailRows
    ))
  }
}