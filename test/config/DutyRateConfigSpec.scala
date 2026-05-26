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
import play.api.Configuration

import java.time.LocalDate

class DutyRateConfigSpec extends SpecBase {

  private val validConfig = Configuration(ConfigFactory.parseString(
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
      |""".stripMargin))

  "DutyRateConfig" - {

    "must parse valid configuration" in {
      val config = new DutyRateConfig(validConfig)
      
      config.rates must have size 2
      config.rates.head.startDate mustBe LocalDate.of(2026, 1, 1)
      config.rates.head.endDate mustBe LocalDate.of(2026, 12, 31)
      config.rates.head.ratePencePerMl mustBe 22
      config.rates(1).ratePencePerMl mustBe 30
    }

    "must sort rates by start date" in {
      val unsortedConfig = Configuration(ConfigFactory.parseString(
        """
          |duty-rates = [
          |  {
          |    start-date = "2027-01-01"
          |    end-date = "9999-12-31"
          |    rate-pence-per-ml = 30
          |  },
          |  {
          |    start-date = "2026-01-01"
          |    end-date = "2026-12-31"
          |    rate-pence-per-ml = 22
          |  }
          |]
          |""".stripMargin))

      val config = new DutyRateConfig(unsortedConfig)
      
      config.rates.head.startDate mustBe LocalDate.of(2026, 1, 1)
      config.rates(1).startDate mustBe LocalDate.of(2027, 1, 1)
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

    "must fail when end date is before start date" in {
      val invalidDateConfig = Configuration(ConfigFactory.parseString(
        """
          |duty-rates = [
          |  {
          |    start-date = "2026-12-31"
          |    end-date = "2026-01-01"
          |    rate-pence-per-ml = 22
          |  }
          |]
          |""".stripMargin))

      val exception = intercept[IllegalArgumentException] {
        new DutyRateConfig(invalidDateConfig)
      }
      
      exception.getMessage must include("End date must be after or equal to start date")
    }

    "must fail when there is a gap between periods" in {
      val gapConfig = Configuration(ConfigFactory.parseString(
        """
          |duty-rates = [
          |  {
          |    start-date = "2026-01-01"
          |    end-date = "2026-06-30"
          |    rate-pence-per-ml = 22
          |  },
          |  {
          |    start-date = "2026-07-02"
          |    end-date = "2026-12-31"
          |    rate-pence-per-ml = 30
          |  }
          |]
          |""".stripMargin))

      val exception = intercept[IllegalArgumentException] {
        new DutyRateConfig(gapConfig)
      }
      
      exception.getMessage must include("Gap or overlap detected between periods")
    }

    "must fail when there is an overlap between periods" in {
      val overlapConfig = Configuration(ConfigFactory.parseString(
        """
          |duty-rates = [
          |  {
          |    start-date = "2026-01-01"
          |    end-date = "2026-07-01"
          |    rate-pence-per-ml = 22
          |  },
          |  {
          |    start-date = "2026-07-01"
          |    end-date = "2026-12-31"
          |    rate-pence-per-ml = 30
          |  }
          |]
          |""".stripMargin))

      val exception = intercept[IllegalArgumentException] {
        new DutyRateConfig(overlapConfig)
      }
      
      exception.getMessage must include("Gap or overlap detected between periods")
    }

    "must fail when current date is not covered" in {
      val pastOnlyConfig = Configuration(ConfigFactory.parseString(
        """
          |duty-rates = [
          |  {
          |    start-date = "2020-01-01"
          |    end-date = "2020-12-31"
          |    rate-pence-per-ml = 22
          |  }
          |]
          |""".stripMargin))

      val exception = intercept[IllegalArgumentException] {
        new DutyRateConfig(pastOnlyConfig)
      }
      
      exception.getMessage must include("Current date")
      exception.getMessage must include("is not covered by any configured duty rate period")
    }
  }
}