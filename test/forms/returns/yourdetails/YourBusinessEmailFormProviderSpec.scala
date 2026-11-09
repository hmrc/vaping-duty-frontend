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

package forms.returns.yourdetails

import forms.FormSpec
import forms.mappings.Constraints
import play.api.data.FormError

class YourBusinessEmailFormProviderSpec extends FormSpec with Constraints {

  private val form = new YourBusinessEmailFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "returns.declaration.emailAddress.error.required"
    val lengthKey = "returns.declaration.emailAddress.error.length"
    val formatKey = "returns.declaration.emailAddress.error.format"
    val maxLength = 132

    "must bind valid email addresses" in {
      Seq("test@example.com", "user.name@example.co.uk", "user+tag@example.com").foreach { email =>
        val result = form.bind(Map(fieldName -> email))
        result.errors mustBe empty
        result.value.value mustEqual email
      }
    }

    "must fail to bind when value is omitted" in {
      form.bind(Map(fieldName -> "")).errors must contain(FormError(fieldName, requiredKey))
    }

    "must fail to bind when value exceeds maximum length" in {
      val tooLongEmail = "a" * (maxLength + 1) + "@example.com"
      form.bind(Map(fieldName -> tooLongEmail)).errors must contain(FormError(fieldName, lengthKey, Seq(maxLength)))
    }

    "must fail to bind invalid email format" in {
      form.bind(Map(fieldName -> "invalid-email")).errors must contain(FormError(fieldName, formatKey, Seq(validEmailRegex)))
    }
  }
}
