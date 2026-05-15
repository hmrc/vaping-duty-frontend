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
      val outOfRangeKey = "returns.enterDutySuspense.volumeReceived.error.outOfRange"

      "must bind valid data" in {
        val result = form.bind(Map(fieldName -> "1000", "volumeMoved" -> "2000"))
        result.value.value mustEqual DutySuspenseVolumes(1000, 2000)
      }

      "must fail to bind when value is omitted" in {
        val result = form.bind(Map(fieldName -> "", "volumeMoved" -> "2000"))
        result.errors must contain(FormError(fieldName, requiredKey))
      }

      "must fail to bind non-numeric values" in {
        val result = form.bind(Map(fieldName -> "abc", "volumeMoved" -> "2000"))
        result.errors must contain(FormError(fieldName, nonNumericKey))
      }

      "must fail to bind values less than 1" in {
        val result = form.bind(Map(fieldName -> "0", "volumeMoved" -> "2000"))
        result.errors must contain(FormError(fieldName, outOfRangeKey))
      }

      "must fail to bind negative values" in {
        val result = form.bind(Map(fieldName -> "-1", "volumeMoved" -> "2000"))
        result.errors must contain(FormError(fieldName, outOfRangeKey))
      }

      "must bind maximum integer value" in {
        val result = form.bind(Map(fieldName -> Int.MaxValue.toString, "volumeMoved" -> "2000"))
        result.value.value mustEqual DutySuspenseVolumes(Int.MaxValue, 2000)
      }
    }

    "volumeMoved field" - {

      val fieldName = "volumeMoved"
      val requiredKey = "returns.enterDutySuspense.volumeMoved.error.required"
      val nonNumericKey = "returns.enterDutySuspense.volumeMoved.error.nonNumeric"
      val outOfRangeKey = "returns.enterDutySuspense.volumeMoved.error.outOfRange"

      "must bind valid data" in {
        val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> "2000"))
        result.value.value mustEqual DutySuspenseVolumes(1000, 2000)
      }

      "must fail to bind when value is omitted" in {
        val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> ""))
        result.errors must contain(FormError(fieldName, requiredKey))
      }

      "must fail to bind non-numeric values" in {
        val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> "xyz"))
        result.errors must contain(FormError(fieldName, nonNumericKey))
      }

      "must fail to bind values less than 1" in {
        val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> "0"))
        result.errors must contain(FormError(fieldName, outOfRangeKey))
      }

      "must fail to bind negative values" in {
        val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> "-1"))
        result.errors must contain(FormError(fieldName, outOfRangeKey))
      }

      "must bind maximum integer value" in {
        val result = form.bind(Map("volumeReceived" -> "1000", fieldName -> Int.MaxValue.toString))
        result.value.value mustEqual DutySuspenseVolumes(1000, Int.MaxValue)
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
        result.value.value mustEqual DutySuspenseVolumes(1500, 2500)
      }
    }
  }
}