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
import forms.mappings.Constraints
import models.returns.DeclarationDetails
import play.api.data.FormError

class DeclarationFormProviderSpec extends FormSpec with Constraints {


  private val formProvider = DeclarationFormProvider()
  private val form = formProvider()

  private val FULL_NAME_FIELD = formProvider.FULL_NAME
  private val CAPACITY_FIELD = formProvider.CAPACITY
  private val EMAIL_FIELD = formProvider.EMAIL

  "DeclarationFormProvider" - {

    "fullName field" - {

      val requiredKey = "returns.declaration.fullName.error.required"
      val lengthKey = "returns.declaration.fullName.error.length"
      val maxLength = 120

      "must bind valid data" in {
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "John Smith",
          CAPACITY_FIELD -> "Director",
          EMAIL_FIELD -> "test@example.com"
        ))
        result.value.value mustEqual DeclarationDetails("John Smith", "Director", "test@example.com")
      }

      "must fail to bind when value is omitted" in {
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "",
          CAPACITY_FIELD -> "Director",
          EMAIL_FIELD -> "test@example.com"
        ))
        result.errors must contain(FormError(FULL_NAME_FIELD, requiredKey))
      }

      "must fail to bind when value exceeds maximum length" in {
        val tooLongName = "a" * (maxLength + 1)
        val result = form.bind(Map(
          FULL_NAME_FIELD -> tooLongName,
          CAPACITY_FIELD -> "Director",
          EMAIL_FIELD -> "test@example.com"
        ))
        result.errors must contain(FormError(FULL_NAME_FIELD, lengthKey, Seq(maxLength)))
      }

      "must bind maximum length value" in {
        val maxLengthName = "a" * maxLength
        val result = form.bind(Map(
          FULL_NAME_FIELD -> maxLengthName,
          CAPACITY_FIELD -> "Director",
          EMAIL_FIELD -> "test@example.com"
        ))
        result.value.value mustEqual DeclarationDetails(maxLengthName, "Director", "test@example.com")
      }

      "must bind valid names with allowed characters" in {
        val validNames = Seq(
          "John Smith",
          "Mary-Jane",
          "Dr. Smith",
          "O'Connor",
          "Jean-Paul O'Brien",
          "User123",
          "A-B.C'D 123"
        )

        validNames.foreach { name =>
          val result = form.bind(Map(
            FULL_NAME_FIELD -> name,
            CAPACITY_FIELD -> "Director",
            EMAIL_FIELD -> "test@example.com"
          ))
          result.errors mustBe empty
          result.value.value mustEqual DeclarationDetails(name, "Director", "test@example.com")
        }
      }

      "must fail to bind names with invalid characters" in {
        val invalidCharactersKey = "returns.declaration.fullName.error.invalidCharacters"
        val invalidNames = Seq(
          ("Smith, John", "comma"),
          ("John@Smith", "@ symbol"),
          ("Mary!Jane", "exclamation mark"),
          ("User#123", "hash symbol"),
          ("Test&Name", "ampersand"),
          ("Name*Test", "asterisk"),
          ("Name(Test)", "parentheses"),
          ("Name[Test]", "brackets"),
          ("Name{Test}", "braces"),
          ("Name/Test", "forward slash"),
          ("Name\\Test", "backslash"),
          ("Name|Test", "pipe"),
          ("Name<Test>", "angle brackets"),
          ("Name=Test", "equals sign"),
          ("Name+Test", "plus sign"),
          ("Name_Test", "underscore"),
          ("Name~Test", "tilde"),
          ("Name`Test", "backtick"),
          ("Name;Test", "semicolon"),
          ("Name:Test", "colon"),
          ("Name\"Test", "quotation mark"),
          ("Name?Test", "question mark"),
          ("Name%Test", "percent sign"),
          ("Name$Test", "dollar sign"),
          ("Name^Test", "caret")
        )

        invalidNames.foreach { case (name, description) =>
          val result = form.bind(Map(
            FULL_NAME_FIELD -> name,
            CAPACITY_FIELD -> "Director",
            EMAIL_FIELD -> "test@example.com"
          ))
          result.errors must contain(FormError(FULL_NAME_FIELD, invalidCharactersKey, Seq("""^[a-zA-Z0-9\-\.\s']+$""")))
        }
      }

      "must trim leading and trailing spaces" in {
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "  John Smith  ",
          CAPACITY_FIELD -> "Director",
          EMAIL_FIELD -> "test@example.com"
        ))
        result.value.value mustEqual DeclarationDetails("John Smith", "Director", "test@example.com")
      }
    }

    "capacityInWhichSigned field" - {

      val requiredKey = "returns.declaration.capacity.error.required"
      val lengthKey = "returns.declaration.capacity.error.length"
      val maxLength = 100

      "must bind valid data" in {
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "John Smith",
          CAPACITY_FIELD -> "Director",
          EMAIL_FIELD -> "test@example.com"
        ))
        result.value.value mustEqual DeclarationDetails("John Smith", "Director", "test@example.com")
      }

      "must fail to bind when value is omitted" in {
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "John Smith",
          CAPACITY_FIELD -> "",
          EMAIL_FIELD -> "test@example.com"
        ))
        result.errors must contain(FormError(CAPACITY_FIELD, requiredKey))
      }

      "must fail to bind when value exceeds maximum length" in {
        val tooLongCapacity = "a" * (maxLength + 1)
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "John Smith",
          CAPACITY_FIELD -> tooLongCapacity,
          EMAIL_FIELD -> "test@example.com"
        ))
        result.errors must contain(FormError(CAPACITY_FIELD, lengthKey, Seq(maxLength)))
      }

      "must bind maximum length value" in {
        val maxLengthCapacity = "a" * maxLength
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "John Smith",
          CAPACITY_FIELD -> maxLengthCapacity,
          EMAIL_FIELD -> "test@example.com"
        ))
        result.value.value mustEqual DeclarationDetails("John Smith", maxLengthCapacity, "test@example.com")
      }
    }

    "signeesEmailAddress field" - {

      val requiredKey = "returns.declaration.emailAddress.error.required"
      val lengthKey = "returns.declaration.emailAddress.error.length"
      val formatKey = "returns.declaration.emailAddress.error.format"
      val maxLength = 132

      "must bind valid data" in {
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "John Smith",
          CAPACITY_FIELD -> "Director",
          EMAIL_FIELD -> "test@example.com"
        ))
        result.value.value mustEqual DeclarationDetails("John Smith", "Director", "test@example.com")
      }

      "must fail to bind when value is omitted" in {
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "John Smith",
          CAPACITY_FIELD -> "Director",
          EMAIL_FIELD -> ""
        ))
        result.errors must contain(FormError(EMAIL_FIELD, requiredKey))
      }

      "must fail to bind when value exceeds maximum length" in {
        val tooLongEmail = "a" * (maxLength + 1) + "@example.com"
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "John Smith",
          CAPACITY_FIELD -> "Director",
          EMAIL_FIELD -> tooLongEmail
        ))
        result.errors must contain(FormError(EMAIL_FIELD, lengthKey, Seq(maxLength)))
      }

      "must fail to bind invalid email format" in {
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "John Smith",
          CAPACITY_FIELD -> "Director",
          EMAIL_FIELD -> "invalid-email"
        ))
        result.errors must contain(FormError(EMAIL_FIELD, formatKey, Seq(validEmailRegex)))
      }

      "must bind valid email addresses with various formats" in {
        val validEmails = Seq(
          "test@example.com",
          "user.name@example.co.uk",
          "user+tag@example.com"
        )

        validEmails.foreach { email =>
          val result = form.bind(Map(
            FULL_NAME_FIELD -> "John Smith",
            CAPACITY_FIELD -> "Director",
            EMAIL_FIELD -> email
          ))
          result.errors mustBe empty
          result.value.value mustEqual DeclarationDetails("John Smith", "Director", email)
        }
      }

      "must not bind invalid email addresses" in {
        val invalidEmails = Seq(
          "invalid",
          "@example.com",
          "user@"
        )

        invalidEmails.foreach { email =>
          val result = form.bind(Map(
            FULL_NAME_FIELD -> "John Smith",
            CAPACITY_FIELD -> "Director",
            EMAIL_FIELD -> email
          ))
          result.errors must contain(FormError(EMAIL_FIELD, formatKey, Seq(validEmailRegex)))
        }
      }
    }

    "all fields" - {

      "must fail when all fields are empty" in {
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "",
          CAPACITY_FIELD -> "",
          EMAIL_FIELD -> ""
        ))
        result.errors.size mustBe 3
        result.errors must contain(FormError(FULL_NAME_FIELD, "returns.declaration.fullName.error.required"))
        result.errors must contain(FormError(CAPACITY_FIELD, "returns.declaration.capacity.error.required"))
        result.errors must contain(FormError(EMAIL_FIELD, "returns.declaration.emailAddress.error.required"))
      }

      "must bind when all fields have valid values" in {
        val result = form.bind(Map(
          FULL_NAME_FIELD -> "Jane Doe",
          CAPACITY_FIELD -> "Chief Financial Officer",
          EMAIL_FIELD -> "jane.doe@example.com"
        ))
        result.value.value mustEqual DeclarationDetails("Jane Doe", "Chief Financial Officer", "jane.doe@example.com")
      }
    }
  }
}
