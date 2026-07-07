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

package utils

import base.UnitSpec

class CurrencyFormatterSpec extends UnitSpec with CurrencyFormatter {

  "currencyFormat" - {
    "must format positive amounts correctly" in {
      currencyFormat(BigDecimal("1234.56")) mustBe "£1,234.56"
    }

    "must format negative amounts correctly" in {
      currencyFormat(BigDecimal("-1234.56")) mustBe "£-1,234.56"
    }

    "must remove trailing zeros" in {
      currencyFormat(BigDecimal("1234.00")) mustBe "£1,234"
    }

    "must format zero correctly" in {
      currencyFormat(BigDecimal("0.00")) mustBe "£0"
    }
  }

  "currencyFormatWithLeadingSign" - {
    "must format positive amounts correctly" in {
      currencyFormatWithLeadingSign(BigDecimal("1234.56")) mustBe "£1,234.56"
    }

    "must format negative amounts with leading minus sign" in {
      currencyFormatWithLeadingSign(BigDecimal("-1234.56")) mustBe "-£1,234.56"
    }

    "must remove trailing zeros for positive amounts" in {
      currencyFormatWithLeadingSign(BigDecimal("1234.00")) mustBe "£1,234"
    }

    "must remove trailing zeros for negative amounts" in {
      currencyFormatWithLeadingSign(BigDecimal("-1234.00")) mustBe "-£1,234"
    }

    "must format zero correctly" in {
      currencyFormatWithLeadingSign(BigDecimal("0.00")) mustBe "£0"
    }

    "must format small negative amounts correctly" in {
      currencyFormatWithLeadingSign(BigDecimal("-0.50")) mustBe "-£0.50"
    }
  }

  "milliliterFormat" - {
    "must format amounts with two decimal places" in {
      milliliterFormat(BigDecimal("1234.567")) mustBe "1,234.56"
    }

    "must truncate rather than round" in {
      milliliterFormat(BigDecimal("1234.999")) mustBe "1,234.99"
    }
  }
}