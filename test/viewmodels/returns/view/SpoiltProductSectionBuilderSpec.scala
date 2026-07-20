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
import data.TestData
import models.identifiers.PeriodKey
import models.obligations.{ObligationItem, ObligationsResponse}
import models.returns.view.{SpoiltProduct, SpoiltProductItem}
import play.api.i18n.Messages
import utils.ReturnsDateUtils

class SpoiltProductSectionBuilderSpec extends SpecBase with TestData {

  implicit val messages: Messages = messages(applicationBuilder(None).build())

  private val returnsDateUtils = new ReturnsDateUtils(clock)
  private val obligationDetails = fulfilledObligation(periodKey)
  private val obligations = ObligationsResponse(obligation = Seq(ObligationItem(None, obligationDetails)))

  private val spoiltProductItem = SpoiltProductItem(
    returnPeriodAffected = periodKey.value,
    taxType = "311",
    dutyRate = BigDecimal("0.50"),
    amountSpoilt = BigDecimal("200.00"),
    dutyDue = BigDecimal("100.00")
  )

  "SpoiltProductSectionBuilder" - {

    "build must" - {

      "return empty sequence when spoiltProduct is None" in {
        val builder = SpoiltProductSectionBuilder(
          spoiltProduct = None,
          nilReturn = false,
          totalDutySpoiltProducts = "£0",
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result mustBe Seq.empty
      }

      "return single list with question row when spoiltProductFilled is 0" in {
        val spoiltProduct = SpoiltProduct(spoiltProductFilled = "0", spoiltProducts = None)
        val builder = SpoiltProductSectionBuilder(
          spoiltProduct = Some(spoiltProduct),
          nilReturn = false,
          totalDutySpoiltProducts = "£0",
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.size mustBe 1
        result.head.rows.size mustBe 1
        result.head.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.question"))
        result.head.rows.head.value.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.no"))
      }

      "return full details when spoiltProductFilled is 1 with items, even if nilReturn is true" in {
        val spoiltProduct = SpoiltProduct(spoiltProductFilled = "1", spoiltProducts = Some(Seq(spoiltProductItem)))
        val builder = SpoiltProductSectionBuilder(
          spoiltProduct = Some(spoiltProduct),
          nilReturn = true,
          totalDutySpoiltProducts = "-£100",
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.size mustBe 1
        result.head.rows.size mustBe 4
        result.head.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.question"))
        result.head.rows.head.value.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.yes"))
        result.head.rows(1).key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.month"))
        result.head.rows(2).key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.spoiltProducts"))
        result.head.rows(3).key.content.asHtml.toString must include(messages("viewIndividualReturn.dutyDue"))
      }

      "return single list with question and details when single item" in {
        val spoiltProduct = SpoiltProduct(spoiltProductFilled = "1", spoiltProducts = Some(Seq(spoiltProductItem)))
        val builder = SpoiltProductSectionBuilder(
          spoiltProduct = Some(spoiltProduct),
          nilReturn = false,
          totalDutySpoiltProducts = "-£100",
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.size mustBe 1
        result.head.rows.size mustBe 4
        result.head.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.question"))
        result.head.rows(1).key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.month"))
        result.head.rows(2).key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.spoiltProducts"))
        result.head.rows(3).key.content.asHtml.toString must include(messages("viewIndividualReturn.dutyDue"))
      }

      "return multiple lists when multiple items" in {
        val item2 = spoiltProductItem.copy(amountSpoilt = BigDecimal("300.00"), dutyDue = BigDecimal("150.00"))
        val spoiltProduct = SpoiltProduct(spoiltProductFilled = "1", spoiltProducts = Some(Seq(spoiltProductItem, item2)))
        val builder = SpoiltProductSectionBuilder(
          spoiltProduct = Some(spoiltProduct),
          nilReturn = false,
          totalDutySpoiltProducts = "-£250",
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.size mustBe 2
        result.head.rows.size mustBe 4  // Question + first item details
        result(1).rows.size mustBe 3     // Second item details only
      }

      "format duty due with minus sign" in {
        val spoiltProduct = SpoiltProduct(spoiltProductFilled = "1", spoiltProducts = Some(Seq(spoiltProductItem)))
        val builder = SpoiltProductSectionBuilder(
          spoiltProduct = Some(spoiltProduct),
          nilReturn = false,
          totalDutySpoiltProducts = "-£100",
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.head.rows(3).value.content.asHtml.toString must include("-£100")
      }

      "format millilitres correctly" in {
        val spoiltProduct = SpoiltProduct(spoiltProductFilled = "1", spoiltProducts = Some(Seq(spoiltProductItem)))
        val builder = SpoiltProductSectionBuilder(
          spoiltProduct = Some(spoiltProduct),
          nilReturn = false,
          totalDutySpoiltProducts = "-£100",
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.head.rows(2).value.content.asHtml.toString must include("200,000.00 ml")
      }

      "lookup period key correctly" in {
        val spoiltProduct = SpoiltProduct(spoiltProductFilled = "1", spoiltProducts = Some(Seq(spoiltProductItem)))
        val builder = SpoiltProductSectionBuilder(
          spoiltProduct = Some(spoiltProduct),
          nilReturn = false,
          totalDutySpoiltProducts = "-£100",
          obligations = obligations,
          returnsDateUtils = returnsDateUtils
        )
        val result = builder.build()

        result.head.rows(1).value.content.asHtml.toString must include("June 2026")
      }

      "throw exception when period key not found" in {
        val obligationsWithMissingPeriod = ObligationsResponse(
          obligation = Seq(ObligationItem(None, fulfilledObligation(PeriodKey("26AF"))))
        )
        val itemWithMissingPeriod = spoiltProductItem.copy(returnPeriodAffected = "24AC")
        val spoiltProduct = SpoiltProduct(spoiltProductFilled = "1", spoiltProducts = Some(Seq(itemWithMissingPeriod)))
        val builder = SpoiltProductSectionBuilder(
          spoiltProduct = Some(spoiltProduct),
          nilReturn = false,
          totalDutySpoiltProducts = "-£100",
          obligations = obligationsWithMissingPeriod,
          returnsDateUtils = returnsDateUtils
        )

        val exception = intercept[IllegalStateException] {
          builder.build()
        }

        exception.getMessage must include("Period key 24AC not found in obligations")
      }
    }
  }
}