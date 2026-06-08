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

class DeclarationFormProviderSpec extends StringFieldBehaviours {

  private val form = new DeclarationFormProvider()()

  private val FULL_NAME_FIELD = "fullName"
  private val CAPACITY_FIELD = "capacityInWhichSigned"
  private val EMAIL_FIELD = "signeesEmailAddress"

  "DeclarationFormProvider" - {

    "fullName field" - {

      val requiredKey = "returns.declaration.fullName.error.required"
      val lengthKey = "returns.declaration.fullName.error.length"
      val maxLength = 255

      behave like fieldThatBindsValidData(
        form,
        FULL_NAME_FIELD,
        "John Smith"
      )

      behave like fieldWithMaxLength(
        form,
        FULL_NAME_FIELD,
        maxLength = maxLength,
        lengthError = FormError(FULL_NAME_FIELD, lengthKey, Seq(maxLength))
      )

      behave like mandatoryField(
        form,
        FULL_NAME_FIELD,
        requiredError = FormError(FULL_NAME_FIELD, requiredKey)
      )
    }

    "capacityInWhichSigned field" - {

      val requiredKey = "returns.declaration.capacity.error.required"
      val lengthKey = "returns.declaration.capacity.error.length"
      val maxLength = 255

      behave like fieldThatBindsValidData(
        form,
        CAPACITY_FIELD,
        "Director"
      )

      behave like fieldWithMaxLength(
        form,
        CAPACITY_FIELD,
        maxLength = maxLength,
        lengthError = FormError(CAPACITY_FIELD, lengthKey, Seq(maxLength))
      )

      behave like mandatoryField(
        form,
        CAPACITY_FIELD,
        requiredError = FormError(CAPACITY_FIELD, requiredKey)
      )
    }

    "signeesEmailAddress field" - {

      val requiredKey = "returns.declaration.emailAddress.error.required"
      val lengthKey = "returns.declaration.emailAddress.error.length"
      val formatKey = "returns.declaration.emailAddress.error.format"
      val maxLength = 254

      behave like fieldThatBindsValidData(
        form,
        EMAIL_FIELD,
        "test@example.com"
      )

      behave like fieldWithMaxLength(
        form,
        EMAIL_FIELD,
        maxLength = maxLength,
        lengthError = FormError(EMAIL_FIELD, lengthKey, Seq(maxLength))
      )

      behave like mandatoryField(
        form,
        EMAIL_FIELD,
        requiredError = FormError(EMAIL_FIELD, requiredKey)
      )

      "must not bind invalid email addresses" in {
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "John Smith",
          CAPACITY_FIELD -> "Director",
          EMAIL_FIELD -> "invalid-email"
        ))
        result.errors must contain(FormError(EMAIL_FIELD, formatKey))
      }

      "must bind valid email addresses" in {
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "John Smith",
          CAPACITY_FIELD -> "Director",
          EMAIL_FIELD -> "test@example.com"
        ))
        result.errors mustBe empty
      }
    }
  }
}