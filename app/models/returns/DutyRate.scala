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

package models.returns

case class DutyRate (ratePencePer10Ml: Int) {

  val dutyRateInPoundsPer10Ml       : BigDecimal = BigDecimal(ratePencePer10Ml) / 100
  private val dutyRateInPoundsPerMl : BigDecimal = dutyRateInPoundsPer10Ml / 10

  def calculateDuty(volumeInMl: BigDecimal): BigDecimal =
    (volumeInMl * dutyRateInPoundsPerMl).setScale(2, BigDecimal.RoundingMode.DOWN)

  def volumeForDutyInMl(dutyInPounds: BigDecimal): BigDecimal =
    (dutyInPounds / dutyRateInPoundsPerMl).setScale(0, BigDecimal.RoundingMode.DOWN)

}
