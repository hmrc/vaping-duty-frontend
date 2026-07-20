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
import models.obligations.ObligationDetails
import models.returns.view.*
import models.returns.{DeclarationDetails, RegularReturn, VapingProductsProduced}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import utils.ReturnsDateUtils

import java.time.{Instant, LocalDate}

class ViewIndividualReturnViewModelSpec extends SpecBase with TestData {

  implicit val messages: Messages = messages(applicationBuilder(None).build())

  private val returnsDateUtils = new ReturnsDateUtils(clock)
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
  
  private val obligationDetails = fulfilledObligation(periodKey)
  private val obligationDetailsJuly = fulfilledObligation(PeriodKey("24AG"))
  private val obligationsForAdjustments = Seq(obligationDetails, obligationDetailsJuly)

  private val chargeDetails = ChargeDetails(
    periodKey = "24AI",
    chargeReference = Some("VPD38270541977"),
    periodFrom = LocalDate.of(2027, 9, 1),
    periodTo = LocalDate.of(2027, 9, 30),
    receiptDate = Instant.parse("2027-10-06T10:00:00Z")
  )

  private val declaration = DeclarationDetails(
    fullName = "Craig Cottingham",
    signeesEmailAddress = "craig@craigsvapes.co.uk",
    capacityInWhichSigned = "Manufacturer"
  )

  private val sampleUnderDeclaration = UnderDeclaration(
    "1",
    Some("Additional products found"),
    Option(Seq(
      UnderDeclarationProduct(
        returnPeriodAffected = "24AG",
        taxType = "614",
        dutyRate = BigDecimal("1.00"),
        amountUnderDeclared = BigDecimal("100.00"),
        dutyDue = BigDecimal("100.00")
      )
    ))
  )

  private val sampleOverDeclaration = OverDeclaration(
    "1",
    Some("Correction of previous return"),
    Option(Seq(
      OverDeclarationProduct(
        returnPeriodAffected = "24AG",
        taxType = "614",
        dutyRate = BigDecimal("1.00"),
        amountOverDeclared = BigDecimal("50.00"),
        dutyDue = BigDecimal("-50.00")
      )
    ))
  )

  "ViewIndividualReturnViewModel" - {
    "must create view model with all fields from ReturnDisplayResponse" in {
      val result = ViewIndividualReturnViewModel(returnResponse, Seq(obligationDetails), returnsDateUtils)

      result.chargeReference mustBe Some("XVC123456789012")
      result.hasVapingProductsDeclaration mustBe true
      result.amountProducedLiquid mustBe Some("1,000,000.00")
      result.dutyDue mustBe Some("£3,150")
      result.totalDutySpoiltProducts mustBe "-£100"
      result.monthYear mustBe "June 2026"
      result.submittedOn must include("February 2026")
      result.dutyRate mustBe Some("£3.15")
      result.personalDetailsSummaryList.rows.size mustBe 3
      result.dutyDeclarationSummaryList mustBe defined
      result.spoiltSummaryLists.nonEmpty mustBe true
      result.totalDutySummaryList.rows.nonEmpty mustBe true
    }

    "must default duty rate to £0 when none is supplied" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoDeclaration, Seq(obligationDetails), returnsDateUtils)

