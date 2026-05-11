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

import connectors.returns.SubmitReturnConnector
import models.requests.returns.ReturnsDataRequest
import models.returns.*
import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import pages.returns.{DeclareDutyPage, EnterDutyAmountPage}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitReturnService @Inject()(submitReturnConnector: SubmitReturnConnector)
                                   (using ExecutionContext) {

  def submit(ua: ReturnsUserAnswers)(implicit request: ReturnsDataRequest[?]): Future[ReturnSubmittedResponse] = {

    given HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(session = request.session, request = request.request)

    submitReturnConnector.submitReturn(buildSubmission(ua), request.enrolmentVpdId)
  }

  private def buildSubmission(ua: ReturnsUserAnswers): ReturnCreateRequest =

    val dutyDeclared = ua.get(DeclareDutyPage).getOrElse(false)
    val totalInMl = ua.get(EnterDutyAmountPage).fold(BigDecimal(0))(value => BigDecimal(value))

    // Temp value
    val zeroValue = BigDecimal(0)

    // Will need to either get or pass the period key here
    val periodKey = "26AF"

    // Will need to enhance this much more
    val totalDue = totalInMl - zeroValue
    val dutyRate = BigDecimal("2.2")
    val dutyDue = (totalDue * dutyRate).setScale(2, BigDecimal.RoundingMode.HALF_UP)

    val vapingProductsProduced = dutyDeclared match {
      case false  =>
        VapingProductsProduced(nilReturn = Seq(NilReturn(vapingProductsProduced = "0")), regularReturn = Seq())
      case _      =>
        VapingProductsProduced(nilReturn = Seq(), regularReturn = Seq(RegularReturn(
          taxType = "301", dutyRate = dutyRate, amountProducedLiquid = totalDue, dutyDue = dutyDue
        )))
    }

    val totalDutyDueVapingProducts  = vapingProductsProduced.regularReturn.head.dutyDue

    def calculateAdjustmentValue(over: BigDecimal, under: BigDecimal, spoilt: BigDecimal) = {
      over + under + spoilt
    }
    val adjustments = calculateAdjustmentValue(zeroValue, zeroValue, zeroValue)

    val totalDutyDue = TotalDutyDue(
      totalDutyDueVapingProducts  = vapingProductsProduced.regularReturn.head.dutyDue,
      totalDutyOverDeclaration    = zeroValue,
      totalDutyUnderDeclaration   = zeroValue,
      totalDutySpoiltProduct      = zeroValue,
      adjustmentAmount            = adjustments,
      totalDutyDue                = totalDutyDueVapingProducts + adjustments
    )

    ReturnCreateRequest(periodKey, vapingProductsProduced, totalDutyDue)
}
