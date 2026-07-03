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

import forms.FormSpec
import models.returns.DutySuspenseVolumes
import play.api.data.FormError

class EnterDutySuspenseFormProviderSpec extends FormSpec {

  val form = new EnterDutySuspenseFormProvider()()

  "EnterDutySuspenseFormProvider" - {

    "volumeReceived field" - {

      val fieldName = "volumeReceived"
      val requiredKey = "returns.enterDutySuspense.volumeReceived.error.required"
      val nonNumericKey = "returns.enterDutySuspense.volumeReceived.error.nonNumeric"
      val invalidDecimalKey = "returns.enterDutySuspense.volumeReceived.error.invalidDecimalPlaces"

      "must bind valid values >= 1000ml with no decimal places" in {
        Seq("1000", "1000000", "999999999999").foreach { input =>
          val result = form.bind(Map(fieldName -> input, "volumeMoved" -> "1000"))
          result.errors mustBe empty
        }
      }

      "must bind valid values < 1000ml with 0 or 1 decimal place" in {
        Seq("0", "10.1", "999.9", "500").foreach { input =>
          val result = form.bind(Map(fieldName -> input, "volumeMoved" -> "1000"))
          result.errors mustBe empty
        }
      }

      "must bind zero" in {
        val result = form.bind(Map(fieldName -> "0", "volumeMoved" -> "1000"))
        result.value.value mustEqual DutySuspenseVolumes(BigDecimal("0"), BigDecimal(1000))
      }

      "must fail to bind when value is omitted" in {
        val result = form.bind(Map(fieldName -> "", "volumeMoved" -> "2000"))
        result.errors must contain(FormError(fieldName, requiredKey))
      }

      "must fail to bind non-numeric values" in {
        Seq("abc", "1.2.3", "£10.12").foreach { input =>
          val result = form.bind(Map(fieldName -> input, "volumeMoved" -> "2000"))
          result.errors must contain(FormError(fieldName, nonNumericKey))
        }
      }

      "must bind values >= 1000ml with trailing zeros" in {
        Seq("1000.0", "2000.0").foreach { input =>
          val result = form.bind(Map(fieldName -> input, "volumeMoved" -> "1000"))
          result.errors.filter(_.key == fieldName) mustBe empty
        }
      }

      "must fail to bind values >= 1000ml with non-zero decimal places" in {
        Seq("1000.1", "1000.12", "999999999999.9").foreach { input =>
          val result = form.bind(Map(fieldName -> input, "volumeMoved" -> "2000"))
          result.errors must contain(FormError(fieldName, "returns.enterDutySuspense.volumeReceived.error.invalidDecimalPlaces.wholeOnly"))
        }
      }

      "must fail to bind values < 1000ml with more than 1 decimal place" in {
        Seq("999.99", "10.12", "1.123").foreach { input =>
          val result = form.bind(Map(fieldName -> input, "volumeMoved" -> "2000"))
          result.errors must contain(FormError(fieldName, "returns.enterDutySuspense.volumeReceived.error.invalidDecimalPlaces.maxOne"))
        }
      }

      "must fail to bind negative values" in {
        val result = form.bind(Map(fieldName -> "-1.00", "volumeMoved" -> "2000"))
        result.errors must contain(FormError(fieldName, nonNumericKey))
      }

      "must bind maximum value" in {
        val result = form.bind(Map(fieldName -> "999999999999", "volumeMoved" -> "2000"))
        result.value.value mustEqual DutySuspenseVolumes(BigDecimal("999999999999"), BigDecimal(2000))
      }
    }

    "volumeMoved field" - {

      val fieldName = "volumeMoved"
      val requiredKey = "returns.enterDutySuspense.volumeMoved.error.required"
      val nonNumericKey = "returns.enterDutySuspense.volumeMoved.error.nonNumeric"
      val invalidDecimalKey = "returns.enterDutySuspense.volumeMoved.error.invalidDecimalPlaces"
      val outOfRangeKey = "returns.enterDutySuspense.volumeMoved.error.outOfRange"

      "must bind valid values >= 1000ml with no decimal places" in {
        Seq("1000", "1000000", "999999999999").foreach { input =>
          val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> input))
          result.errors mustBe empty
        }
      }

      "must bind valid values < 1000ml with 0 or 1 decimal place" in {
        Seq("0", "10.1", "999.9", "500").foreach { input =>
          val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> input))
          result.errors mustBe empty
        }
      }

      "must bind zero" in {
        val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> "0"))
        result.value.value mustEqual DutySuspenseVolumes(BigDecimal(1000), BigDecimal("0"))
      }

      "must fail to bind when value is omitted" in {
        val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> ""))
        result.errors must contain(FormError(fieldName, requiredKey))
      }

      "must fail to bind non-numeric values" in {
        Seq("abc", "1.2.3", "£10.12").foreach { input =>
          val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> input))
          result.errors must contain(FormError(fieldName, nonNumericKey))
        }
      }

      "must bind values >= 1000ml with trailing zeros" in {
        Seq("1000.0", "2000.0").foreach { input =>
          val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> input))
          result.errors.filter(_.key == fieldName) mustBe empty
        }
      }

      "must fail to bind values >= 1000ml with non-zero decimal places" in {
        Seq("1000.1", "1000.12", "999999999999.9").foreach { input =>
          val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> input))
          result.errors must contain(FormError(fieldName, "returns.enterDutySuspense.volumeMoved.error.invalidDecimalPlaces.wholeOnly"))
        }
      }

      "must fail to bind values < 1000ml with more than 1 decimal place" in {
        Seq("999.99", "10.12", "1.123").foreach { input =>
          val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> input))
          result.errors must contain(FormError(fieldName, "returns.enterDutySuspense.volumeMoved.error.invalidDecimalPlaces.maxOne"))
        }
      }

      "must fail to bind negative values" in {
        val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> "-1.00"))
        result.errors must contain(FormError(fieldName, nonNumericKey))
      }

      "must bind maximum value" in {
        val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> "999999999999"))
        result.value.value mustEqual DutySuspenseVolumes(BigDecimal(1000), BigDecimal("999999999999"))
      }
    }

    "both fields" - {

      "must fail when both fields are empty" in {
        val result = form.bind(Map("volumeReceived" -> "", "volumeMoved" -> ""))
        result.errors.size mustBe 2
        result.errors must contain(FormError("volumeReceived", "returns.enterDutySuspense.volumeReceived.error.required"))
        result.errors must contain(FormError("volumeMoved", "returns.enterDutySuspense.volumeMoved.error.required"))
      }

      "must bind when both fields have valid values" in {
        val result = form.bind(Map("volumeReceived" -> "1500", "volumeMoved" -> "2500"))
        result.value.value mustEqual DutySuspenseVolumes(BigDecimal("1500"), BigDecimal("2500"))
      }
    }
  }
}