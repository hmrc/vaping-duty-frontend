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
import models.returns.{RegularReturn, VapingProductsProduced}
import models.returns.view.*
import play.api.i18n.Messages

import java.time.{Instant, LocalDate}

class ViewIndividualReturnViewModelSpec extends SpecBase {

  implicit val messages: Messages = messages(applicationBuilder(None).build())

  private val returnResponse = createReturnDisplayResponse()

  private val returnResponseNoDeclaration = returnResponse.copy(
    success = returnResponse.success.copy(
      vapingProductsProduced = Some(VapingProductsProduced(Seq.empty, Seq.empty))
    )
  )

  private val returnResponseNoChargeRef = returnResponse.copy(
    success = returnResponse.success.copy(
      chargeDetails = returnResponse.success.chargeDetails.map(_.copy(chargeReference = None))
    )
  )

  private val returnResponseNoTotalDuty = returnResponse.copy(
    success = returnResponse.success.copy(totalDutyDue = None)
  )

  "ViewIndividualReturnViewModel" - {

    "must create view model with all fields from ReturnDisplayResponse" in {
      val result = ViewIndividualReturnViewModel(returnResponse, testDutyRate)

      result.chargeReference mustBe "XVC123456789012"
      result.hasVapingProductsDeclaration mustBe false
      result.amountProducedLiquid mustBe None
      result.dutyDue mustBe None
      result.totalDutyDueVapingProducts mustBe "£1,000"
      result.totalDutyDue mustBe "£1,825"
      result.monthYear mustBe "June 2026"
      result.submittedOn must include("June 2026")
      result.dutyRate mustBe "£3.15"
    }

    "must handle missing charge reference" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoChargeRef, testDutyRate)

      result.chargeReference mustBe ""
    }

    "must handle missing vaping products declaration" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoDeclaration, testDutyRate)

      result.hasVapingProductsDeclaration mustBe false
      result.amountProducedLiquid mustBe None
      result.dutyDue mustBe None
    }

    "must default to £0.00 when totalDutyDue is missing" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoTotalDuty, testDutyRate)

      result.totalDutyDueVapingProducts mustBe "£0"
      result.totalDutyDue mustBe "£0"
    }

    "must correctly identify when vaping products declaration exists" in {
      val responseWithDeclaration = returnResponse.copy(
        success = returnResponse.success.copy(
          vapingProductsProduced = Some(
            VapingProductsProduced(
              nilReturn = Seq.empty,
              regularReturn = Seq(
                RegularReturn(
                  taxType = "311",
                  dutyRate = BigDecimal("0.50"),
                  amountProducedLiquid = BigDecimal("2000.00"),
                  dutyDue = BigDecimal("1000.00")
                )
              )
            )
          )
        )
      )

      val result = ViewIndividualReturnViewModel(responseWithDeclaration, testDutyRate)

      result.hasVapingProductsDeclaration mustBe true
      result.amountProducedLiquid mustBe Some("2,000,000.00")
      result.dutyDue mustBe Some("£1,000")
    }
  }

  "vapingProductsDeclarationSummaryList" - {

    "must show correct answer when declaration exists" in {
      val responseWithDeclaration = returnResponse.copy(
        success = returnResponse.success.copy(
          vapingProductsProduced = Some(
            VapingProductsProduced(
              nilReturn = Seq.empty,
              regularReturn = Seq(
                RegularReturn(
                  taxType = "311",
                  dutyRate = BigDecimal("0.50"),
                  amountProducedLiquid = BigDecimal("2000.00"),
                  dutyDue = BigDecimal("1000.00")
                )
              )
            )
          )
        )
      )

      val viewModel = ViewIndividualReturnViewModel(responseWithDeclaration, testDutyRate)
      val result = viewModel.vapingProductsDeclarationSummaryList

      result.rows.size mustBe 1
      result.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.vapingProductsDeclaration.question"))
      result.rows.head.value.content.asHtml.toString must include(messages("viewIndividualReturn.vapingProductsDeclaration.yes"))
    }

    "must show correct answer when no declaration" in {
      val viewModel = ViewIndividualReturnViewModel(returnResponseNoDeclaration, testDutyRate)
      val result = viewModel.vapingProductsDeclarationSummaryList

      result.rows.size mustBe 1
      result.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.vapingProductsDeclaration.question"))
      result.rows.head.value.content.asHtml.toString must include(messages("viewIndividualReturn.vapingProductsDeclaration.no"))
    }
  }

  "productDetailsSummaryList" - {

    "must return Some with rows when declaration exists" in {
      val responseWithDeclaration = returnResponse.copy(
        success = returnResponse.success.copy(
          vapingProductsProduced = Some(
            VapingProductsProduced(
              nilReturn = Seq.empty,
              regularReturn = Seq(
                RegularReturn(
                  taxType = "311",
                  dutyRate = BigDecimal("0.50"),
                  amountProducedLiquid = BigDecimal("2000.00"),
                  dutyDue = BigDecimal("1000.00")
                )
              )
            )
          )
        )
      )

      val viewModel = ViewIndividualReturnViewModel(responseWithDeclaration, testDutyRate)
      val result = viewModel.productDetailsSummaryList

      result mustBe defined
      result.get.rows.size mustBe 2
    }

    "must return None when no declaration exists" in {
      val viewModel = ViewIndividualReturnViewModel(returnResponseNoDeclaration, testDutyRate)
      val result = viewModel.productDetailsSummaryList

      result mustBe None
    }
  }

  "dutyTotalsSummaryList" - {

    "must return summary list with two rows" in {
      val viewModel = ViewIndividualReturnViewModel(returnResponse, testDutyRate)
      val result = viewModel.dutyTotalsSummaryList

      result.rows.size mustBe 2
      result.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.totalDutyDueVapingProducts"))
      result.rows(1).key.content.asHtml.toString must include(messages("viewIndividualReturn.totalDutyDue"))
    }
  }
}