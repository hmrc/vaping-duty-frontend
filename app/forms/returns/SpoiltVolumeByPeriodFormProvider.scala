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

package forms.returns

import forms.mappings.Mappings
import models.identifiers.{PeriodKey, VpdId}
import play.api.data.Form
import services.returns.{DutyRateService, VolumePrecisionService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SpoiltVolumeByPeriodFormProvider @Inject()(
  dutyRateService: DutyRateService,
  volumePrecisionService: VolumePrecisionService
) extends Mappings {

  def apply(periodKey: PeriodKey, vpdId: VpdId)
           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Form[BigDecimal]] = {

    dutyRateService.getDutyRate(vpdId, periodKey).map { dutyRate =>
      val maxVolumeResult = volumePrecisionService.calculateMaxVolume(dutyRate)

      Form(
        "value" -> volume(
          "returns.spoiltVolumeByPeriod.error.required",
          "returns.spoiltVolumeByPeriod.error.nonNumeric",
          "returns.spoiltVolumeByPeriod.error.invalidDecimalPlaces.wholeOnly",
          "returns.spoiltVolumeByPeriod.error.invalidDecimalPlaces.maxOne")
            .verifying(greaterThanZero("returns.spoiltVolumeByPeriod.error.mustBeGreaterThanZero"))
            .verifying(inRange(
              BigDecimal(0),
              maxVolumeResult.maxVolumeInMl,
              "returns.spoiltVolumeByPeriod.error.exceedsMaxDuty",
              maxVolumeResult.formattedForDisplay
            ))
      )
    }
  }
}
