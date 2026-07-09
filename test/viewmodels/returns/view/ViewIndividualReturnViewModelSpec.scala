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
import models.identifiers.PeriodKey
import models.obligations.{ObligationDetails, ObligationsResponse}
import models.returns.view.*
import models.returns.{RegularReturn, VapingProductsProduced}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import utils.ReturnsDateUtils

import java.time.LocalDate

class ViewIndividualReturnViewModelSpec extends SpecBase {

  implicit val messages: Messages = messages(applicationBuilder(None).build())

  private val returnsDateUtils = new ReturnsDateUtils(clock)
  private val obligations: ObligationsResponse = createMockObligationsResponse()
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
      val result = ViewIndividualReturnViewModel(returnResponse, obligations, returnsDateUtils)

      result.chargeReference mustBe "XVC123456789012"
      result.hasVapingProductsDeclaration mustBe true
      result.amountProducedLiquid mustBe Some("1,000,000.00")
      result.dutyDue mustBe Some("£3,150")
      result.totalDutySpoiltProducts mustBe "-£100"
      result.monthYear mustBe "June 2026"
      result.submittedOn must include("February 2026")
      result.dutyRate mustBe "£3.15"
      result.personalDetailsSummaryList.rows.size mustBe 3
      result.dutyDeclarationSummaryList mustBe defined
      result.spoiltSummaryList.rows.nonEmpty mustBe true
      result.totalDutySummaryList.rows.nonEmpty mustBe true
    }

    "must default duty rate to £0 when none is supplied" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoDeclaration, obligations, returnsDateUtils)

      result.dutyRate mustBe "£0"
    }

    "must handle missing charge reference" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoChargeRef, obligations, returnsDateUtils)

      result.chargeReference mustBe ""
    }

    "must handle missing vaping products declaration" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoDeclaration, obligations, returnsDateUtils)

      result.hasVapingProductsDeclaration mustBe false
      result.amountProducedLiquid mustBe None
      result.dutyDue mustBe None
    }

    "must default to £0.00 when totalDutyDue is missing" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoTotalDuty, obligations, returnsDateUtils)

      result.totalDutySpoiltProducts mustBe "£0"
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

      val result = ViewIndividualReturnViewModel(responseWithDeclaration, obligations, returnsDateUtils)

      result.hasVapingProductsDeclaration mustBe true
      result.amountProducedLiquid mustBe Some("2,000,000.00")
      result.dutyDue mustBe Some("£1,000")
    }
  }

  "dutyDeclarationSummaryList" - {

    "must be pre-computed with rows when declaration exists" in {
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

      val viewModel = ViewIndividualReturnViewModel(responseWithDeclaration, obligations, returnsDateUtils)
      val result = viewModel.dutyDeclarationSummaryList

      result mustBe defined
      result.get.rows.size mustBe 3
    }

    "must include a row with No when no declaration" in {
      val viewModel = ViewIndividualReturnViewModel(returnResponseNoDeclaration, obligations, returnsDateUtils)
      val result = viewModel.dutyDeclarationSummaryList.get.rows.head.value.content

      result mustBe Text("No")
    }
  }

  "spoiltSummaryList" - {

    "must be pre-computed with spoilt product detail rows" in {
      val viewModel = ViewIndividualReturnViewModel(returnResponse, obligations, returnsDateUtils)
      val result = viewModel.spoiltSummaryList

      result.rows.size mustBe 4
      result.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.question"))
      result.rows(1).key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.month"))
      result.rows(2).key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.spoiltProducts"))
      result.rows(3).key.content.asHtml.toString must include(messages("viewIndividualReturn.totalDutySpoiltProducts"))
    }

    "must hide totalDutySpoiltProducts row when spoilt products answer is No" in {
      val noSpoiltResponse = returnResponse.copy(
        success = returnResponse.success.copy(
          spoiltProduct = Some(SpoiltProduct(spoiltProductFilled = "0", spoiltProducts = None))
        )
      )
      val viewModel = ViewIndividualReturnViewModel(noSpoiltResponse, obligations, returnsDateUtils)
      val result = viewModel.spoiltSummaryList

      result.rows.size mustBe 1
      result.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.question"))
      result.rows.forall(row =>
        !row.key.content.asHtml.toString.contains(messages("viewIndividualReturn.totalDutySpoiltProducts"))
      ) mustBe true
    }

    "must only include question row when nilReturn is true" in {
      val nilReturnResponse = returnResponseNoDeclaration
      val nilViewModel = ViewIndividualReturnViewModel(nilReturnResponse, obligations, returnsDateUtils)
      val result = nilViewModel.spoiltSummaryList

      result.rows.size mustBe 3
    }
  }

  "period key lookup" - {

    "must throw IllegalStateException when period key not found in obligations during view model creation" in {
      val obligationsWithMissingPeriod = ObligationsResponse(
        obligation = obligations(Seq(fulfilledObligation(PeriodKey("26AF"))))
      )

      val responseWithSpoiltProduct = returnResponse.copy(
        success = returnResponse.success.copy(
          spoiltProduct = Some(SpoiltProduct(
            spoiltProductFilled = "1",
            spoiltProducts = Some(Seq(
              SpoiltProductItem(
                returnPeriodAffected = "24AC",
                taxType = "311",
                dutyRate = BigDecimal("0.50"),
                amountSpoilt = BigDecimal("200.00"),
                dutyDue = BigDecimal("100.00")
              )
            ))
          ))
        )
      )

      val exception = intercept[IllegalStateException] {
        ViewIndividualReturnViewModel(responseWithSpoiltProduct, obligationsWithMissingPeriod, returnsDateUtils)
      }

      exception.getMessage must include("Period key 24AC not found in obligations")
    }
  }

  "dutySuspenseSummaryList" - {

    "must return None when otherOptions is None" in {
      val responseNoOtherOptions = returnResponse.copy(
        success = returnResponse.success.copy(otherOptions = None)
      )

      val viewModel = ViewIndividualReturnViewModel(responseNoOtherOptions, obligations, returnsDateUtils)
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

      val viewModel = ViewIndividualReturnViewModel(responseWithNo, obligations, returnsDateUtils)
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

      val viewModel = ViewIndividualReturnViewModel(responseWithYes, obligations, returnsDateUtils)
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

      val viewModel = ViewIndividualReturnViewModel(responseWithZero, obligations, returnsDateUtils)
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

      val viewModel = ViewIndividualReturnViewModel(responseWithNone, obligations, returnsDateUtils)
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

      val viewModel = ViewIndividualReturnViewModel(responseWithBothZero, obligations, returnsDateUtils)
      val result = viewModel.dutySuspenseSummaryList

      result mustBe defined
      result.get.rows(1).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.nothingToDeclare"))
      result.get.rows(2).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.nothingToDeclare"))
    }
  }
}
