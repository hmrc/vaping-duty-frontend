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

import config.FrontendAppConfig
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.ObligationDetails
import models.returns.*
import models.returns.adjustments.{AdjustmentEntry, AdjustmentType}
import models.returns.submit.ReturnCreateRequest
import models.returns.view.*
import pages.returns.*
import pages.returns.adjustments.{AdjustmentListPage, AdjustmentReasonPage}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class BuildReturnSubmissionService @Inject()(
  totalDutyDueCalculationService: TotalDutyDueCalculationService,
  config: FrontendAppConfig
)(using ExecutionContext) {

  private val ZERO_VALUE = BigDecimal("0")
  private val FLAG_NOT_FILLED = "0"
  private val FLAG_FILLED = "1"

  def buildSubmission(ua: ReturnsUserAnswers,
                      obligation: ObligationDetails,
                      vpdId: VpdId,
                      periodKeyToDutyRateInPencePer10Ml: Map[PeriodKey, Int]): ReturnCreateRequest = {

    val periodKey = PeriodKey(ua.periodKey)

    val vapingProductsProduced = buildVapingProductsProduced(ua, obligation, periodKeyToDutyRateInPencePer10Ml)
    val totalDutyDueVapingProducts = vapingProductsProduced.returns.headOption.map(_.dutyDue).getOrElse(ZERO_VALUE)

    val underDeclaration = buildUnderDeclaration(ua, periodKeyToDutyRateInPencePer10Ml)
    val overDeclaration = buildOverDeclaration(ua, periodKeyToDutyRateInPencePer10Ml)
    val otherOptions = buildOtherOptions(ua)

    val spoiltProduct = buildSpoiltProduct(ua, periodKeyToDutyRateInPencePer10Ml)

    val totalDutyDue = totalDutyDueCalculationService.calculate(
      totalDutyDueVapingProducts,
      underDeclaration,
      overDeclaration,
      spoiltProduct
    )

    val declaration = ua.get(DeclarationPage).getOrElse(
      // scalafix:off DisableSyntax.throw
      throw new IllegalStateException("Declaration details are required for submission")
    )

    ReturnCreateRequest(
      periodKey = periodKey.toString,
      vapingProductsProduced = vapingProductsProduced,
      underDeclaration = underDeclaration,
      overDeclaration = overDeclaration,
      spoiltProduct = spoiltProduct,
      totalDutyDue = totalDutyDue,
      otherOptions = otherOptions,
      declaration = declaration
    )
  }

  private def buildVapingProductsProduced(ua: ReturnsUserAnswers,
                                          obligation: ObligationDetails,
                                          periodKeyToDutyRateInPencePer10Ml: Map[PeriodKey, Int]): VapingProductsProduced = {
    val dutyDeclared = ua.get(DeclareDutyPage).getOrElse(false)
    val liquidInMl = ua.get(EnterDutyAmountPage).getOrElse(ZERO_VALUE)

    val dutyRateInPencePerMl = BigDecimal(periodKeyToDutyRateInPencePer10Ml(PeriodKey(obligation.periodKey))) / 10

    val liquidInLitres = ConvertToLitres(liquidInMl).toLitres

    val dutyDue = (liquidInMl * (dutyRateInPencePerMl / 100)).setScale(2, BigDecimal.RoundingMode.DOWN)

    val dutyRateInPoundsPer10Ml = (dutyRateInPencePerMl / 100) * 10

    val vapingProductsProduced = if (dutyDeclared) {
      VapingProductsProduced(vapingProdManufactured = FLAG_FILLED, returns = Seq(
        RegularReturn(
          taxType = config.taxType,
          dutyRate = dutyRateInPoundsPer10Ml,
          amountProducedLiquid = liquidInLitres,
          dutyDue = dutyDue
        )))
    } else {
      VapingProductsProduced(vapingProdManufactured = FLAG_NOT_FILLED, returns = Seq())
    }
    vapingProductsProduced
  }

  private def buildUnderDeclaration(ua: ReturnsUserAnswers, periodKeyToDutyRateInPencePer10Ml: Map[PeriodKey, Int]): Option[UnderDeclaration] = {
    val underDeclaredEntries = ua.get(AdjustmentListPage)
      .map(_.adjustments.filter(_.adjustmentType == AdjustmentType.UnderDeclared))
      .getOrElse(Seq.empty)

    if (underDeclaredEntries.nonEmpty) {
      val underDeclarationProducts = underDeclaredEntries.map(buildUnderDeclarationProduct(_, periodKeyToDutyRateInPencePer10Ml))
      val reasonForUnderDecl = if (underDeclarationProducts.map(_.dutyDue).sum >= AdjustmentType.dutyThreshold)
        ua.get(AdjustmentReasonPage)
      else
        None

      Some(UnderDeclaration(
        underDeclFilled = FLAG_FILLED,
        reasonForUnderDecl = reasonForUnderDecl,
        underDeclarationProducts = Some(underDeclarationProducts)
      ))
    } else {
      Some(UnderDeclaration(
        underDeclFilled = FLAG_NOT_FILLED,
        reasonForUnderDecl = None,
        underDeclarationProducts = None
      ))
    }
  }

  private def buildUnderDeclarationProduct(entry: AdjustmentEntry, periodKeyToDutyRateInPencePer10Ml: Map[PeriodKey, Int]): UnderDeclarationProduct = {
    val dutyRateInPencePerMl = BigDecimal(periodKeyToDutyRateInPencePer10Ml(entry.period)) / 10
    val dutyRateInPoundsPer10Ml = (dutyRateInPencePerMl * 10) / 100
    val dutyDue = (entry.volumeInMl * (dutyRateInPencePerMl / 100)).setScale(2, BigDecimal.RoundingMode.DOWN)

    UnderDeclarationProduct(
      returnPeriodAffected = entry.period.toString,
      taxType = config.taxType,
      dutyRate = dutyRateInPoundsPer10Ml,
      amountUnderDeclared = ConvertToLitres(entry.volumeInMl).toLitres,
      dutyDue = dutyDue
    )
  }

  private def buildOverDeclaration(ua: ReturnsUserAnswers, periodKeyToDutyRateInPencePer10Ml: Map[PeriodKey, Int]): Option[OverDeclaration] = {
    val overDeclaredEntries = ua.get(AdjustmentListPage)
      .map(_.adjustments.filter(_.adjustmentType == AdjustmentType.OverDeclared))
      .getOrElse(Seq.empty)

    if (overDeclaredEntries.nonEmpty) {
      val overDeclarationProducts = overDeclaredEntries.map(buildOverDeclarationProduct(_, periodKeyToDutyRateInPencePer10Ml))
      val reasonForOverDecl = if (overDeclarationProducts.map(_.dutyDue).sum >= AdjustmentType.dutyThreshold)
        ua.get(AdjustmentReasonPage)
      else
        None

      Some(OverDeclaration(
        overDeclFilled = FLAG_FILLED,
        reasonForOverDecl = reasonForOverDecl,
        overDeclarationProducts = Some(overDeclarationProducts)
      ))
    } else {
      Some(OverDeclaration(
        overDeclFilled = FLAG_NOT_FILLED,
        reasonForOverDecl = None,
        overDeclarationProducts = None
      ))
    }
  }

  private def buildOverDeclarationProduct(entry: AdjustmentEntry, periodKeyToDutyRateInPencePer10Ml: Map[PeriodKey, Int]): OverDeclarationProduct = {
    val dutyRateInPencePerMl = BigDecimal(periodKeyToDutyRateInPencePer10Ml(entry.period)) / 10
    val dutyRateInPoundsPer10Ml = (dutyRateInPencePerMl * 10) / 100
    val dutyDue = (entry.volumeInMl * (dutyRateInPencePerMl / 100)).setScale(2, BigDecimal.RoundingMode.DOWN)

    OverDeclarationProduct(
      returnPeriodAffected = entry.period.toString,
      taxType = config.taxType,
      dutyRate = dutyRateInPoundsPer10Ml,
      amountOverDeclared = ConvertToLitres(entry.volumeInMl).toLitres,
      dutyDue = dutyDue
    )
  }

  private def buildSpoiltProduct(ua: ReturnsUserAnswers, periodKeyToDutyRateInPencePer10Ml: Map[PeriodKey, Int]) = {
    val declareSpoiltProducts = ua.get(DeclareSpoiltProductsPage)
    val spoiltVolumes = ua.get(SpoiltVolumeByPeriodPage)

    (declareSpoiltProducts, spoiltVolumes) match {
      case (Some(true), Some(volumes)) if volumes.nonEmpty =>
        val spoiltProducts = volumes.map(buildSpoiltProductItem(_, periodKeyToDutyRateInPencePer10Ml))

        Some(SpoiltProduct(
          spoiltProductFilled = FLAG_FILLED,
          spoiltProducts = Some(spoiltProducts)
        ))

      case (Some(false), None) =>
        Some(SpoiltProduct(
          spoiltProductFilled = FLAG_NOT_FILLED,
          spoiltProducts = None
        ))

      case _ =>
        None
    }
  }

  private def buildSpoiltProductItem(spoiltVolume: SpoiltVolumeByPeriod, periodKeyToDutyRateInPencePer10Ml: Map[PeriodKey, Int]) = {
    val dutyRateInPencePerMl = BigDecimal(periodKeyToDutyRateInPencePer10Ml(spoiltVolume.periodKey)) / 10
    val dutyRateInPoundsPer10Ml = (dutyRateInPencePerMl * 10) / 100
    val volumeInMl = spoiltVolume.volume
    val volumeInLitres = ConvertToLitres(volumeInMl).toLitres
    val dutyDue = (volumeInMl * (dutyRateInPencePerMl / 100)).setScale(2, BigDecimal.RoundingMode.DOWN)

    SpoiltProductItem(
      returnPeriodAffected = spoiltVolume.periodKey.toString,
      taxType = config.taxType,
      dutyRate = dutyRateInPoundsPer10Ml,
      amountSpoilt = volumeInLitres,
      dutyDue = dutyDue
    )
  }

  private def buildOtherOptions(ua: ReturnsUserAnswers): Option[OtherOptions] = {
    val dutySuspenseDeclared = ua.get(DeclareDutySuspensePage).getOrElse(false)
    val dutySuspenseVolumes = ua.get(EnterDutySuspensePage)
    
    (dutySuspenseDeclared, dutySuspenseVolumes) match {
      case (true, Some(volumes)) =>
        val volumeReceivedInMl = volumes.volumeReceived
        val volumeMovedInMl = volumes.volumeMoved
        
        Some(OtherOptions(
          vapingProductUnderDutySuspense = FLAG_FILLED,
          volumeMovedFromDutySuspense = Some(ConvertToLitres(volumeReceivedInMl).toLitres),
          volumeMovedToDutySuspense = Some(ConvertToLitres(volumeMovedInMl).toLitres)
        ))
      case _ =>
        Some(OtherOptions(
          vapingProductUnderDutySuspense = FLAG_NOT_FILLED,
          volumeMovedFromDutySuspense = None,
          volumeMovedToDutySuspense = None
        ))
    }
  }
}
