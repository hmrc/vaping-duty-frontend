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

package config

import com.google.inject.{Inject, Singleton}
import models.returns.{DutyRate, DutyRateValidationError, DateRange}
import play.api.Configuration

import java.time.LocalDate

@Singleton
class DutyRateConfig @Inject()(configuration: Configuration) {
  
  private val DUTY_RATES_KEY = "duty-rates"
  
  val rates: Seq[DutyRate] = {
    val configList = configuration.get[Seq[Configuration]](DUTY_RATES_KEY)
    val parsedRates = configList.map { rateConfig =>
      DutyRate(
        period = DateRange(
          start = LocalDate.parse(rateConfig.get[String]("start-date")),
          end = LocalDate.parse(rateConfig.get[String]("end-date"))
        ),
        ratePencePerMl = rateConfig.get[Int]("rate-pence-per-ml")
      )
    }.sortBy(_.period.start)
    
    DutyRateValidator.validate(parsedRates) match {
      case Right(validRates) => validRates
      case Left(errors)      => 
        val errorMessage = errors.map(_.message).mkString("\n  - ", "\n  - ", "")
        // scalafix:off DisableSyntax.throw
        throw new IllegalArgumentException(s"Invalid duty rate configuration:$errorMessage")
    }
  }
}