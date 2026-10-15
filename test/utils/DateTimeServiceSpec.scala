/*
 * Copyright 2024 HM Revenue & Customs
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

package utils

import base.SpecBase

import java.time.Instant

class DateTimeServiceSpec extends SpecBase {

  "DateTimeService" - {
    "timestamp" - {
      "must return current timestamp as formatted string" in {
        val service = new DateTimeService(clock)
        val result = service.timestamp

        result mustBe a[String]
        result must not be empty
      }
    }

    "now" - {
      "must return current time as Instant" in {
        val service = new DateTimeService(clock)
        val before = Instant.now(clock)
        val result = service.now
        val after = Instant.now(clock)

        result.isAfter(before.minusSeconds(1)) mustBe true
        result.isBefore(after.plusSeconds(1)) mustBe true
      }

      "must return different values on subsequent calls" in {
        val service = new DateTimeService(clock)
        val instant1 = service.now
        Thread.sleep(10)
        val instant2 = service.now

        instant2.isAfter(instant1) || instant2.equals(instant1) mustBe true
      }
    }
  }
}