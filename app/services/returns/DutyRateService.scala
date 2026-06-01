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

import java.time.LocalDate

@Singleton
class DutyRateService @Inject()(dutyRateConfig: DutyRateConfig) {
  
  def getRateForDate(date: LocalDate): Int =
    dutyRateConfig.rates
      .find(_.isValidFor(date))
      .map(_.ratePencePerMl)
      .get  // Safe because validation ensures there's always a rate
}