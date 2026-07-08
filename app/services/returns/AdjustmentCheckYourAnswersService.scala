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

import com.google.inject.{Inject, Singleton}
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.ObligationDetails
import models.returns.adjustments.AdjustmentList
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.returns.submit.adjustments.AdjustmentCheckYourAnswersViewModel

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdjustmentCheckYourAnswersService @Inject()(
  obligationService: ObligationService,
  dutyRateService: DutyRateService
)(using ExecutionContext) {

  def buildViewModel(
    declareAdjustment: Option[Boolean],
    adjustmentList: Option[AdjustmentList],
    periodKey: PeriodKey,
    vpdId: VpdId
  )(using HeaderCarrier, Messages): Future[AdjustmentCheckYourAnswersViewModel] = {
    obligationService.getObligationsDirectly(vpdId).map { obligationDetails =>
      val dutyRatesMap = getDutyRatesForAdjustments(adjustmentList, obligationDetails)
      AdjustmentCheckYourAnswersViewModel(
        declareAdjustment,
        adjustmentList,
        obligationDetails,
        periodKey,
        dutyRatesMap
      )
    }
  }

  private def getDutyRatesForAdjustments(
    adjustmentList: Option[AdjustmentList],
    obligationDetails: Seq[ObligationDetails]
  ): Map[String, BigDecimal] = {

    val uniquePeriods = adjustmentList
      .map(_.adjustments.map(_.period).distinct)
      .getOrElse(Seq.empty)

    uniquePeriods.map { period =>
      val obligation = obligationDetails.find(_.periodKey == period.toString)
      val dutyRate = obligation.map { obl =>
        val rateInPencePerMl = dutyRateService.getRateForDate(obl.iCFromDate)
        BigDecimal(rateInPencePerMl) / 100
      }.getOrElse(
        // scalafix:off DisableSyntax.throw
        throw new RuntimeException(s"No obligation found for period ${period.toString}")
      )
      period.toString -> dutyRate
    }.toMap
  }
}