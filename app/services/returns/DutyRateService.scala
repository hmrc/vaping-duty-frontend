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
import config.DutyRateConfig
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.ObligationDetails
import models.returns.DutyRate
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DutyRateService @Inject()(dutyRateConfig: DutyRateConfig, obligationService: ObligationService) {

  def getDutyRateForDate(date: LocalDate) =
    DutyRate(
      dutyRateConfig.rates
        .find(_.isValidFor(date))
        .map(_.ratePencePer10Ml)
        .get // Safe because validation ensures there's always a rate
    )

  def getDutyRateInPoundsPerMl(vpdId: VpdId, periodKey: PeriodKey)
                              (using ec: ExecutionContext, hc: HeaderCarrier): Future[BigDecimal] =

    getDutyRateForPeriod(vpdId, periodKey).flatMap {
      case Some(dutyRate) => Future.successful(dutyRate.dutyRateInPoundsPerMl)
      case None => Future.failed(RuntimeException("No duty rate found"))
    }

  private def getDutyRateForPeriod(vpdId: VpdId, periodKey: PeriodKey)
                                  (using ec: ExecutionContext, hc: HeaderCarrier): Future[Option[DutyRate]] =

    obligationService.getObligationByPeriodKey(vpdId, periodKey).map { obligationOpt =>
      obligationOpt.map { obligation =>
        getDutyRateForDate(obligation.iCFromDate)
      }
    }

  def getDutyRatesForPeriodKeys(obligations: Seq[ObligationDetails]): Map[PeriodKey, DutyRate] = {
    obligations.map(o => PeriodKey(o.periodKey) -> getDutyRateForDate(o.iCFromDate)).toMap
  }
}
