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
import models.{Mode, NormalMode}
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.ObligationDetails
import models.returns.{DutyRate, ReturnsUserAnswers}
import models.returns.adjustments.AdjustmentList
import pages.returns.adjustments.{AdjustmentListPage, DeclareAdjustmentPage}
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import utils.ReturnsDateUtils
import viewmodels.returns.submit.adjustments.{AdjustmentCheckYourAnswersViewModel, RemoveAdjustmentViewModel}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class AdjustmentCheckYourAnswersService @Inject()(
                                                   obligationService: ObligationService,
                                                   dutyRateService: DutyRateService,
                                                   returnsDateUtils: ReturnsDateUtils,
                                                   sessionRepository: ReturnsUserAnswersService
                                                 )(using ExecutionContext) {

  def buildViewModel(
                      declareAdjustment: Option[Boolean],
                      adjustmentList: Option[AdjustmentList],
                      periodKey: PeriodKey,
                      vpdId: VpdId,
                      mode: Mode = NormalMode
                    )(using HeaderCarrier, Messages): Future[AdjustmentCheckYourAnswersViewModel] = {
    obligationService.getObligationsDirectly(vpdId).map { obligationDetails =>
      val dutyRatesMap = getDutyRatesForAdjustments(adjustmentList, obligationDetails)
      AdjustmentCheckYourAnswersViewModel(
        declareAdjustment,
        adjustmentList,
        obligationDetails,
        periodKey,
        dutyRatesMap,
        returnsDateUtils,
        mode
      )
    }
  }

  def buildRemoveViewModel(
                            adjustmentList: Option[AdjustmentList],
                            adjustmentPeriod: PeriodKey,
                            vpdId: VpdId
                          )(using HeaderCarrier, Messages): Future[Option[RemoveAdjustmentViewModel]] =
    obligationService.getObligationsDirectly(vpdId).map { obligationDetails =>
      for {
        entry      <- adjustmentList.map(_.adjustments).getOrElse(Seq.empty).find(_.period == adjustmentPeriod)
        obligation <- obligationDetails.find(_.periodKey == adjustmentPeriod.toString)
      } yield RemoveAdjustmentViewModel(
        entry,
        obligationDetails,
        dutyRateService.getDutyRateForDate(obligation.iCFromDate),
        returnsDateUtils
      )
    }

  def handleRemoval(
                     userAnswers: ReturnsUserAnswers,
                     adjustmentPeriod: PeriodKey,
                     confirmed: Boolean
                   )(using HeaderCarrier): Future[ReturnsUserAnswers] =
    if (confirmed) {
      for {
        updatedAnswers <- Future.fromTry(removeEntry(userAnswers, adjustmentPeriod))
        _              <- sessionRepository.set(updatedAnswers)
      } yield updatedAnswers
    } else {
      Future.successful(userAnswers)
    }

  private def removeEntry(userAnswers: ReturnsUserAnswers, adjustmentPeriod: PeriodKey): Try[ReturnsUserAnswers] = {
    val updatedAdjustments = userAnswers.get(AdjustmentListPage).map(_.adjustments).getOrElse(Seq.empty).filterNot(_.period == adjustmentPeriod)

    if (updatedAdjustments.isEmpty) userAnswers.set(DeclareAdjustmentPage, false)
    else userAnswers.set(AdjustmentListPage, AdjustmentList(updatedAdjustments))
  }

  private def getDutyRatesForAdjustments(
                                          adjustmentList: Option[AdjustmentList],
                                          obligationDetails: Seq[ObligationDetails]
                                        ): Map[PeriodKey, DutyRate] = {

    val uniquePeriods = adjustmentList
      .map(_.adjustments.map(_.period).distinct)
      .getOrElse(Seq.empty)

    dutyRateService.getDutyRatesForPeriods(uniquePeriods, obligationDetails)
  }

  private def calculateDutyTotalByType(
                                        adjustments: Seq[models.returns.adjustments.Adjustment],
                                        adjustmentType: models.returns.adjustments.AdjustmentType,
                                        dutyRatesMap: Map[PeriodKey, DutyRate]
                                      ): BigDecimal = {
    adjustments
      .filter(_.adjustmentType == adjustmentType)
      .flatMap(adjustment =>
        dutyRatesMap
          .get(adjustment.period)
          .map(_.calculateDuty(adjustment.volumeInMl))
      )
      .sum
  }
}