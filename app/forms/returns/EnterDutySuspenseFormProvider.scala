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
import models.returns.DutySuspenseVolumes
import play.api.data.Form
import play.api.data.Forms.mapping
import services.returns.{DutyRateService, VolumePrecisionService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnterDutySuspenseFormProvider @Inject()(
  dutyRateService: DutyRateService,
  volumePrecisionService: VolumePrecisionService
) extends Mappings {

  private val VOLUME_RECEIVED_FIELD = "volumeReceived"
  private val VOLUME_MOVED_FIELD = "volumeMoved"

  def apply(periodKey: PeriodKey, vpdId: VpdId)
           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Form[DutySuspenseVolumes]] = {

    dutyRateService.getDutyRate(vpdId, periodKey).map { dutyRate =>
      val dutyRateInPencePerMl = (dutyRate * 100).toInt
      val maxVolumeResult = volumePrecisionService.calculateMaxVolume(dutyRateInPencePerMl)

      Form(
        mapping(
          VOLUME_RECEIVED_FIELD -> volume(
            "returns.enterDutySuspense.volumeReceived.error.required",
            "returns.enterDutySuspense.volumeReceived.error.nonNumeric",
            "returns.enterDutySuspense.volumeReceived.error.invalidDecimalPlaces.wholeOnly",
            "returns.enterDutySuspense.volumeReceived.error.invalidDecimalPlaces.maxOne")
              .verifying(inRange(
                BigDecimal(0),
                maxVolumeResult.maxVolumeInMl,
                "returns.enterDutySuspense.volumeReceived.error.exceedsMaxDuty",
                maxVolumeResult.formattedForDisplay
              )),

          VOLUME_MOVED_FIELD -> volume(
            "returns.enterDutySuspense.volumeMoved.error.required",
            "returns.enterDutySuspense.volumeMoved.error.nonNumeric",
            "returns.enterDutySuspense.volumeMoved.error.invalidDecimalPlaces.wholeOnly",
            "returns.enterDutySuspense.volumeMoved.error.invalidDecimalPlaces.maxOne")
              .verifying(inRange(
                BigDecimal(0),
                maxVolumeResult.maxVolumeInMl,
                "returns.enterDutySuspense.volumeMoved.error.exceedsMaxDuty",
                maxVolumeResult.formattedForDisplay
              ))
        )((received, moved) => DutySuspenseVolumes(received, moved))(o => Some((o.volumeReceived, o.volumeMoved)))
      )
    }
  }
}
