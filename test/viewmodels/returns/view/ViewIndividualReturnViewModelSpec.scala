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
      vapingProductsProduced = Some(VapingProductsProduced(vapingProdManufactured = "0", returns = Seq.empty))
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
      val result = ViewIndividualReturnViewModel(returnResponse, Some(testDutyRate))

      result.chargeReference mustBe "XVC123456789012"
      result.hasVapingProductsDeclaration mustBe true
      result.amountProducedLiquid mustBe Some("1,000,000.00")
      result.dutyDue mustBe Some("£3,150")
      result.totalDutySpoiltProducts mustBe "£-100"
      result.totalDutyDue mustBe "£1,825"
      result.monthYear mustBe "June 2026"
      result.submittedOn must include("February 2026")
      result.dutyRate mustBe "£3.15"
    }

    "must default duty rate to £0 when none is supplied" in {
      val result = ViewIndividualReturnViewModel(returnResponse, None)

      result.dutyRate mustBe "£0"
    }

    "must handle missing charge reference" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoChargeRef, Some(testDutyRate))

      result.chargeReference mustBe ""
    }

    "must handle missing vaping products declaration" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoDeclaration, Some(testDutyRate))

      result.hasVapingProductsDeclaration mustBe false
      result.amountProducedLiquid mustBe None
      result.dutyDue mustBe None
    }

    "must default to £0.00 when totalDutyDue is missing" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoTotalDuty, Some(testDutyRate))

      result.totalDutySpoiltProducts mustBe "£0"
      result.totalDutyDue mustBe "£0"
    }

    "must correctly identify when vaping products declaration exists" in {
      val responseWithDeclaration = returnResponse.copy(
        success = returnResponse.success.copy(
          vapingProductsProduced = Some(
            VapingProductsProduced(
              vapingProdManufactured = "1",
              returns = Seq(
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

      val result = ViewIndividualReturnViewModel(responseWithDeclaration, Some(testDutyRate))

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
              vapingProdManufactured = "1",
              returns = Seq(
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

      val viewModel = ViewIndividualReturnViewModel(responseWithDeclaration, Some(testDutyRate))
      val result = viewModel.vapingProductsDeclarationSummaryList

      result.rows.size mustBe 1
      result.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.vapingProductsDeclaration.question"))
      result.rows.head.value.content.asHtml.toString must include(messages("viewIndividualReturn.vapingProductsDeclaration.yes"))
    }

    "must show correct answer when no declaration" in {
      val viewModel = ViewIndividualReturnViewModel(returnResponseNoDeclaration, Some(testDutyRate))
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
              vapingProdManufactured = "1",
              returns = Seq(
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

      val viewModel = ViewIndividualReturnViewModel(responseWithDeclaration, Some(testDutyRate))
      val result = viewModel.productDetailsSummaryList

      result mustBe defined
      result.get.rows.size mustBe 2
    }

    "must return None when no declaration exists" in {
      val viewModel = ViewIndividualReturnViewModel(returnResponseNoDeclaration, Some(testDutyRate))
      val result = viewModel.productDetailsSummaryList

      result mustBe None
    }
  }

  "dutyTotalsSummaryList" - {

    "must return summary list with spoilt product detail rows and total rows" in {
      val viewModel = ViewIndividualReturnViewModel(returnResponse, Some(testDutyRate))
      val result = viewModel.dutyTotalsSummaryList

      result.rows.size mustBe 5
      result.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.question"))
      result.rows(1).key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.month"))
      result.rows(2).key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.spoiltProducts"))
      result.rows(3).key.content.asHtml.toString must include(messages("viewIndividualReturn.totalDutySpoiltProducts"))
      result.rows(4).key.content.asHtml.toString must include(messages("viewIndividualReturn.totalDutyDue"))
    }
  }

  "dutySuspenseSummaryList" - {

    "must return None when otherOptions is None" in {
      val responseNoOtherOptions = returnResponse.copy(
        success = returnResponse.success.copy(otherOptions = None)
      )

      val viewModel = ViewIndividualReturnViewModel(responseNoOtherOptions, Some(testDutyRate))
      val result = viewModel.dutySuspenseSummaryList

      result mustBe None
    }

    "must return summary list with single row when vapingProductUnderDutySuspense is 0" in {
      val otherOptionsNo = OtherOptions(
        vapingProductUnderDutySuspense = "0",
        volumeMovedFromDutySuspense = None,
        volumeMovedToDutySuspense = None
      )
      val responseWithNo = returnResponse.copy(
        success = returnResponse.success.copy(otherOptions = Some(otherOptionsNo))
      )

      val viewModel = ViewIndividualReturnViewModel(responseWithNo, Some(testDutyRate))
      val result = viewModel.dutySuspenseSummaryList

      result mustBe defined
      result.get.rows.size mustBe 1
      result.get.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.question"))
      result.get.rows.head.value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.no"))
    }

    "must return summary list with 3 rows when vapingProductUnderDutySuspense is 1 with volumes" in {
      val otherOptionsYes = OtherOptions(
        vapingProductUnderDutySuspense = "1",
        volumeMovedFromDutySuspense = Some(BigDecimal("300")),
        volumeMovedToDutySuspense = Some(BigDecimal("150"))
      )
      val responseWithYes = returnResponse.copy(
        success = returnResponse.success.copy(otherOptions = Some(otherOptionsYes))
      )

      val viewModel = ViewIndividualReturnViewModel(responseWithYes, Some(testDutyRate))
      val result = viewModel.dutySuspenseSummaryList

      result mustBe defined
      result.get.rows.size mustBe 3
      result.get.rows(0).key.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.question"))
      result.get.rows(0).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.yes"))
      result.get.rows(1).key.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.productReceived"))
      result.get.rows(1).value.content.asHtml.toString must include("300,000.00 ml")
      result.get.rows(2).key.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.productMoved"))
      result.get.rows(2).value.content.asHtml.toString must include("150,000.00 ml")
    }

    "must show 'Nothing to declare' when volumeMovedFromDutySuspense is 0" in {
      val otherOptionsZeroReceived = OtherOptions(
        vapingProductUnderDutySuspense = "1",
        volumeMovedFromDutySuspense = Some(BigDecimal("0")),
        volumeMovedToDutySuspense = Some(BigDecimal("150"))
      )
      val responseWithZero = returnResponse.copy(
        success = returnResponse.success.copy(otherOptions = Some(otherOptionsZeroReceived))
      )

      val viewModel = ViewIndividualReturnViewModel(responseWithZero, Some(testDutyRate))
      val result = viewModel.dutySuspenseSummaryList

      result mustBe defined
      result.get.rows(1).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.nothingToDeclare"))
    }

    "must show 'Nothing to declare' when volumeMovedToDutySuspense is None" in {
      val otherOptionsNoneMoved = OtherOptions(
        vapingProductUnderDutySuspense = "1",
        volumeMovedFromDutySuspense = Some(BigDecimal("300")),
        volumeMovedToDutySuspense = None
      )
      val responseWithNone = returnResponse.copy(
        success = returnResponse.success.copy(otherOptions = Some(otherOptionsNoneMoved))
      )

      val viewModel = ViewIndividualReturnViewModel(responseWithNone, Some(testDutyRate))
      val result = viewModel.dutySuspenseSummaryList

      result mustBe defined
      result.get.rows(2).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.nothingToDeclare"))
    }

    "must show 'Nothing to declare' for both volumes when both are 0" in {
      val otherOptionsBothZero = OtherOptions(
        vapingProductUnderDutySuspense = "1",
        volumeMovedFromDutySuspense = Some(BigDecimal("0")),
        volumeMovedToDutySuspense = Some(BigDecimal("0"))
      )
      val responseWithBothZero = returnResponse.copy(
        success = returnResponse.success.copy(otherOptions = Some(otherOptionsBothZero))
      )

      val viewModel = ViewIndividualReturnViewModel(responseWithBothZero, Some(testDutyRate))
      val result = viewModel.dutySuspenseSummaryList

      result mustBe defined
      result.get.rows(1).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.nothingToDeclare"))
      result.get.rows(2).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.nothingToDeclare"))
    }
  }
}
