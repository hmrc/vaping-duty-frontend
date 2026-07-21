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

package viewmodels.returns.view

import base.SpecBase
import models.returns.TotalDutyDue
import play.api.i18n.Messages
import utils.CssConstants

class TotalDutySectionBuilderSpec extends SpecBase {

  implicit val messages: Messages = messages(applicationBuilder(None).build())

  private val totalDutyDue = TotalDutyDue(
    totalDutyDueVapingProducts = BigDecimal("1000.00"),
    totalDutySpoiltProduct = BigDecimal("-100.00"),
    totalDutyUnderDeclaration = BigDecimal("50.00"),
    totalDutyOverDeclaration = BigDecimal("-25.00"),
    totalDue = BigDecimal("925.00")
  )

  "TotalDutySectionBuilder" - {

    "build must" - {

      "return summary list with 5 rows when totalDutyDue exists" in {
        val builder = TotalDutySectionBuilder(Some(totalDutyDue))
        val result = builder.build()

        result.rows.size mustBe 5
      }

      "return empty summary list when totalDutyDue is None" in {
        val builder = TotalDutySectionBuilder(None)
        val result = builder.build()

        result.rows.size mustBe 0
      }

      "format totalDutyDueVapingProducts correctly" in {
        val builder = TotalDutySectionBuilder(Some(totalDutyDue))
        val result = builder.build()

        result.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.totals.totalDutyDueVapingProducts"))
        result.rows.head.value.content.asHtml.toString must include("£1,000")
      }

      "format totalDutySpoiltProduct with minus when negative" in {
        val builder = TotalDutySectionBuilder(Some(totalDutyDue))
        val result = builder.build()

        result.rows(1).key.content.asHtml.toString must include(messages("viewIndividualReturn.totals.totalDutySpoiltProduct"))
        result.rows(1).value.content.asHtml.toString must include("-£100")
      }

      "format totalDutySpoiltProduct without minus when zero" in {
        val totalDutyDueZeroSpoilt = totalDutyDue.copy(totalDutySpoiltProduct = BigDecimal("0"))
        val builder = TotalDutySectionBuilder(Some(totalDutyDueZeroSpoilt))
        val result = builder.build()

        result.rows(1).value.content.asHtml.toString must include("£0")
        result.rows(1).value.content.asHtml.toString mustNot include("-£0")
      }

      "format totalDutyUnderDeclaration correctly" in {
        val builder = TotalDutySectionBuilder(Some(totalDutyDue))
        val result = builder.build()

        result.rows(2).key.content.asHtml.toString must include(messages("viewIndividualReturn.totals.totalDutyUnderDeclaration"))
        result.rows(2).value.content.asHtml.toString must include("£50")
      }

      "format totalDutyOverDeclaration with minus when negative" in {
        val builder = TotalDutySectionBuilder(Some(totalDutyDue))
        val result = builder.build()

        result.rows(3).key.content.asHtml.toString must include(messages("viewIndividualReturn.totals.totalDutyOverDeclaration"))
        result.rows(3).value.content.asHtml.toString must include("-£25")
      }

      "format totalDutyOverDeclaration without minus when zero" in {
        val totalDutyDueZeroOver = totalDutyDue.copy(totalDutyOverDeclaration = BigDecimal("0"))
        val builder = TotalDutySectionBuilder(Some(totalDutyDueZeroOver))
        val result = builder.build()

        result.rows(3).value.content.asHtml.toString must include("£0")
        result.rows(3).value.content.asHtml.toString mustNot include("-£0")
      }

      "format totalDue with minus when negative" in {
        val totalDutyDueNegative = totalDutyDue.copy(totalDue = BigDecimal("-500.00"))
        val builder = TotalDutySectionBuilder(Some(totalDutyDueNegative))
        val result = builder.build()

        result.rows(4).key.content.asHtml.toString must include(messages("viewIndividualReturn.totals.totalDue"))
        result.rows(4).value.content.asHtml.toString must include("-£500")
      }

      "format totalDue without minus when positive" in {
        val builder = TotalDutySectionBuilder(Some(totalDutyDue))
        val result = builder.build()

        result.rows(4).value.content.asHtml.toString must include("£925")
        result.rows(4).value.content.asHtml.toString mustNot include("-£925")
      }

      "apply bold formatting to all keys" in {
        val builder = TotalDutySectionBuilder(Some(totalDutyDue))
        val result = builder.build()

        result.rows.foreach { row =>
          row.key.classes must include(CssConstants.boldFontWeight)
        }
      }

      "apply bold formatting to totalDue value" in {
        val builder = TotalDutySectionBuilder(Some(totalDutyDue))
        val result = builder.build()

        result.rows(4).value.classes must include(CssConstants.boldFontWeight)
      }
    }
  }
}