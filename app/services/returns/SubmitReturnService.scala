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
import pages.returns.{DeclarationPage, DeclareDutyPage, EnterDutyAmountPage}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitReturnService @Inject()(
  submitReturnConnector: SubmitReturnConnector,
  dutyRateService: DutyRateService,
  obligationService: ObligationService,
  config: FrontendAppConfig
)(using ExecutionContext) {

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
    } yield result
  }

  private def buildSubmission(ua: ReturnsUserAnswers, obligation: ObligationDetails): ReturnCreateRequest = {

    val zeroValue = BigDecimal("0")
    val dutyDeclared = ua.get(DeclareDutyPage).getOrElse(false)
    val liquidInMl = ua.get(EnterDutyAmountPage).fold(zeroValue)(value => BigDecimal(value))

    val periodKey = PeriodKey(ua.periodKey)

    val dutyRateInPencePerMl: Int = dutyRateService.getRateForDate(obligation.iCFromDate)

    val liquidInLitres = ConvertToLitres(liquidInMl).toLitres

    val dutyDue = (liquidInMl * (BigDecimal(dutyRateInPencePerMl) / 100)).setScale(2, BigDecimal.RoundingMode.DOWN)

    val dutyRateInPoundsPer10Ml = (BigDecimal(dutyRateInPencePerMl) / 100) * 10
    val vapingProductsProduced = if (dutyDeclared) {
      VapingProductsProduced(nilReturn = Seq(), regularReturn = Seq(
        RegularReturn(
          taxType = config.taxType,
          dutyRate = dutyRateInPoundsPer10Ml,
          amountProducedLiquid = liquidInLitres,
          dutyDue = dutyDue
        )))
    } else {
      VapingProductsProduced(nilReturn = Seq(NilReturn(vapingProductsProduced = "0")), regularReturn = Seq())
    }

    val totalDutyDueVapingProducts = if (dutyDeclared) dutyDue else zeroValue

    def calculateAdjustmentValue(over: BigDecimal, under: BigDecimal, spoilt: BigDecimal) = {
      over + under + spoilt
    }

    val adjustments = calculateAdjustmentValue(zeroValue, zeroValue, zeroValue)

    val totalDutyDue = TotalDutyDue(
      totalDutyDueVapingProducts = totalDutyDueVapingProducts,
      totalDutyOverDeclaration = zeroValue,
      totalDutyUnderDeclaration = zeroValue,
      totalDutySpoiltProduct = zeroValue,
      adjustmentAmount = adjustments,
      totalDutyDue = totalDutyDueVapingProducts + adjustments
    )

    val declaration = ua.get(DeclarationPage).getOrElse(
      // scalafix:off DisableSyntax.throw
      throw new IllegalStateException("Declaration details are required for submission")
    )

    ReturnCreateRequest(periodKey.toString, vapingProductsProduced, totalDutyDue, declaration)
  }
}