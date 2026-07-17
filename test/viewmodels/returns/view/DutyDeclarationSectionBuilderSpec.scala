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

import base.SpecBase
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import utils.CssConstants

class DutyDeclarationSectionBuilderSpec extends SpecBase {

  implicit val messages: Messages = messages(applicationBuilder(None).build())

  "DutyDeclarationSectionBuilder" - {

    "build must" - {

      "return Some with 3 rows when declaration exists" in {
        val builder = DutyDeclarationSectionBuilder(
          hasVapingProductsDeclaration = true,
          amountProducedLiquid = Some("2,000,000.00"),
          dutyDue = Some("£1,000")
        )
        val result = builder.build()

        result mustBe defined
        result.get.rows.size mustBe 3
      }

      "return Some with 1 row when no declaration" in {
        val builder = DutyDeclarationSectionBuilder(
          hasVapingProductsDeclaration = false,
          amountProducedLiquid = None,
          dutyDue = None
        )
        val result = builder.build()

        result mustBe defined
        result.get.rows.size mustBe 1
      }

      "include question row with 'Yes' when declaration exists" in {
        val builder = DutyDeclarationSectionBuilder(
          hasVapingProductsDeclaration = true,
          amountProducedLiquid = Some("2,000,000.00"),
          dutyDue = Some("£1,000")
        )
        val result = builder.build()

        result.get.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.vapingProductsDeclaration.question"))
        result.get.rows.head.value.content mustBe Text(messages("viewIndividualReturn.vapingProductsDeclaration.yes"))
      }

      "include question row with 'No' when no declaration" in {
        val builder = DutyDeclarationSectionBuilder(
          hasVapingProductsDeclaration = false,
          amountProducedLiquid = None,
          dutyDue = None
        )
        val result = builder.build()

        result.get.rows.head.value.content mustBe Text(messages("viewIndividualReturn.vapingProductsDeclaration.no"))
      }

      "include amount produced row when declaration exists" in {
        val builder = DutyDeclarationSectionBuilder(
          hasVapingProductsDeclaration = true,
          amountProducedLiquid = Some("2,000,000.00"),
          dutyDue = Some("£1,000")
        )
        val result = builder.build()

        result.get.rows(1).key.content.asHtml.toString must include(messages("viewIndividualReturn.amountProducedLiquid"))
        result.get.rows(1).value.content.asHtml.toString must include("2,000,000.00 ml")
      }

      "include duty due row with bold formatting when declaration exists" in {
        val builder = DutyDeclarationSectionBuilder(
          hasVapingProductsDeclaration = true,
          amountProducedLiquid = Some("2,000,000.00"),
          dutyDue = Some("£1,000")
        )
        val result = builder.build()

        result.get.rows(2).key.content.asHtml.toString must include(messages("viewIndividualReturn.dutyDue"))
        result.get.rows(2).value.content.asHtml.toString must include("£1,000")
        result.get.rows(2).value.classes must include(CssConstants.boldFontWeight)
      }

      "not include amount or duty rows when no declaration" in {
        val builder = DutyDeclarationSectionBuilder(
          hasVapingProductsDeclaration = false,
          amountProducedLiquid = None,
          dutyDue = None
        )
        val result = builder.build()

        result.get.rows.size mustBe 1
        result.get.rows.forall(row =>
          !row.key.content.asHtml.toString.contains(messages("viewIndividualReturn.amountProducedLiquid")) &&
          !row.key.content.asHtml.toString.contains(messages("viewIndividualReturn.dutyDue"))
        ) mustBe true
      }
    }
  }
}