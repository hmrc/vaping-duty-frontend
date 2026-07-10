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

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class AdjustmentReasonFormProviderSpec extends StringFieldBehaviours {

  val form = new AdjustmentReasonFormProvider()()

  ".adjustmentReason" - {

    val fieldName = "adjustmentReason"
    val requiredKey = "returns.adjustmentReason.error.required"
    val lengthKey = "returns.adjustmentReason.error.length"
    val maxLength = 250

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "bind a string that is below the maximum length" in {
      val validData = "a" * 50
      val result = form.bind(Map(fieldName -> validData)).apply(fieldName)
      result.value.value mustBe validData
      result.errors mustBe empty
    }

    "bind a string that is exactly at the maximum length" in {
      val validData = "a" * maxLength
      val result = form.bind(Map(fieldName -> validData)).apply(fieldName)
      result.value.value mustBe validData
      result.errors mustBe empty
    }

    "not bind a string one character over the maximum length" in {
      val invalidData = "a" * (maxLength + 1)
      val result = form.bind(Map(fieldName -> invalidData)).apply(fieldName)
      result.errors must contain only FormError(fieldName, lengthKey, Seq(maxLength))
    }

  }
}