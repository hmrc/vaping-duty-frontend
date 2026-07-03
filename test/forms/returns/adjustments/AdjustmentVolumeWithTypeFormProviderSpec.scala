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

package forms.returns.adjustments

import forms.behaviours.FieldBehaviours
import models.returns.adjustments.AdjustmentType
import play.api.data.FormError

class AdjustmentVolumeWithTypeFormProviderSpec extends FieldBehaviours {

  val form = new AdjustmentVolumeWithTypeFormProvider()()

  ".adjustmentType" - {

    val fieldName = "adjustmentType"
    val requiredKey = "returns.adjustmentVolumeWithType.error.required"

    "must bind valid adjustment types" in {
      val result = form.bind(Map(
        fieldName -> "underDeclared",
        "underDeclaredVolume" -> "100.55"
      ))
      result.errors mustBe empty
      result.value.value.adjustmentType mustBe AdjustmentType.UnderDeclared
    }

    "must not bind invalid adjustment types" in {
      val result = form.bind(Map(
        fieldName -> "InvalidType",
        "underDeclaredVolume" -> "1000.5"
      ))
      result.errors must contain(FormError(fieldName, "error.invalid"))
    }

    "must fail when adjustment type is missing" in {
      val result = form.bind(Map("underDeclaredVolume" -> "100.55"))
      result.errors must contain(FormError(fieldName, requiredKey))
    }
  }

  ".underDeclaredVolume" - {

    val fieldName = "underDeclaredVolume"

    "must bind valid volumes >= 1000ml with up to 1 decimal place" in {
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        fieldName -> "1000.5"
      ))
      result.errors mustBe empty
      result.value.value.underDeclaredVolume mustBe Some(BigDecimal("1000.5"))
    }

    "must bind valid volumes < 1000ml with exactly 2 decimal places" in {
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        fieldName -> "100.55"
      ))
      result.errors mustBe empty
      result.value.value.underDeclaredVolume mustBe Some(BigDecimal("100.55"))
    }

    "must fail when under declared volume is missing for UnderDeclared type" in {
      val result = form.bind(Map("adjustmentType" -> "underDeclared"))
      result.errors must contain(FormError("", "returns.adjustmentVolumeWithType.error.required"))
    }

    "must fail when under declared volume is not numeric" in {
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        fieldName -> "abc"
      ))
      result.errors must contain(FormError(fieldName, "returns.adjustmentVolumeWithType.underDeclared.error.nonNumeric"))
    }

    "must fail when volume >= 1000ml has more than 1 decimal place" in {
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        fieldName -> "1000.55"
      ))
      result.errors must contain(FormError(fieldName, "returns.adjustmentVolumeWithType.underDeclared.error.invalidDecimalPlaces"))
    }

    "must fail when volume < 1000ml does not have exactly 2 decimal places" in {
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        fieldName -> "100.5"
      ))
      result.errors must contain(FormError(fieldName, "returns.adjustmentVolumeWithType.underDeclared.error.invalidDecimalPlaces"))
    }
  }

  ".overDeclaredVolume" - {

    val fieldName = "overDeclaredVolume"

    "must bind valid volumes >= 1000ml with up to 1 decimal place" in {
      val result = form.bind(Map(
        "adjustmentType" -> "overDeclared",
        fieldName -> "2000.7"
      ))
      result.errors mustBe empty
      result.value.value.overDeclaredVolume mustBe Some(BigDecimal("2000.7"))
    }

    "must bind valid volumes < 1000ml with exactly 2 decimal places" in {
      val result = form.bind(Map(
        "adjustmentType" -> "overDeclared",
        fieldName -> "200.77"
      ))
      result.errors mustBe empty
      result.value.value.overDeclaredVolume mustBe Some(BigDecimal("200.77"))
    }

    "must fail when over declared volume is missing for OverDeclared type" in {
      val result = form.bind(Map("adjustmentType" -> "overDeclared"))
      result.errors must contain(FormError("", "returns.adjustmentVolumeWithType.error.required"))
    }

    "must fail when over declared volume is not numeric" in {
      val result = form.bind(Map(
        "adjustmentType" -> "overDeclared",
        fieldName -> "xyz"
      ))
      result.errors must contain(FormError(fieldName, "returns.adjustmentVolumeWithType.overDeclared.error.nonNumeric"))
    }

    "must fail when volume >= 1000ml has more than 1 decimal place" in {
      val result = form.bind(Map(
        "adjustmentType" -> "overDeclared",
        fieldName -> "2000.77"
      ))
      result.errors must contain(FormError(fieldName, "returns.adjustmentVolumeWithType.overDeclared.error.invalidDecimalPlaces"))
    }

    "must fail when volume < 1000ml does not have exactly 2 decimal places" in {
      val result = form.bind(Map(
        "adjustmentType" -> "overDeclared",
        fieldName -> "200.7"
      ))
      result.errors must contain(FormError(fieldName, "returns.adjustmentVolumeWithType.overDeclared.error.invalidDecimalPlaces"))
    }
  }

  "form validation" - {

    "must fail when both volumes are provided" in {
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        "underDeclaredVolume" -> "100.55",
        "overDeclaredVolume" -> "200.77"
      ))
      result.errors must contain(FormError("", "returns.adjustmentVolumeWithType.error.bothProvided"))
    }

    "must accept minimum valid volume (< 1000ml with 2 decimal places)" in {
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        "underDeclaredVolume" -> "1.00"
      ))
      result.errors mustBe empty
    }

    "must accept maximum valid volume (>= 1000ml with 1 decimal place)" in {
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        "underDeclaredVolume" -> "999999999999.9"
      ))
      result.errors mustBe empty
    }

    "must fail when volume is below minimum" in {
      val exception = the[IllegalArgumentException] thrownBy {
        form.bind(Map(
          "adjustmentType" -> "underDeclared",
          "underDeclaredVolume" -> "0.50"
        ))
      }
      exception.getMessage mustBe "returns.adjustmentVolumeWithType.error.outOfRange"
    }

    "must fail when volume exceeds maximum" in {
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        "underDeclaredVolume" -> "9999999999999"
      ))
      result.errors must contain(FormError("underDeclaredVolume", "returns.adjustmentVolumeWithType.underDeclared.error.nonNumeric"))
    }
  }

  ".getVolume" - {

    "must return the correct volume for UnderDeclared" in {
      val formData = AdjustmentVolumeWithTypeFormData(
        adjustmentType = AdjustmentType.UnderDeclared,
        underDeclaredVolume = Some(BigDecimal("100.55")),
        overDeclaredVolume = None
      )
      formData.getVolume mustBe BigDecimal("100.55")
    }

    "must return the correct volume for OverDeclared" in {
      val formData = AdjustmentVolumeWithTypeFormData(
        adjustmentType = AdjustmentType.OverDeclared,
        underDeclaredVolume = None,
        overDeclaredVolume = Some(BigDecimal("200.77"))
      )
      formData.getVolume mustBe BigDecimal("200.77")
    }
  }
}
