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

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class YourFullNameFormProviderSpec extends StringFieldBehaviours {

  val form = new YourFullNameFormProvider()()

  ".fullName" - {

    val fieldName = "fullName"
    val requiredKey = "returns.declaration.fullName.error.required"
    val lengthKey = "returns.declaration.fullName.error.length"
    val invalidCharactersKey = "returns.declaration.fullName.error.invalidCharacters"
    val maxLength = 120

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "bind a string that is exactly at the maximum length" in {
      val validData = "a" * maxLength
      val result = form.bind(Map(fieldName -> validData)).apply(fieldName)
      result.value.value mustBe validData
      result.errors mustBe empty
    }

    "not bind a string one character over the maximum length" in {
      val result = form.bind(Map(fieldName -> "a" * (maxLength + 1))).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Seq(maxLength)))
    }

    "bind names with allowed characters" in {
      Seq("John Smith", "Mary-Jane", "Dr. Smith", "O'Connor", "A-B.C'D 123").foreach { name =>
        val result = form.bind(Map(fieldName -> name))
        result.errors mustBe empty
        result.value.value mustBe name
      }
    }

    "not bind names with invalid characters" in {
      Seq("Smith, John", "John@Smith", "Name_Test", "Name/Test").foreach { name =>
        val result = form.bind(Map(fieldName -> name))
        result.errors must contain(FormError(fieldName, invalidCharactersKey, Seq("""^[a-zA-Z0-9\-\.\s']+$""")))
      }
    }

    "trim leading and trailing spaces" in {
      form.bind(Map(fieldName -> "  John Smith  ")).value.value mustBe "John Smith"
    }
  }
}
