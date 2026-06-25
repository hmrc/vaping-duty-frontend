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

package services.returns

import base.SpecBase
import models.returns.view.{OverDeclaration, OverDeclarationProduct, SpoiltProduct, SpoiltProductItem, UnderDeclaration, UnderDeclarationProduct}

class TotalDutyDueCalculationServiceSpec extends SpecBase {

  private val service = new TotalDutyDueCalculationService()

  private val ZERO_VALUE = BigDecimal("0")

  "TotalDutyDueCalculationService" - {

    "calculate must" - {

      "return correct totals when no adjustments" in {
        val totalDutyDueVapingProducts = BigDecimal("100.00")

        val result = service.calculate(
          totalDutyDueVapingProducts,
          None,
          None,
          None
        )

        result.totalDutyDueVapingProducts mustBe BigDecimal("100.00")
        result.totalDutyOverDeclaration mustBe ZERO_VALUE
        result.totalDutyUnderDeclaration mustBe ZERO_VALUE
        result.totalDutySpoiltProduct mustBe ZERO_VALUE
        result.adjustmentAmount mustBe ZERO_VALUE
        result.totalDue mustBe BigDecimal("100.00")
      }

      "return correct totals with under declaration" in {
        val totalDutyDueVapingProducts = BigDecimal("100.00")
        val underDeclaration = Some(UnderDeclaration(
          underDeclFilled = "1",
          reasonForUnderDecl = Some("Test reason"),
          underDeclarationProducts = Some(Seq(
            UnderDeclarationProduct("23KB", "641", BigDecimal("10.50"), BigDecimal("10.00"), BigDecimal("105.00"))
          ))
        ))

        val result = service.calculate(
          totalDutyDueVapingProducts,
          underDeclaration,
          None,
          None
        )

        result.totalDutyDueVapingProducts mustBe BigDecimal("100.00")
        result.totalDutyOverDeclaration mustBe ZERO_VALUE
        result.totalDutyUnderDeclaration mustBe BigDecimal("105.00")
        result.totalDutySpoiltProduct mustBe ZERO_VALUE
        result.adjustmentAmount mustBe BigDecimal("105.00")
        result.totalDue mustBe BigDecimal("205.00")
      }

      "return correct totals with over declaration" in {
        val totalDutyDueVapingProducts = BigDecimal("100.00")
        val overDeclaration = Some(OverDeclaration(
          overDeclFilled = "1",
          reasonForOverDecl = Some("Test reason"),
          overDeclarationProducts = Some(Seq(
            OverDeclarationProduct("23KC", "641", BigDecimal("10.50"), BigDecimal("5.00"), BigDecimal("52.50"))
          ))
        ))

        val result = service.calculate(
          totalDutyDueVapingProducts,
          None,
          overDeclaration,
          None
        )

        result.totalDutyDueVapingProducts mustBe BigDecimal("100.00")
        result.totalDutyOverDeclaration mustBe BigDecimal("52.50")
        result.totalDutyUnderDeclaration mustBe ZERO_VALUE
        result.totalDutySpoiltProduct mustBe ZERO_VALUE
        result.adjustmentAmount mustBe BigDecimal("-52.50")
        result.totalDue mustBe BigDecimal("47.50")
      }

      "return correct totals with spoilt product" in {
        val totalDutyDueVapingProducts = BigDecimal("100.00")
        val spoiltProduct = Some(SpoiltProduct(
          spoiltProductFilled = "1",
          spoiltProducts = Some(Seq(
            SpoiltProductItem("24KA", "641", BigDecimal("10.50"), BigDecimal("3.00"), BigDecimal("31.50"))
          ))
        ))

        val result = service.calculate(
          totalDutyDueVapingProducts,
          None,
          None,
          spoiltProduct
        )

        result.totalDutyDueVapingProducts mustBe BigDecimal("100.00")
        result.totalDutyOverDeclaration mustBe ZERO_VALUE
        result.totalDutyUnderDeclaration mustBe ZERO_VALUE
        result.totalDutySpoiltProduct mustBe BigDecimal("31.50")
        result.adjustmentAmount mustBe BigDecimal("-31.50")
        result.totalDue mustBe BigDecimal("68.50")
      }

      "return correct totals with multiple adjustments" in {
        val totalDutyDueVapingProducts = BigDecimal("1000.00")
        val underDeclaration = Some(UnderDeclaration(
          underDeclFilled = "1",
          reasonForUnderDecl = Some("Test reason"),
          underDeclarationProducts = Some(Seq(
            UnderDeclarationProduct("23KB", "641", BigDecimal("10.50"), BigDecimal("10.00"), BigDecimal("105.00")),
            UnderDeclarationProduct("23KC", "641", BigDecimal("10.50"), BigDecimal("5.00"), BigDecimal("52.50"))
          ))
        ))
        val overDeclaration = Some(OverDeclaration(
          overDeclFilled = "1",
          reasonForOverDecl = Some("Test reason"),
          overDeclarationProducts = Some(Seq(
            OverDeclarationProduct("23KD", "641", BigDecimal("10.50"), BigDecimal("3.00"), BigDecimal("31.50"))
          ))
        ))
        val spoiltProduct = Some(SpoiltProduct(
          spoiltProductFilled = "1",
          spoiltProducts = Some(Seq(
            SpoiltProductItem("24KA", "641", BigDecimal("10.50"), BigDecimal("2.00"), BigDecimal("21.00"))
          ))
        ))

        val result = service.calculate(
          totalDutyDueVapingProducts,
          underDeclaration,
          overDeclaration,
          spoiltProduct
        )

        result.totalDutyDueVapingProducts mustBe BigDecimal("1000.00")
        result.totalDutyOverDeclaration mustBe BigDecimal("31.50")
        result.totalDutyUnderDeclaration mustBe BigDecimal("157.50")
        result.totalDutySpoiltProduct mustBe BigDecimal("21.00")
        result.adjustmentAmount mustBe BigDecimal("105.00")
        result.totalDue mustBe BigDecimal("1105.00")
      }

      "return zero totals when declarations have no products" in {
        val totalDutyDueVapingProducts = BigDecimal("100.00")
        val underDeclaration = Some(UnderDeclaration(
          underDeclFilled = "0",
          reasonForUnderDecl = None,
          underDeclarationProducts = None
        ))
        val overDeclaration = Some(OverDeclaration(
          overDeclFilled = "0",
          reasonForOverDecl = None,
          overDeclarationProducts = None
        ))
        val spoiltProduct = Some(SpoiltProduct(
          spoiltProductFilled = "0",
          spoiltProducts = None
        ))

        val result = service.calculate(
          totalDutyDueVapingProducts,
          underDeclaration,
          overDeclaration,
          spoiltProduct
        )

        result.totalDutyDueVapingProducts mustBe BigDecimal("100.00")
        result.totalDutyOverDeclaration mustBe ZERO_VALUE
        result.totalDutyUnderDeclaration mustBe ZERO_VALUE
        result.totalDutySpoiltProduct mustBe ZERO_VALUE
        result.adjustmentAmount mustBe ZERO_VALUE
        result.totalDue mustBe BigDecimal("100.00")
      }
    }
  }
}
