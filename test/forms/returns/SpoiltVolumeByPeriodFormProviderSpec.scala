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

import base.SpecBase
import models.identifiers.{PeriodKey, VpdId}
import models.returns.MaxVolumeResult
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.FormError
import services.returns.{DutyRateService, VolumePrecisionService}

import scala.concurrent.Future

class SpoiltVolumeByPeriodFormProviderSpec extends SpecBase with MockitoSugar {

  private val mockDutyRateService = mock[DutyRateService]
  private val mockVolumePrecisionService = mock[VolumePrecisionService]
  private val formProvider = new SpoiltVolumeByPeriodFormProvider(mockDutyRateService, mockVolumePrecisionService)

  private val testPeriodKey = PeriodKey("24KA")
  private val testVpdId = VpdId("VPDID123")
  private val testDutyRate = BigDecimal("3.37")
  private val testMaxVolume = BigDecimal("296735905.0")
  private val testFormattedMax = "296,735,905 ml"

  ".value" - {

    val fieldName = "value"

    "must bind valid values >= 1000ml with no decimal places" in {
      when(mockDutyRateService.getDutyRate(eqTo(testVpdId), eqTo(testPeriodKey))(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))
      when(mockVolumePrecisionService.calculateMaxVolume(any()))
        .thenReturn(MaxVolumeResult(testMaxVolume, testFormattedMax))

      whenReady(formProvider(testPeriodKey, testVpdId)) { form =>
        Seq("1000", "1000000", "100000000").foreach { input =>
          val result = form.bind(Map(fieldName -> input))
          result.errors mustBe empty
        }
      }
    }

    "must bind valid values < 1000ml with 0 or 1 decimal place" in {
      when(mockDutyRateService.getDutyRate(eqTo(testVpdId), eqTo(testPeriodKey))(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))
      when(mockVolumePrecisionService.calculateMaxVolume(any()))
        .thenReturn(MaxVolumeResult(testMaxVolume, testFormattedMax))

      whenReady(formProvider(testPeriodKey, testVpdId)) { form =>
        Seq("1", "10.1", "999.9", "500").foreach { input =>
          val result = form.bind(Map(fieldName -> input))
          result.errors mustBe empty
        }
      }
    }

    "must not bind empty values" in {
      when(mockDutyRateService.getDutyRate(eqTo(testVpdId), eqTo(testPeriodKey))(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))
      when(mockVolumePrecisionService.calculateMaxVolume(any()))
        .thenReturn(MaxVolumeResult(testMaxVolume, testFormattedMax))

      whenReady(formProvider(testPeriodKey, testVpdId)) { form =>
        val result = form.bind(Map(fieldName -> "")).apply(fieldName)
        result.errors mustEqual Seq(FormError(fieldName, "returns.spoiltVolumeByPeriod.error.required"))
      }
    }

    "must not bind non-numeric values" in {
      when(mockDutyRateService.getDutyRate(eqTo(testVpdId), eqTo(testPeriodKey))(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))
      when(mockVolumePrecisionService.calculateMaxVolume(any()))
        .thenReturn(MaxVolumeResult(testMaxVolume, testFormattedMax))

      whenReady(formProvider(testPeriodKey, testVpdId)) { form =>
        Seq("abc", "1.2.3", "£10.12").foreach { input =>
          val result = form.bind(Map(fieldName -> input)).apply(fieldName)
          result.errors mustEqual Seq(FormError(fieldName, "returns.spoiltVolumeByPeriod.error.nonNumeric"))
        }
      }
    }

    "must bind values >= 1000ml with trailing zeros" in {
      when(mockDutyRateService.getDutyRate(eqTo(testVpdId), eqTo(testPeriodKey))(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))
      when(mockVolumePrecisionService.calculateMaxVolume(any()))
        .thenReturn(MaxVolumeResult(testMaxVolume, testFormattedMax))

      whenReady(formProvider(testPeriodKey, testVpdId)) { form =>
        Seq("1000.0", "2000.0").foreach { input =>
          val result = form.bind(Map(fieldName -> input))
          result.errors mustBe empty
        }
      }
    }

    "must not bind values >= 1000ml with non-zero decimal places" in {
      when(mockDutyRateService.getDutyRate(eqTo(testVpdId), eqTo(testPeriodKey))(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))
      when(mockVolumePrecisionService.calculateMaxVolume(any()))
        .thenReturn(MaxVolumeResult(testMaxVolume, testFormattedMax))

      whenReady(formProvider(testPeriodKey, testVpdId)) { form =>
        Seq("1000.1", "1000.12").foreach { input =>
          val result = form.bind(Map(fieldName -> input)).apply(fieldName)
          result.errors mustEqual Seq(FormError(fieldName, "returns.spoiltVolumeByPeriod.error.invalidDecimalPlaces.wholeOnly"))
        }
      }
    }

    "must not bind values < 1000ml with more than 1 decimal place" in {
      when(mockDutyRateService.getDutyRate(eqTo(testVpdId), eqTo(testPeriodKey))(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))
      when(mockVolumePrecisionService.calculateMaxVolume(any()))
        .thenReturn(MaxVolumeResult(testMaxVolume, testFormattedMax))

      whenReady(formProvider(testPeriodKey, testVpdId)) { form =>
        Seq("999.99", "10.12", "1.123").foreach { input =>
          val result = form.bind(Map(fieldName -> input)).apply(fieldName)
          result.errors mustEqual Seq(FormError(fieldName, "returns.spoiltVolumeByPeriod.error.invalidDecimalPlaces.maxOne"))
        }
      }
    }

    "must not bind values below the minimum of 1ml" in {
      when(mockDutyRateService.getDutyRate(eqTo(testVpdId), eqTo(testPeriodKey))(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))
      when(mockVolumePrecisionService.calculateMaxVolume(any()))
        .thenReturn(MaxVolumeResult(testMaxVolume, testFormattedMax))

      whenReady(formProvider(testPeriodKey, testVpdId)) { form =>
        Seq("0", "0.1").foreach { input =>
          val result = form.bind(Map(fieldName -> input)).apply(fieldName)
          result.errors.head.key mustEqual fieldName
          result.errors.head.message mustEqual "returns.spoiltVolumeByPeriod.error.exceedsMaxDuty"
        }
      }
    }

    "must not bind values that exceed the calculated maximum" in {
      when(mockDutyRateService.getDutyRate(eqTo(testVpdId), eqTo(testPeriodKey))(using any(), any()))
        .thenReturn(Future.successful(testDutyRate))
      when(mockVolumePrecisionService.calculateMaxVolume(337))
        .thenReturn(MaxVolumeResult(testMaxVolume, testFormattedMax))

      whenReady(formProvider(testPeriodKey, testVpdId)) { form =>
        val result = form.bind(Map(fieldName -> "999999999999")).apply(fieldName)
        result.errors.head.key mustEqual fieldName
        result.errors.head.message mustEqual "returns.spoiltVolumeByPeriod.error.exceedsMaxDuty"
        result.errors.head.args mustEqual Seq(testFormattedMax)
      }
    }
  }
}