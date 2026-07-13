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
        "underDeclaredVolume" -> "100.5"
      ))
      result.errors mustBe empty
      result.value.value.adjustmentType mustBe AdjustmentType.UnderDeclared
    }

    "must not bind invalid adjustment types" in {
      val result = form.bind(Map(
        fieldName -> "InvalidType",
        "underDeclaredVolume" -> "1000"
      ))
      result.errors must contain(FormError(fieldName, "error.invalid"))
    }

    "must fail when adjustment type is missing" in {
      val result = form.bind(Map("underDeclaredVolume" -> "100.5"))
      result.errors must contain(FormError(fieldName, requiredKey))
    }
  }

  ".underDeclaredVolume" - {

    val fieldName = "underDeclaredVolume"

    "must bind valid volumes >= 1000ml with no decimal places" in {
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        fieldName -> "1000"
      ))
      result.errors mustBe empty
      result.value.value.underDeclaredVolume mustBe Some(BigDecimal("1000"))
    }

    "must bind valid volumes < 1000ml with 0 or 1 decimal place" in {
      Seq("100", "100.5", "999.9").foreach { input =>
        val result = form.bind(Map(
          "adjustmentType" -> "underDeclared",
          fieldName -> input
        ))
        result.errors mustBe empty
      }
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

    "must fail when volume >= 1000ml has any decimal places" in {
      Seq("1000.1", "1000.5", "2000.99").foreach { input =>
        val result = form.bind(Map(
          "adjustmentType" -> "underDeclared",
          fieldName -> input
        ))
        result.errors must contain(FormError(fieldName, "returns.adjustmentVolumeWithType.underDeclared.error.invalidDecimalPlaces.wholeOnly"))
      }
    }

    "must fail when volume < 1000ml has more than 1 decimal place" in {
      Seq("100.55", "999.99", "10.123").foreach { input =>
        val result = form.bind(Map(
          "adjustmentType" -> "underDeclared",
          fieldName -> input
        ))
        result.errors must contain(FormError(fieldName, "returns.adjustmentVolumeWithType.underDeclared.error.invalidDecimalPlaces.maxOne"))
      }
    }
  }

  ".overDeclaredVolume" - {

    val fieldName = "overDeclaredVolume"

    "must bind valid volumes >= 1000ml with no decimal places" in {
      val result = form.bind(Map(
        "adjustmentType" -> "overDeclared",
        fieldName -> "2000"
      ))
      result.errors mustBe empty
      result.value.value.overDeclaredVolume mustBe Some(BigDecimal("2000"))
    }

    "must bind valid volumes < 1000ml with 0 or 1 decimal place" in {
      Seq("200", "200.7", "999.9").foreach { input =>
        val result = form.bind(Map(
          "adjustmentType" -> "overDeclared",
          fieldName -> input
        ))
        result.errors mustBe empty
      }
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

    "must fail when volume >= 1000ml has any decimal places" in {
      Seq("2000.1", "2000.7", "3000.99").foreach { input =>
        val result = form.bind(Map(
          "adjustmentType" -> "overDeclared",
          fieldName -> input
        ))
        result.errors must contain(FormError(fieldName, "returns.adjustmentVolumeWithType.overDeclared.error.invalidDecimalPlaces.wholeOnly"))
      }
    }

    "must fail when volume < 1000ml has more than 1 decimal place" in {
      Seq("200.77", "999.99", "50.123").foreach { input =>
        val result = form.bind(Map(
          "adjustmentType" -> "overDeclared",
          fieldName -> input
        ))
        result.errors must contain(FormError(fieldName, "returns.adjustmentVolumeWithType.overDeclared.error.invalidDecimalPlaces.maxOne"))
      }
    }
  }

  "form validation" - {

    "must fail when both volumes are provided" in {
      // Note: The controller also clears the non-selected field as a defensive measure
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        "underDeclaredVolume" -> "100.5",
        "overDeclaredVolume" -> "200.7"
      ))
      result.errors must contain(FormError("", "returns.adjustmentVolumeWithType.error.bothProvided"))
    }

    "must not bind zero for under declared volume" in {
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        "underDeclaredVolume" -> "0"
      ))
      result.errors must contain(FormError("underDeclaredVolume", "returns.adjustmentVolumeWithType.underDeclared.error.mustBeGreaterThanZero"))
    }

    "must not bind zero for over declared volume" in {
      val result = form.bind(Map(
        "adjustmentType" -> "overDeclared",
        "overDeclaredVolume" -> "0"
      ))
      result.errors must contain(FormError("overDeclaredVolume", "returns.adjustmentVolumeWithType.overDeclared.error.mustBeGreaterThanZero"))
    }

    "must accept minimum valid volume of 1ml" in {
      Seq("1", "1.0").foreach { input =>
        val result = form.bind(Map(
          "adjustmentType" -> "underDeclared",
          "underDeclaredVolume" -> input
        ))
        result.errors mustBe empty
      }
    }

    "must accept maximum valid volume" in {
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        "underDeclaredVolume" -> "999999999999"
      ))
      result.errors mustBe empty
    }

    "must accept volumes below 1ml" in {
      Seq("0.1", "0.5", "0.9").foreach { input =>
        val result = form.bind(Map(
          "adjustmentType" -> "underDeclared",
          "underDeclaredVolume" -> input
        ))
        result.errors mustBe empty
      }
    }

    "must accept large volumes up to 13 digits" in {
      val result = form.bind(Map(
        "adjustmentType" -> "underDeclared",
        "underDeclaredVolume" -> "9999999999999"
      ))
      result.errors mustBe empty
    }
  }

  ".getVolume" - {

    "must return the correct volume for UnderDeclared" in {
      val formData = AdjustmentVolumeWithTypeFormData(
        adjustmentType = AdjustmentType.UnderDeclared,
        underDeclaredVolume = Some(BigDecimal("100.5")),
        overDeclaredVolume = None
      )
      formData.getVolume mustBe BigDecimal("100.5")
    }

    "must return the correct volume for OverDeclared" in {
      val formData = AdjustmentVolumeWithTypeFormData(
        adjustmentType = AdjustmentType.OverDeclared,
        underDeclaredVolume = None,
        overDeclaredVolume = Some(BigDecimal("200.7"))
      )
      formData.getVolume mustBe BigDecimal("200.7")
    }
  }
}