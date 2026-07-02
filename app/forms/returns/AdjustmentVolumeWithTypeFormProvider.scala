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
import models.returns.AdjustmentType
import play.api.data.Form
import play.api.data.Forms.{mapping, optional}

import javax.inject.Inject

class AdjustmentVolumeWithTypeFormProvider @Inject() extends Mappings {

  private val maxVolume = "999999999999.9"

  def apply(): Form[AdjustmentVolumeWithTypeFormData] =
    Form(
      mapping(
        "adjustmentType" -> enumerable[AdjustmentType](
          "returns.adjustmentVolumeWithType.error.required"
        ),
        "underDeclaredVolume" -> optional(volume(
          "returns.adjustmentVolumeWithType.underDeclared.error.required",
          "returns.adjustmentVolumeWithType.underDeclared.error.nonNumeric",
          "returns.adjustmentVolumeWithType.underDeclared.error.invalidDecimalPlaces"
        )),
        "overDeclaredVolume" -> optional(volume(
          "returns.adjustmentVolumeWithType.overDeclared.error.required",
          "returns.adjustmentVolumeWithType.overDeclared.error.nonNumeric",
          "returns.adjustmentVolumeWithType.overDeclared.error.invalidDecimalPlaces"
        ))
      )(
        (adjustmentType, underDeclaredVolume, overDeclaredVolume) =>
          AdjustmentVolumeWithTypeFormData(adjustmentType, underDeclaredVolume, overDeclaredVolume)
      )(
        data => Some((data.adjustmentType, data.underDeclaredVolume, data.overDeclaredVolume))
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
        .transform[AdjustmentVolumeWithTypeFormData](
          data => {
            val volume = data.adjustmentType match {
              case AdjustmentType.UnderDeclared => data.underDeclaredVolume
              case AdjustmentType.OverDeclared => data.overDeclaredVolume
            }
            volume.foreach { v =>
              if (v < BigDecimal(1) || v > BigDecimal(maxVolume)) {
                throw new IllegalArgumentException("returns.adjustmentVolumeWithType.error.outOfRange")
              }
            }
            data
          },
          identity
        )
    )
}

case class AdjustmentVolumeWithTypeFormData(
  adjustmentType: AdjustmentType,
  underDeclaredVolume: Option[BigDecimal],
  overDeclaredVolume: Option[BigDecimal]
) {
  def getVolume: BigDecimal = adjustmentType match {
    case AdjustmentType.UnderDeclared => underDeclaredVolume.getOrElse(BigDecimal(0))
    case AdjustmentType.OverDeclared => overDeclaredVolume.getOrElse(BigDecimal(0))
  }
}