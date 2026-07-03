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

package forms.returns

import forms.behaviours.FieldBehaviours
import play.api.data.FormError

class EnterDutyAmountFormProviderSpec extends FieldBehaviours {

  val form = new EnterDutyAmountFormProvider()()

  ".value" - {

    val fieldName = "value"

    "must bind valid values >= 1000ml with no decimal places" in {
      Seq("1000", "1000000", "999999999999").foreach { input =>
        val result = form.bind(Map(fieldName -> input))
        result.errors mustBe empty
      }
    }

    "must bind valid values < 1000ml with 0 or 1 decimal place" in {
      Seq("1", "10.1", "999.9", "500").foreach { input =>
        val result = form.bind(Map(fieldName -> input))
        result.errors mustBe empty
      }
    }

    "must not bind empty values" in {
      val result = form.bind(Map(fieldName -> "")).apply(fieldName)
      result.errors mustEqual Seq(FormError(fieldName, "returns.enterDutyAmount.error.required"))
    }

    "must not bind non-numeric values" in {
      Seq("abc", "1.2.3", "£10.12").foreach { input =>
        val result = form.bind(Map(fieldName -> input)).apply(fieldName)
        result.errors mustEqual Seq(FormError(fieldName, "returns.enterDutyAmount.error.nonNumeric"))
      }
    }

    "must bind values >= 1000ml with trailing zeros" in {
      Seq("1000.0", "2000.0").foreach { input =>
        val result = form.bind(Map(fieldName -> input))
        result.errors mustBe empty
      }
    }

    "must not bind values >= 1000ml with non-zero decimal places" in {
      Seq("1000.1", "1000.12", "999999999999.9").foreach { input =>
        val result = form.bind(Map(fieldName -> input)).apply(fieldName)
        result.errors mustEqual Seq(FormError(fieldName, "returns.enterDutyAmount.error.invalidDecimalPlaces.wholeOnly"))
      }
    }

    "must not bind values < 1000ml with more than 1 decimal place" in {
      Seq("999.99", "10.12", "1.123").foreach { input =>
        val result = form.bind(Map(fieldName -> input)).apply(fieldName)
        result.errors mustEqual Seq(FormError(fieldName, "returns.enterDutyAmount.error.invalidDecimalPlaces.maxOne"))
      }
    }

    "must not bind values below the minimum of 1ml" in {
      Seq("0", "0.1").foreach { input =>
        val result = form.bind(Map(fieldName -> input)).apply(fieldName)
        result.errors mustEqual Seq(FormError(fieldName, "returns.enterDutyAmount.error.outOfRange", Seq(BigDecimal(1), BigDecimal("999999999999.9"))))
      }
    }
  }
}
