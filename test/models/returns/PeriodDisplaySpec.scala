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

import base.UnitSpec

class PeriodDisplaySpec extends UnitSpec {

  "PeriodDisplay" - {

    "formatted" - {

      "must return month and year separated by space" in {
        val periodDisplay = PeriodDisplay("January", "2024")
        
        periodDisplay.formatted mustBe "January 2024"
      }

      "must handle different months correctly" in {
        val testCases = Seq(
          ("January", "2024", "January 2024"),
          ("February", "2025", "February 2025"),
          ("December", "2023", "December 2023"),
          ("September", "2026", "September 2026")
        )

        testCases.foreach { case (month, year, expected) =>
          val periodDisplay = PeriodDisplay(month, year)
          periodDisplay.formatted mustBe expected
        }
      }

      "must preserve exact month and year values" in {
        val periodDisplay = PeriodDisplay("September", "2026")
        
        periodDisplay.month mustBe "September"
        periodDisplay.year mustBe "2026"
      }
    }
  }
}