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
import play.api.data.Form

import javax.inject.Inject

class SpoiltVolumeByPeriodFormProvider @Inject() extends Mappings {

  private val maxVolume = "999999999999.9"

  def apply(): Form[BigDecimal] =
    Form(
      "value" -> volume(
        "returns.spoiltVolumeByPeriod.error.required",
        "returns.spoiltVolumeByPeriod.error.nonNumeric",
        "returns.spoiltVolumeByPeriod.error.invalidDecimalPlaces.wholeOnly",
        "returns.spoiltVolumeByPeriod.error.invalidDecimalPlaces.maxOne")
          .verifying(inRange(BigDecimal(1), BigDecimal(maxVolume), "returns.spoiltVolumeByPeriod.error.outOfRange"))
    )
}
