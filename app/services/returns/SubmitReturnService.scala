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
import connectors.returns.SubmitReturnConnector
import models.identifiers.PeriodKey
import models.obligations.ObligationDetails
import models.requests.returns.ReturnsDataRequest
import models.returns.*
import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import models.returns.view.{OtherOptions, OverDeclaration, SpoiltProduct, SpoiltProductItem, UnderDeclaration}
import pages.returns.{DeclarationPage, DeclareDutyPage, DeclareDutySuspensePage, EnterDutyAmountPage, EnterDutySuspensePage, SpoiltVolumeByPeriodPage}
import play.api.libs.json.{JsObject, Json}
import services.contactPreference.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitReturnService @Inject()(
  submitReturnConnector: SubmitReturnConnector,
  dutyRateService: DutyRateService,
  obligationService: ObligationService,
  totalDutyDueCalculationService: TotalDutyDueCalculationService,
  auditService: AuditService,
  config: FrontendAppConfig
)(using ExecutionContext) {

  private val ZERO_VALUE = BigDecimal("0")
  private val FLAG_NOT_FILLED = "0"
  private val FLAG_FILLED = "1"

  def submit(ua: ReturnsUserAnswers)(implicit request: ReturnsDataRequest[?]): Future[ReturnSubmittedResponse] = {

    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(session = request.session, request = request.request)

    for {
      obligationOpt <- obligationService.getObligationByPeriodKey(request.enrolmentVpdId, request.periodKey)
      obligation <- obligationOpt match {
        case Some(obl) => Future.successful(obl)
        case None => Future.failed(new IllegalStateException(s"No obligation found for period key: ${ua.periodKey}"))
      }
      submission = buildSubmission(ua, obligation)
      result <- submitReturnConnector.submitReturn(submission, request.enrolmentVpdId)
    } yield {
      val detail = Json.parse("{}").as[JsObject]
      auditService.auditReturnSubmitted(detail)
      result
    }
  }

  private def buildSubmission(ua: ReturnsUserAnswers, obligation: ObligationDetails): ReturnCreateRequest = {

    val dutyDeclared = ua.get(DeclareDutyPage).getOrElse(false)
    val liquidInMl = ua.get(EnterDutyAmountPage).getOrElse(ZERO_VALUE)

    val periodKey = PeriodKey(ua.periodKey)

    val dutyRateInPencePerMl: Int = dutyRateService.getRateForDate(obligation.iCFromDate)

    val liquidInLitres = ConvertToLitres(liquidInMl).toLitres

    val dutyDue = (liquidInMl * (BigDecimal(dutyRateInPencePerMl) / 100)).setScale(2, BigDecimal.RoundingMode.DOWN)

    val dutyRateInPoundsPer10Ml = (BigDecimal(dutyRateInPencePerMl) / 100) * 10
    
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

    val totalDutyDueVapingProducts = if (dutyDeclared) dutyDue else ZERO_VALUE

    val underDeclaration = buildUnderDeclaration()
    val overDeclaration = buildOverDeclaration()
    val spoiltProduct = buildSpoiltProduct(ua, dutyRateInPencePerMl)
    val otherOptions = buildOtherOptions(ua)

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

  private def buildUnderDeclaration(): Option[UnderDeclaration] =
    Some(UnderDeclaration(
      underDeclFilled = FLAG_NOT_FILLED,
      reasonForUnderDecl = None,
      underDeclarationProducts = None
    ))

  private def buildOverDeclaration(): Option[OverDeclaration] =
    Some(OverDeclaration(
      overDeclFilled = FLAG_NOT_FILLED,
      reasonForOverDecl = None,
      overDeclarationProducts = None
    ))

  private def buildSpoiltProduct(ua: ReturnsUserAnswers, dutyRateInPencePerMl: Int): Option[SpoiltProduct] = {
    val spoiltVolumes = ua.get(SpoiltVolumeByPeriodPage)
    
    spoiltVolumes match {
      case Some(volumes) if volumes.nonEmpty =>
        val dutyRateInPoundsPer10Ml = (BigDecimal(dutyRateInPencePerMl) / 100) * 10
        
        val spoiltProducts = volumes.map { spoiltVolume =>
          val volumeInMl = BigDecimal(spoiltVolume.volume)
          val volumeInLitres = ConvertToLitres(volumeInMl).toLitres
          val dutyDue = (volumeInMl * (BigDecimal(dutyRateInPencePerMl) / 100)).setScale(2, BigDecimal.RoundingMode.DOWN)
          
          SpoiltProductItem(
            returnPeriodAffected = spoiltVolume.periodKey.toString,
            taxType = config.taxType,
            dutyRate = dutyRateInPoundsPer10Ml,
            amountSpoilt = volumeInLitres,
            dutyDue = dutyDue
          )
        }
        
        Some(SpoiltProduct(
          spoiltProductFilled = FLAG_FILLED,
          spoiltProducts = Some(spoiltProducts)
        ))
      case _ =>
        Some(SpoiltProduct(
          spoiltProductFilled = FLAG_NOT_FILLED,
          spoiltProducts = None
        ))
    }
  }

  private def buildOtherOptions(ua: ReturnsUserAnswers): Option[OtherOptions] = {
    val dutySuspenseDeclared = ua.get(DeclareDutySuspensePage).getOrElse(false)
    val dutySuspenseVolumes = ua.get(EnterDutySuspensePage)
    
    (dutySuspenseDeclared, dutySuspenseVolumes) match {
      case (true, Some(volumes)) =>
        val volumeReceivedInMl = BigDecimal(volumes.volumeReceived)
        val volumeMovedInMl = BigDecimal(volumes.volumeMoved)
        
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
