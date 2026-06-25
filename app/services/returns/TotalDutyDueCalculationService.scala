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

import models.returns.TotalDutyDue
import models.returns.view.{OverDeclaration, SpoiltProduct, UnderDeclaration}

import javax.inject.Inject

class TotalDutyDueCalculationService @Inject()() {

  private val ZERO_VALUE = BigDecimal("0")

  def calculate(
    totalDutyDueVapingProducts: BigDecimal,
    underDeclaration: Option[UnderDeclaration],
    overDeclaration: Option[OverDeclaration],
    spoiltProduct: Option[SpoiltProduct]
  ): TotalDutyDue = {

    val totalDutyUnderDeclaration = calculateUnderDeclarationTotal(underDeclaration)
    val totalDutyOverDeclaration = calculateOverDeclarationTotal(overDeclaration)
    val totalDutySpoiltProduct = calculateSpoiltProductTotal(spoiltProduct)

    val adjustmentAmount = calculateAdjustmentAmount(
      totalDutyOverDeclaration,
      totalDutyUnderDeclaration,
      totalDutySpoiltProduct
    )

    val totalDue = totalDutyDueVapingProducts + adjustmentAmount

    TotalDutyDue(
      totalDutyDueVapingProducts = totalDutyDueVapingProducts,
      totalDutyOverDeclaration = totalDutyOverDeclaration,
      totalDutyUnderDeclaration = totalDutyUnderDeclaration,
      totalDutySpoiltProduct = totalDutySpoiltProduct,
      adjustmentAmount = adjustmentAmount,
      totalDue = totalDue
    )
  }

  private def calculateUnderDeclarationTotal(underDeclaration: Option[UnderDeclaration]): BigDecimal =
    underDeclaration
      .flatMap(_.underDeclarationProducts)
      .map(_.map(_.dutyDue).sum)
      .getOrElse(ZERO_VALUE)

  private def calculateOverDeclarationTotal(overDeclaration: Option[OverDeclaration]): BigDecimal =
    overDeclaration
      .flatMap(_.overDeclarationProducts)
      .map(_.map(_.dutyDue).sum)
      .getOrElse(ZERO_VALUE)

  private def calculateSpoiltProductTotal(spoiltProduct: Option[SpoiltProduct]): BigDecimal =
    spoiltProduct
      .flatMap(_.spoiltProducts)
      .map(_.map(_.dutyDue).sum)
      .getOrElse(ZERO_VALUE)

  private def calculateAdjustmentAmount(
    over: BigDecimal,
    under: BigDecimal,
    spoilt: BigDecimal
  ): BigDecimal = {
    under - over - spoilt
  }
}