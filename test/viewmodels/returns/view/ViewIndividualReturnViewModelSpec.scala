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
import models.returns.DeclarationDetails
import models.returns.view.*

import java.time.{Instant, LocalDate}

class ViewIndividualReturnViewModelSpec extends SpecBase with TestData {
  
  private val obligationDetails = fulfilledObligation(periodKey)

  private val obligationDetailsJuly = fulfilledObligation(PeriodKey("24AG"))

  private val obligations = ObligationsResponse(
    obligation = Seq(
      ObligationItem(None, obligationDetails),
      ObligationItem(None, obligationDetailsJuly)
    )
  )

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
        val viewModel = ViewIndividualReturnViewModel(returnDisplayResponse, obligations, returnsDateUtils)

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
        val viewModel = ViewIndividualReturnViewModel(returnDisplayResponse, obligations, returnsDateUtils)

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
        val viewModel = ViewIndividualReturnViewModel(returnDisplayResponse, obligations, returnsDateUtils)

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
        val viewModel = ViewIndividualReturnViewModel(returnDisplayResponse, obligations, returnsDateUtils)

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
}
