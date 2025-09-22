/*
 * Copyright 2025 HM Revenue & Customs
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

package forms.behaviours

import play.api.data.{Form, FormError}

trait CurrencyFieldBehaviours extends FieldBehaviours {

  def currencyField(form: Form[_],
                    fieldName: String,
                    nonNumericError: FormError,
                    invalidNumericError: FormError): Unit = {

    "must not bind non-numeric numbers" in {

      forAll(nonNumerics -> "nonNumeric") {
        nonNumeric =>
          val result = form.bind(Map(fieldName -> nonNumeric)).apply(fieldName)
          result.errors mustEqual Seq(nonNumericError)
      }
    }

    s"must not bind invalid decimals (over 2dp)" in {
      val result = form.bind(Map(fieldName -> "1.234")).apply(fieldName)
      result.errors mustEqual Seq(invalidNumericError)
    }
  }

  def currencyFieldWithMinimum(form: Form[_],
                               fieldName: String,
                               minimum: BigDecimal,
                               expectedError: FormError): Unit = {

    "must not bind when the value is less than the minimum" in {

      val result = form.bind(Map(fieldName -> (minimum - 0.01).toString)).apply(fieldName)
      result.errors mustEqual Seq(expectedError)
    }
  }

  def currencyFieldWithMaximum(form: Form[_],
                               fieldName: String,
                               maximum: BigDecimal,
                               expectedError: FormError): Unit = {

    "must not bind when the value is greater than the maximum" in {

      val result = form.bind(Map(fieldName -> (maximum + 0.01).toString)).apply(fieldName)
      result.errors mustEqual Seq(expectedError)
    }
  }
}
