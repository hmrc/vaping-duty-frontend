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
import models.returns.DutyRate
import play.api.Configuration

import java.time.LocalDate

@Singleton
class DutyRateConfig @Inject()(configuration: Configuration) {
  
  private val DUTY_RATES_KEY = "duty-rates"
  
  val rates: Seq[DutyRate] = {
    val configList = configuration.get[Seq[Configuration]](DUTY_RATES_KEY)
    val parsedRates = configList.map { rateConfig =>
      DutyRate(
        startDate = LocalDate.parse(rateConfig.get[String]("start-date")),
        endDate = LocalDate.parse(rateConfig.get[String]("end-date")),
        ratePencePerMl = rateConfig.get[Int]("rate-pence-per-ml")
      )
    }.sortBy(_.startDate)
    
    validateRates(parsedRates)
    parsedRates
  }
  
  private def validateRates(rates: Seq[DutyRate]): Unit = {
    require(rates.nonEmpty, "At least one duty rate must be configured")
    
    // Validate each rate
    rates.foreach { rate =>
      require(rate.ratePencePerMl > 0, s"Duty rate must be positive: ${rate.ratePencePerMl}")
      require(
        rate.endDate.isAfter(rate.startDate) || rate.endDate.isEqual(rate.startDate),
        s"End date must be after or equal to start date: ${rate.startDate} to ${rate.endDate}"
      )
    }
    
    // Check for gaps and overlaps
    rates.sliding(2).foreach {
      case Seq(current, next) =>
        val dayAfterCurrentEnd = current.endDate.plusDays(1)
        require(
          dayAfterCurrentEnd.isEqual(next.startDate),
          s"Gap or overlap detected between periods: ${current.endDate} and ${next.startDate}"
        )
      case _ => RuntimeException("Duty rates misconfigured")
    }
    
    // Ensure current date is covered
    val today = LocalDate.now()
    require(
      rates.exists(_.isValidFor(today)),
      s"Current date $today is not covered by any configured duty rate period"
    )
  }
}