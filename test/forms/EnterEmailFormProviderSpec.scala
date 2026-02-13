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

package forms

import forms.behaviours.StringFieldBehaviours
import forms.contactPreference.EnterEmailFormProvider
import forms.mappings.Constraints
import play.api.data.FormError

import scala.collection.immutable.ArraySeq
import scala.util.Random

class EnterEmailFormProviderSpec extends StringFieldBehaviours with Constraints {

  val requiredKey = "contactPreference.enterEmail.error.required"
  val lengthKey = "contactPreference.enterEmail.error.length"
  val formatKey = "contactPreference.enterEmail.error.format"
  val maxLength = 254

  val validEmails =
    Seq(
      "valid@email.com",
      s"${Random.alphanumeric.take(240).mkString}@${Random.alphanumeric.take(7).mkString}.co.uk",
      s"${Random.alphanumeric.take(120).mkString}@${Random.alphanumeric.take(126).mkString}.gov.uk",
      s"${Random.alphanumeric.take(1).mkString}@${Random.alphanumeric.take(246).mkString}.co.uk",
      s"${Random.alphanumeric.take(246).mkString}@${Random.alphanumeric.take(1).mkString}.co.uk",
      s"${Random.alphanumeric.take(1).mkString}@${Random.alphanumeric.take(1).mkString}.com"
    )

  val form = new EnterEmailFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like emailFieldWithValidData(
      form,
      fieldName,
      validEmails
    )

    behave like emailFieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      errors = Seq(FormError(fieldName, lengthKey, Seq(maxLength)), FormError(fieldName, formatKey, Seq(emailRegex)))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like emailFieldWithInvalidData(
      form,
      fieldName,
      FormError(fieldName, Seq(formatKey), ArraySeq(emailRegex)),
      stringsWithMaxLength(maxLength)
    )
  }
}
