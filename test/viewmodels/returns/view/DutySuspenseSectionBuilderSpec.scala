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
import models.returns.view.OtherOptions
import play.api.i18n.Messages

class DutySuspenseSectionBuilderSpec extends SpecBase {

  implicit val messages: Messages = messages(applicationBuilder(None).build())

  "DutySuspenseSectionBuilder" - {

    "build must" - {

      "return None when otherOptions is None" in {
        val builder = DutySuspenseSectionBuilder(None)
        val result = builder.build()

        result mustBe None
      }

      "return Some with 1 row when vapingProductUnderDutySuspense is 0" in {
        val otherOptions = OtherOptions(
          vapingProductUnderDutySuspense = "0",
          volumeMovedFromDutySuspense = None,
          volumeMovedToDutySuspense = None
        )
        val builder = DutySuspenseSectionBuilder(Some(otherOptions))
        val result = builder.build()

        result mustBe defined
        result.get.rows.size mustBe 1
        result.get.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.question"))
        result.get.rows.head.value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.no"))
      }

      "return Some with 3 rows when vapingProductUnderDutySuspense is 1 with volumes" in {
        val otherOptions = OtherOptions(
          vapingProductUnderDutySuspense = "1",
          volumeMovedFromDutySuspense = Some(BigDecimal("300")),
          volumeMovedToDutySuspense = Some(BigDecimal("150"))
        )
        val builder = DutySuspenseSectionBuilder(Some(otherOptions))
        val result = builder.build()

        result mustBe defined
        result.get.rows.size mustBe 3
        result.get.rows(0).key.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.question"))
        result.get.rows(0).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.yes"))
        result.get.rows(1).key.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.productReceived"))
        result.get.rows(1).value.content.asHtml.toString must include("300,000.00 ml")
        result.get.rows(2).key.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.productMoved"))
        result.get.rows(2).value.content.asHtml.toString must include("150,000.00 ml")
      }

      "show 'Nothing to declare' when volumeMovedFromDutySuspense is 0" in {
        val otherOptions = OtherOptions(
          vapingProductUnderDutySuspense = "1",
          volumeMovedFromDutySuspense = Some(BigDecimal("0")),
          volumeMovedToDutySuspense = Some(BigDecimal("150"))
        )
        val builder = DutySuspenseSectionBuilder(Some(otherOptions))
        val result = builder.build()

        result mustBe defined
        result.get.rows(1).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.nothingToDeclare"))
      }

      "show 'Nothing to declare' when volumeMovedToDutySuspense is None" in {
        val otherOptions = OtherOptions(
          vapingProductUnderDutySuspense = "1",
          volumeMovedFromDutySuspense = Some(BigDecimal("300")),
          volumeMovedToDutySuspense = None
        )
        val builder = DutySuspenseSectionBuilder(Some(otherOptions))
        val result = builder.build()

        result mustBe defined
        result.get.rows(2).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.nothingToDeclare"))
      }

      "show 'Nothing to declare' for both volumes when both are 0" in {
        val otherOptions = OtherOptions(
          vapingProductUnderDutySuspense = "1",
          volumeMovedFromDutySuspense = Some(BigDecimal("0")),
          volumeMovedToDutySuspense = Some(BigDecimal("0"))
        )
        val builder = DutySuspenseSectionBuilder(Some(otherOptions))
        val result = builder.build()

        result mustBe defined
        result.get.rows(1).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.nothingToDeclare"))
        result.get.rows(2).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.nothingToDeclare"))
      }

      "format millilitres correctly" in {
        val otherOptions = OtherOptions(
          vapingProductUnderDutySuspense = "1",
          volumeMovedFromDutySuspense = Some(BigDecimal("1.5")),
          volumeMovedToDutySuspense = Some(BigDecimal("2.75"))
        )
        val builder = DutySuspenseSectionBuilder(Some(otherOptions))
        val result = builder.build()

        result mustBe defined
        result.get.rows(1).value.content.asHtml.toString must include("1,500.00 ml")
        result.get.rows(2).value.content.asHtml.toString must include("2,750.00 ml")
      }
    }
  }
}