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
import models.returns.{DutyRate, SpoiltVolumeByPeriod}
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import utils.ReturnsDateUtils
import viewmodels.returns.submit.spoilt.{RemoveSpoiltAdjustmentViewModel, SpoiltCheckYourAnswersViewModel}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SpoiltCheckYourAnswersService @Inject()(
                                               obligationService: ObligationService,
                                               dutyRateService: DutyRateService,
                                               returnsDateUtils: ReturnsDateUtils
                                             )(using ExecutionContext) {

  def buildViewModel(
                      declareSpoiltProducts: Option[Boolean],
                      spoiltList: Option[List[SpoiltVolumeByPeriod]],
                      periodKey: PeriodKey,
                      vpdId: VpdId
                    )(using HeaderCarrier, Messages): Future[SpoiltCheckYourAnswersViewModel] = {
    obligationService.getObligationsDirectly(vpdId).map { obligationDetails =>
      val dutyRatesMap = getDutyRatesForSpoiltEntries(spoiltList, obligationDetails)
      SpoiltCheckYourAnswersViewModel(
        declareSpoiltProducts,
        spoiltList,
        obligationDetails,
        periodKey,
        dutyRatesMap,
        returnsDateUtils
      )
    }
  }

  def buildRemoveViewModel(
                            spoiltList: Option[List[SpoiltVolumeByPeriod]],
                            spoiltPeriod: PeriodKey,
                            vpdId: VpdId
                          )(using HeaderCarrier, Messages): Future[Option[RemoveSpoiltAdjustmentViewModel]] =
    obligationService.getObligationsDirectly(vpdId).map { obligationDetails =>
      for {
        entry      <- spoiltList.getOrElse(List.empty).find(_.periodKey == spoiltPeriod)
        obligation <- obligationDetails.find(_.periodKey == spoiltPeriod.toString)
      } yield RemoveSpoiltAdjustmentViewModel(
        entry,
        obligationDetails,
        dutyRateService.getDutyRateForDate(obligation.iCFromDate),
        returnsDateUtils
      )
    }

  def hasAvailablePeriodsToAdd(
                                spoiltList: Option[List[SpoiltVolumeByPeriod]],
                                periodKey: PeriodKey,
                                vpdId: VpdId
                              )(using HeaderCarrier): Future[Boolean] =
    obligationService.getObligationsDirectly(vpdId).map { obligationDetails =>
      SpoiltCheckYourAnswersViewModel.hasAvailablePeriodsToAdd(obligationDetails, periodKey, spoiltList)
    }

  private def getDutyRatesForSpoiltEntries(
                                            spoiltList: Option[List[SpoiltVolumeByPeriod]],
                                            obligationDetails: Seq[ObligationDetails]
                                          ): Map[PeriodKey, DutyRate] = {

    val uniquePeriods = spoiltList.getOrElse(List.empty).map(_.periodKey).distinct

    uniquePeriods.map { period =>
      val obligation = obligationDetails.find(_.periodKey == period.toString)
      val dutyRate = obligation.map { obl =>
        dutyRateService.getDutyRateForDate(obl.iCFromDate)
      }.getOrElse(
        // scalafix:off DisableSyntax.throw
        throw new RuntimeException(s"No obligation found for period ${period.toString}")
      )
      period -> dutyRate
    }.toMap
  }
}
