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

import base.SpecBase
import com.typesafe.config.ConfigFactory
import models.returns.PencePerMillilitre
import play.api.Configuration

import java.time.LocalDate

class DutyRateConfigSpec extends SpecBase {

  "DutyRateConfig" - {

    "must parse valid configuration" in {

      val config = new DutyRateConfig(Configuration(ConfigFactory.parseString(
        """
          |duty-rates = [
          |  {
          |    start-date = "2026-01-01"
          |    end-date = "2026-12-31"
          |    rate-pence-per-ml = 22
          |  },
          |  {
          |    start-date = "2027-01-01"
          |    end-date = "9999-12-31"
          |    rate-pence-per-ml = 30
          |  }
          |]
          |""".stripMargin)))

      config.rates must have size 2
      config.rates.head.period.start mustBe LocalDate.of(2026, 1, 1)
      config.rates.head.period.end mustBe LocalDate.of(2026, 12, 31)
      config.rates.head.ratePencePerMl mustBe PencePerMillilitre(22)
      config.rates(1).ratePencePerMl mustBe PencePerMillilitre(30)
    }


    "must fail when no rates are configured" in {
      val emptyConfig = Configuration(ConfigFactory.parseString(
        """
          |duty-rates = []
          |""".stripMargin))

      val exception = intercept[IllegalArgumentException] {
        new DutyRateConfig(emptyConfig)
      }

      exception.getMessage must include("At least one duty rate must be configured")
    }
  }
}