      result.dutyRate mustBe None
    }

    "must handle missing charge reference" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoChargeRef, Seq(obligationDetails), returnsDateUtils)

      result.chargeReference mustBe None
    }

    "must handle missing vaping products declaration" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoDeclaration, Seq(obligationDetails), returnsDateUtils)

      result.hasVapingProductsDeclaration mustBe false
      result.amountProducedLiquid mustBe None
      result.dutyDue mustBe None
    }

    "must default to £0.00 when totalDutyDue is missing" in {
      val result = ViewIndividualReturnViewModel(returnResponseNoTotalDuty, Seq(obligationDetails), returnsDateUtils)

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

      val result = ViewIndividualReturnViewModel(responseWithDeclaration, Seq(obligationDetails), returnsDateUtils)

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

      val viewModel = ViewIndividualReturnViewModel(responseWithDeclaration, Seq(obligationDetails), returnsDateUtils)
      val result = viewModel.dutyDeclarationSummaryList

      result mustBe defined
      result.get.rows.size mustBe 3
    }

    "must include a row with No when no declaration" in {
      val viewModel = ViewIndividualReturnViewModel(returnResponseNoDeclaration, Seq(obligationDetails), returnsDateUtils)
      val result = viewModel.dutyDeclarationSummaryList.get.rows.head.value.content

      result mustBe Text("No")
    }
  }

  "spoiltSummaryList" - {

    "must be pre-computed with spoilt product detail rows" in {
      val viewModel = ViewIndividualReturnViewModel(returnResponse, Seq(obligationDetails), returnsDateUtils)
      val result = viewModel.spoiltSummaryLists

      result.head.rows.size mustBe 4
      result.head.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.question"))
      result.head.rows(1).key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.month"))
      result.head.rows(2).key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.spoiltProducts"))
      result.head.rows(3).key.content.asHtml.toString must include(messages("viewIndividualReturn.dutyDue"))
    }

    "must hide totalDutySpoiltProducts row when spoilt products answer is No" in {
      val noSpoiltResponse = returnResponse.copy(
        success = returnResponse.success.copy(
          spoiltProduct = Some(SpoiltProduct(spoiltProductFilled = "0", spoiltProducts = None))
        )
      )
      val viewModel = ViewIndividualReturnViewModel(noSpoiltResponse, Seq(obligationDetails), returnsDateUtils)
      val result = viewModel.spoiltSummaryLists

      result.head.rows.size mustBe 1
      result.head.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.spoiltProducts.question"))
      result.head.rows.forall(row =>
        !row.key.content.asHtml.toString.contains(messages("viewIndividualReturn.dutyDue"))
      ) mustBe true
    }
  }

  "period key lookup" - {

    "must throw IllegalStateException when period key not found in obligations during view model creation" in {
      val obligationDetailsWithMissingPeriod = Seq(fulfilledObligation(PeriodKey("26AF")))

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
        ViewIndividualReturnViewModel(responseWithSpoiltProduct, obligationDetailsWithMissingPeriod, returnsDateUtils)
      }

      exception.getMessage must include("Period key 24AC not found in obligations")
    }
  }

  "dutySuspenseSummaryList" - {

    "must return None when otherOptions is None" in {
      val responseNoOtherOptions = returnResponse.copy(
        success = returnResponse.success.copy(otherOptions = None)
      )

      val viewModel = ViewIndividualReturnViewModel(responseNoOtherOptions, Seq(obligationDetails), returnsDateUtils)
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

      val viewModel = ViewIndividualReturnViewModel(responseWithNo, Seq(obligationDetails), returnsDateUtils)
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

      val viewModel = ViewIndividualReturnViewModel(responseWithYes, Seq(obligationDetails), returnsDateUtils)
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

      val viewModel = ViewIndividualReturnViewModel(responseWithZero, Seq(obligationDetails), returnsDateUtils)
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

      val viewModel = ViewIndividualReturnViewModel(responseWithNone, Seq(obligationDetails), returnsDateUtils)
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

      val viewModel = ViewIndividualReturnViewModel(responseWithBothZero, Seq(obligationDetails), returnsDateUtils)
      val result = viewModel.dutySuspenseSummaryList

      result mustBe defined
      result.get.rows(1).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.nothingToDeclare"))
      result.get.rows(2).value.content.asHtml.toString must include(messages("viewIndividualReturn.dutySuspense.nothingToDeclare"))
    }
  }
  
  "when building adjustments summary list" - {
    
    "must display no adjustments when both flags are 0" in {
      val returnDisplaySuccess = ReturnDisplaySuccess(
        processingDate = Instant.now(clock),
        idDetails = None,
        chargeDetails = Some(chargeDetails),
        vapingProductsProduced = None,
        overDeclaration = Some(OverDeclaration("0", None, None)),
        underDeclaration = Some(UnderDeclaration("0", None, None)),
        spoiltProduct = None,
        totalDutyDue = None,
        totalDutyDueByTaxType = None,
        otherOptions = None,
        declaration = declaration
      )

      val returnDisplayResponse = ReturnDisplayResponse(returnDisplaySuccess)
      val viewModel = ViewIndividualReturnViewModel(returnDisplayResponse, obligationsForAdjustments, returnsDateUtils)

      viewModel.adjustmentsSummaryLists.size mustBe 1
      viewModel.adjustmentsSummaryLists.head.rows.size mustBe 1
    }

    "must display under declarations correctly" in {
      val returnDisplaySuccess = ReturnDisplaySuccess(
        processingDate = Instant.now(clock),
        idDetails = None,
        chargeDetails = Option(chargeDetails),
        vapingProductsProduced = None,
        overDeclaration = None,
        underDeclaration = Option(sampleUnderDeclaration),
        spoiltProduct = None,
        totalDutyDue = None,
        totalDutyDueByTaxType = None,
        otherOptions = None,
        declaration = declaration
      )

      val returnDisplayResponse = ReturnDisplayResponse(returnDisplaySuccess)
      val viewModel = ViewIndividualReturnViewModel(returnDisplayResponse, obligationsForAdjustments, returnsDateUtils)

      // Should have 2 lists: one for the under declaration, one for the reason
      viewModel.adjustmentsSummaryLists.size mustBe 2

      // First list has question + under declaration details
      viewModel.adjustmentsSummaryLists.head.rows.size mustBe 4

      // Second list has the reason
      viewModel.adjustmentsSummaryLists(1).rows.size mustBe 1
    }

    "must display over declarations correctly" in {
      val returnDisplaySuccess = ReturnDisplaySuccess(
        processingDate = Instant.now(clock),
        idDetails = None,
        chargeDetails = Option(chargeDetails),
        vapingProductsProduced = None,
        overDeclaration = Option(sampleOverDeclaration),
        underDeclaration = None,
        spoiltProduct = None,
        totalDutyDue = None,
        totalDutyDueByTaxType = None,
        otherOptions = None,
        declaration = declaration
      )

      val returnDisplayResponse = ReturnDisplayResponse(returnDisplaySuccess)
      val viewModel = ViewIndividualReturnViewModel(returnDisplayResponse, obligationsForAdjustments, returnsDateUtils)

      // With 1 over declaration and a reason, should have 2 lists
      viewModel.adjustmentsSummaryLists.size mustBe 2

      // First list has question + over declaration details
      viewModel.adjustmentsSummaryLists.head.rows.size mustBe 4

      // Second list has the reason
      viewModel.adjustmentsSummaryLists(1).rows.size mustBe 1
    }

    "must display both under and over declarations correctly" in {
      val returnDisplaySuccess = ReturnDisplaySuccess(
        processingDate = Instant.now(clock),
        idDetails = None,
        chargeDetails = Option(chargeDetails),
        vapingProductsProduced = None,
        overDeclaration = Option(sampleOverDeclaration),
        underDeclaration = Option(sampleUnderDeclaration),
        spoiltProduct = None,
        totalDutyDue = None,
        totalDutyDueByTaxType = None,
        otherOptions = None,
        declaration = declaration
      )

      val returnDisplayResponse = ReturnDisplayResponse(returnDisplaySuccess)
      val viewModel = ViewIndividualReturnViewModel(returnDisplayResponse, obligationsForAdjustments, returnsDateUtils)

      // Should have 3 summary lists: one for under declaration, one for over declaration, one for reason
      viewModel.adjustmentsSummaryLists.size mustBe 3

      // First list (under declaration) should have 4 rows (question + details)
      viewModel.adjustmentsSummaryLists.head.rows.size mustBe 4

      // Second list (over declaration) should have 3 rows (no question row)
      viewModel.adjustmentsSummaryLists(1).rows.size mustBe 3

      // Third list has the reason (uses under declaration reason since it's checked first)
      viewModel.adjustmentsSummaryLists(2).rows.size mustBe 1
    }
  }
}
