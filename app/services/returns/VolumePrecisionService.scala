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
import models.returns.MaxVolumeResult

import java.text.NumberFormat
import java.util.Locale

@Singleton
class VolumePrecisionService @Inject() {

  private val API_MAX_DUTY_DUE = BigDecimal("99999999999.99")

  def calculateMaxVolume(dutyRateInPencePerMl: Int): MaxVolumeResult = {
    val rateInPounds = BigDecimal(dutyRateInPencePerMl) / 100
    val maxVolume = (API_MAX_DUTY_DUE / rateInPounds)
      .setScale(0, BigDecimal.RoundingMode.DOWN)

    MaxVolumeResult(
      maxVolumeInMl = maxVolume,
      formattedForDisplay = formatWithCommas(maxVolume)
    )
  }

  private def formatWithCommas(value: BigDecimal): String = {
    val formatter = NumberFormat.getInstance(Locale.UK)
    formatter.setMaximumFractionDigits(1)
    formatter.setMinimumFractionDigits(0)
    s"${formatter.format(value)} ml"
  }
}