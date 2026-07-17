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
import data.TestData
import models.identifiers.PeriodKey
import models.obligations.{ObligationItem, ObligationsResponse}
import models.returns.view.{OverDeclaration, OverDeclarationProduct, UnderDeclaration, UnderDeclarationProduct}
import play.api.i18n.Messages
import utils.ReturnsDateUtils

class AdjustmentsSectionBuilderSpec extends SpecBase with TestData {

  implicit val messages: Messages = messages(applicationBuilder(None).build())

  private val returnsDateUtils = new ReturnsDateUtils(clock)
  private val obligationDetails = fulfilledObligation(periodKey)
  private val obligationDetailsJuly = fulfilledObligation(PeriodKey("24AG"))
  private val obligations = ObligationsResponse(
    obligation = Seq(
      ObligationItem(None, obligationDetails),
      ObligationItem(None, obligationDetailsJuly)
    )
  )

  private val underDeclarationProduct = UnderDeclarationProduct(
    returnPeriodAffected = "24AG",
    taxType = "614",
    dutyRate = BigDecimal("1.00"),
    amountUnderDeclared = BigDecimal("100.00"),
    dutyDue = BigDecimal("100.00")
  )

  private val overDeclarationProduct = OverDeclarationProduct(
    returnPeriodAffected = "24AG",
    taxType = "614",
    dutyRate = BigDecimal("1.00"),
    amountOverDeclared = BigDecimal("50.00"),
    dutyDue = BigDecimal("-50.00")
  )

  "AdjustmentsSectionBuilder" - {

    "build must" - {

      "return single list with 'No' when no adjustments" in {
        val builder = AdjustmentsSectionBuilder(
          overDeclaration = Some(OverDeclaration("0", None, None)),
          underDeclaration = Some(UnderDeclaration("0", None, None)),
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.size mustBe 1
        result.head.rows.size mustBe 1
        result.head.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.adjustments.question"))
        result.head.rows.head.value.content.asHtml.toString must include(messages("viewIndividualReturn.adjustments.no"))
      }

      "display under declaration correctly with single item" in {
        val underDeclaration = UnderDeclaration(
          "1",
          Some("Additional products found"),
          Some(Seq(underDeclarationProduct))
        )
        val builder = AdjustmentsSectionBuilder(
          overDeclaration = None,
          underDeclaration = Some(underDeclaration),
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.size mustBe 2
        result.head.rows.size mustBe 4  // Question + under declaration details
        result(1).rows.size mustBe 1     // Reason
      }

      "display over declaration correctly with single item" in {
        val overDeclaration = OverDeclaration(
          "1",
          Some("Correction of previous return"),
          Some(Seq(overDeclarationProduct))
        )
        val builder = AdjustmentsSectionBuilder(
          overDeclaration = Some(overDeclaration),
          underDeclaration = None,
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.size mustBe 2
        result.head.rows.size mustBe 4  // Question + over declaration details
        result(1).rows.size mustBe 1     // Reason
      }

      "display both under and over declarations correctly" in {
        val underDeclaration = UnderDeclaration(
          "1",
          Some("Additional products found"),
          Some(Seq(underDeclarationProduct))
        )
        val overDeclaration = OverDeclaration(
          "1",
          Some("Correction of previous return"),
          Some(Seq(overDeclarationProduct))
        )
        val builder = AdjustmentsSectionBuilder(
          overDeclaration = Some(overDeclaration),
          underDeclaration = Some(underDeclaration),
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.size mustBe 3
        result.head.rows.size mustBe 4  // Question + under declaration
        result(1).rows.size mustBe 3     // Over declaration
        result(2).rows.size mustBe 1     // Reason
      }

      "format duty due correctly for under declarations" in {
        val underDeclaration = UnderDeclaration(
          "1",
          Some("Additional products found"),
          Some(Seq(underDeclarationProduct))
        )
        val builder = AdjustmentsSectionBuilder(
          overDeclaration = None,
          underDeclaration = Some(underDeclaration),
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.head.rows(3).value.content.asHtml.toString must include("£100")
        result.head.rows(3).value.content.asHtml.toString mustNot include("-£100")
      }

      "format duty due with minus for over declarations" in {
        val overDeclaration = OverDeclaration(
          "1",
          Some("Correction of previous return"),
          Some(Seq(overDeclarationProduct))
        )
        val builder = AdjustmentsSectionBuilder(
          overDeclaration = Some(overDeclaration),
          underDeclaration = None,
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.head.rows(3).value.content.asHtml.toString must include("-£50")
      }

      "format millilitres correctly" in {
        val underDeclaration = UnderDeclaration(
          "1",
          Some("Additional products found"),
          Some(Seq(underDeclarationProduct))
        )
        val builder = AdjustmentsSectionBuilder(
          overDeclaration = None,
          underDeclaration = Some(underDeclaration),
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.head.rows(2).value.content.asHtml.toString must include("100,000.00 ml")
      }

      "lookup period key correctly" in {
        val underDeclaration = UnderDeclaration(
          "1",
          Some("Additional products found"),
          Some(Seq(underDeclarationProduct))
        )
        val builder = AdjustmentsSectionBuilder(
          overDeclaration = None,
          underDeclaration = Some(underDeclaration),
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.head.rows(1).value.content.asHtml.toString must include("July 2024")
      }

      "include reason from under declaration when both exist" in {
        val underDeclaration = UnderDeclaration(
          "1",
          Some("Additional products found"),
          Some(Seq(underDeclarationProduct))
        )
        val overDeclaration = OverDeclaration(
          "1",
          Some("Correction of previous return"),
          Some(Seq(overDeclarationProduct))
        )
        val builder = AdjustmentsSectionBuilder(
          overDeclaration = Some(overDeclaration),
          underDeclaration = Some(underDeclaration),
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.last.rows.head.value.content.asHtml.toString must include("Additional products found")
      }

      "handle multiple under declarations" in {
        val product2 = underDeclarationProduct.copy(amountUnderDeclared = BigDecimal("200.00"), dutyDue = BigDecimal("200.00"))
        val underDeclaration = UnderDeclaration(
          "1",
          Some("Additional products found"),
          Some(Seq(underDeclarationProduct, product2))
        )
        val builder = AdjustmentsSectionBuilder(
          overDeclaration = None,
          underDeclaration = Some(underDeclaration),
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.size mustBe 3  // First with question, second item, reason
        result.head.rows.size mustBe 4
        result(1).rows.size mustBe 3
        result(2).rows.size mustBe 1
      }

      "handle multiple over declarations" in {
        val product2 = overDeclarationProduct.copy(amountOverDeclared = BigDecimal("75.00"), dutyDue = BigDecimal("-75.00"))
        val overDeclaration = OverDeclaration(
          "1",
          Some("Correction of previous return"),
          Some(Seq(overDeclarationProduct, product2))
        )
        val builder = AdjustmentsSectionBuilder(
          overDeclaration = Some(overDeclaration),
          underDeclaration = None,
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.size mustBe 3  // First with question, second item, reason
        result.head.rows.size mustBe 4
        result(1).rows.size mustBe 3
        result(2).rows.size mustBe 1
      }
    }
  }
}