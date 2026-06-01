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
import com.typesafe.config.{ConfigException, ConfigFactory}
import models.returns.{DateRange, DutyRate, DutyRateValidationError}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Configuration

import java.time.LocalDate
import java.time.format.DateTimeParseException
import scala.collection.immutable.Seq

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
      config.rates.head.ratePencePerMl mustBe 22
      config.rates(1).ratePencePerMl mustBe 30
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
    
    "parseRatesFromConfig" - {
      "must be able to parse correct config" in {
        val config = Configuration(ConfigFactory.parseString(
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

        parseRatesFromConfig(config) mustBe Seq(
          DutyRate(DateRange(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31")), 22),
          DutyRate(DateRange(LocalDate.parse("2027-01-01"), LocalDate.parse("9999-12-31")), 30)
        )
      }

      "must be able to parse syntactically correct, but invalid config" in {
        val config = Configuration(ConfigFactory.parseString(
          """
            |duty-rates = [
            |  {
            |    start-date = "2026-01-01"
            |    end-date = "2026-11-30"
            |    rate-pence-per-ml = 22
            |  },
            |  # Gap between Nov and Feb can be read but will fail later validation checks!
            |  {
            |    start-date = "2027-02-01"
            |    end-date = "9999-12-31"
            |    rate-pence-per-ml = 30
            |  }
            |]
            |""".stripMargin))

        parseRatesFromConfig(config) mustBe Seq(
          DutyRate(DateRange(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-11-30")), 22),
          DutyRate(DateRange(LocalDate.parse("2027-02-01"), LocalDate.parse("9999-12-31")), 30)
        )
      }

      "will fail to parse incorrect dates" in {
        val config = Configuration(ConfigFactory.parseString(
          """
            |duty-rates = [
            |  {
            |    start-date = "2026-01-01"
            |    # The 31st Nov does not exist! 
            |    end-date = "2026-11-31"
            |    rate-pence-per-ml = 22
            |  }
            |]
            |""".stripMargin))

        val exception = intercept[DateTimeParseException] {
          parseRatesFromConfig(config)
        }

        exception.getMessage must include("Text '2026-11-31' could not be parsed: Invalid date 'NOVEMBER 31'")
      }

      "will fail to parse non-integer rate-pence-per-ml" in {
        val config = Configuration(ConfigFactory.parseString(
          """
            |duty-rates = [
            |  {
            |    start-date = "2026-01-01"
            |    end-date = "2026-11-30"
            |    rate-pence-per-ml = 22.5
            |  }
            |]
            |""".stripMargin))

        val exception = intercept[NumberFormatException] {
          parseRatesFromConfig(config)
        }

        exception.getMessage must include("For input string: \"22.5\"")
      }

      "will fail to parse non-numeric reate-pence-per-ml" in {
        val config = Configuration(ConfigFactory.parseString(
          """
            |duty-rates = [
            |  {
            |    start-date = "2026-01-01"
            |    end-date = "2026-11-30"
            |    rate-pence-per-ml = "foo-bar"
            |  }
            |]
            |""".stripMargin))

        val exception = intercept[NumberFormatException] {
          parseRatesFromConfig(config)
        }

        exception.getMessage must include("For input string: \"foo-bar\"")
      }

      "will fail to parse if the start date is missing" in {
        val config = Configuration(ConfigFactory.parseString(
          """
            |duty-rates = [
            |  {
            |    # start-date missing!
            |    end-date = "2026-11-31"
            |    rate-pence-per-ml = 22
            |  }
            |]
            |""".stripMargin))

        val exception = intercept[ConfigException.Missing] {
          parseRatesFromConfig(config)
        }

        exception.getMessage must include("No configuration setting found for key 'start-date'")
      }

      "will fail to parse if the end date is missing" in {
        val config = Configuration(ConfigFactory.parseString(
          """
            |duty-rates = [
            |  {
            |    start-date = "2026-01-01"
            |    # end-date missing!
            |    rate-pence-per-ml = 22
            |  }
            |]
            |""".stripMargin))

        val exception = intercept[ConfigException.Missing] {
          parseRatesFromConfig(config)
        }

        exception.getMessage must include("No configuration setting found for key 'end-date'")
      }

      "will fail to parse if the rate-pence-per-ml is missing" in {
        val config = Configuration(ConfigFactory.parseString(
          """
            |duty-rates = [
            |  {
            |    start-date = "2026-01-01"
            |    end-date = "2026-11-30"
            |    # rate-pence-per-ml missing!
            |  }
            |]
            |""".stripMargin))

        val exception = intercept[ConfigException.Missing] {
          parseRatesFromConfig(config)
        }

        exception.getMessage must include("No configuration setting found for key 'rate-pence-per-ml'")
      }
    }

    "throwExceptionIfInvalid" - {

      val validDutyRate1 = DutyRate(DateRange(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31")), 22)
      val validDutyRate2 = DutyRate(DateRange(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-12-31")), 30)
      
      "will return a single duty rates if valid" in {
        throwExceptionIfInvalid(Right(Seq(validDutyRate1))) mustBe
          Seq(validDutyRate1)
      }

      "will return a multiple duty rates if valid" in {
        throwExceptionIfInvalid(Right(Seq(validDutyRate1, validDutyRate2))) mustBe
          Seq(validDutyRate1, validDutyRate2)
      }

      val error1 = mock[DutyRateValidationError]
      when(error1.message) thenReturn "Error 1"
      
      val error2 = mock[DutyRateValidationError]
      when(error2.message) thenReturn "Error 2"

      "will throw an exception if one validation error is detected" in {
        val exception = intercept[IllegalArgumentException] {
          throwExceptionIfInvalid(Left(List(error1)))
        }

        exception.getMessage must include("Invalid duty rate configuration:")
        exception.getMessage must include("Error 1")
      }

      "will throw an exception if multiple validation errors are detected" in {
        val exception = intercept[IllegalArgumentException] {
          throwExceptionIfInvalid(Left(List(error1, error2)))
        }

        exception.getMessage must include("Invalid duty rate configuration:")
        exception.getMessage must include("Error 1")
        exception.getMessage must include("Error 2")
      }
    }
  }
}
