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

package forms.returns.adjustments

import forms.mappings.Mappings
import models.identifiers.{PeriodKey, VpdId}
import models.returns.adjustments.{AdjustmentType, AdjustmentVolumeWithTypeFormData}
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}
import play.api.data.validation.{Constraint, Invalid, Valid}
import services.returns.{DutyRateService, VolumePrecisionService}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AdjustmentVolumeWithTypeFormProvider @Inject()(
  dutyRateService: DutyRateService,
  volumePrecisionService: VolumePrecisionService
) extends Mappings {

  def apply(periodKey: PeriodKey, vpdId: VpdId)
           (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Form[AdjustmentVolumeWithTypeFormData]] = {

    dutyRateService.getDutyRate(vpdId, periodKey).map { dutyRate =>
      val dutyRateInPencePerMl = (dutyRate * 100).toInt
      val maxVolumeResult = volumePrecisionService.calculateMaxVolume(dutyRateInPencePerMl)

      Form(
        mapping(
          "adjustmentType" -> enumerable[AdjustmentType](
            "returns.adjustmentVolumeWithType.error.required"
          ),
          "underDeclaredVolume" -> optional(volume(
            "returns.adjustmentVolumeWithType.underDeclared.error.required",
            "returns.adjustmentVolumeWithType.underDeclared.error.nonNumeric",
            "returns.adjustmentVolumeWithType.underDeclared.error.invalidDecimalPlaces.wholeOnly",
            "returns.adjustmentVolumeWithType.underDeclared.error.invalidDecimalPlaces.maxOne"
          ).verifying(greaterThanZero("returns.adjustmentVolumeWithType.underDeclared.error.mustBeGreaterThanZero"))),
          "overDeclaredVolume" -> optional(volume(
            "returns.adjustmentVolumeWithType.overDeclared.error.required",
            "returns.adjustmentVolumeWithType.overDeclared.error.nonNumeric",
            "returns.adjustmentVolumeWithType.overDeclared.error.invalidDecimalPlaces.wholeOnly",
            "returns.adjustmentVolumeWithType.overDeclared.error.invalidDecimalPlaces.maxOne"
          ).verifying(greaterThanZero("returns.adjustmentVolumeWithType.overDeclared.error.mustBeGreaterThanZero")))
        )((adjustmentType, underDeclaredVolume, overDeclaredVolume) =>
          AdjustmentVolumeWithTypeFormData(adjustmentType, underDeclaredVolume, overDeclaredVolume)
        )(data =>
          Some((data.adjustmentType, data.underDeclaredVolume, data.overDeclaredVolume))
        )
          .verifying("returns.adjustmentVolumeWithType.error.required", data => {
            data.adjustmentType match {
              case AdjustmentType.UnderDeclared => data.underDeclaredVolume.isDefined
              case AdjustmentType.OverDeclared => data.overDeclaredVolume.isDefined
            }
          })
          .verifying("returns.adjustmentVolumeWithType.error.bothProvided", data => {
            !(data.underDeclaredVolume.isDefined && data.overDeclaredVolume.isDefined)
          })
          .verifying(
            Constraint[AdjustmentVolumeWithTypeFormData] { data =>
              val volume = data.getVolume
              if (volume >= BigDecimal(0) && volume <= maxVolumeResult.maxVolumeInMl) {
                Valid
              } else {
                Invalid("returns.adjustmentVolumeWithType.error.exceedsMaxDuty", maxVolumeResult.formattedForDisplay)
              }
            }
          )
      )
    }
  }
}
