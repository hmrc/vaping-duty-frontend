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
import models.obligations.{ObligationDetails, ObligationStatus, ObligationsResponse, SingleObligation}
import models.returns.view.*
import models.returns.{ChargeDetails, DeclarationDetails, TotalDutyDue}
import play.api.i18n.Messages
import utils.ReturnsDateUtils

import java.time.{Instant, LocalDate}

class ViewIndividualReturnViewModelSpec extends SpecBase with TestData {

  private val returnsDateUtils = app.injector.instanceOf[ReturnsDateUtils]
  
  private val obligationDetails = ObligationDetails(
    obligationStatus = ObligationStatus.F,
    openOrFulfilledStatus = "F",
    iCFromDate = LocalDate.of(2027, 8, 1),
    iCToDate = LocalDate.of(2027, 8, 31),
    iCDueDate = LocalDate.of(2027, 10, 25),
    periodKey = "24AH"
  )

  private val obligationDetailsJuly = ObligationDetails(
    obligationStatus = ObligationStatus.F,
    openOrFulfilledStatus = "F",
    iCFromDate = LocalDate.of(2027, 7, 1),
    iCToDate = LocalDate.of(2027, 7, 31),
    iCDueDate = LocalDate.of(2027, 9, 25),
    periodKey = "24AG"
  )

  private val obligations = ObligationsResponse(
    obligation = Seq(
      SingleObligation(obligationDetails),
      SingleObligation(obligationDetailsJuly)
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

        viewModel.adjustmentsSummaryList.rows.size mustBe 1
        viewModel.adjustmentsSummaryList.rows.head.key.content.asHtml.body must include("Do you have any vaping products you need to over or under declare?")
        viewModel.adjustmentsSummaryList.rows.head.value.content.asHtml.body must include("No")
      }

      "must display under declarations correctly" in {
        val returnDisplaySuccess = ReturnDisplaySuccess(
          processingDate = Instant.now(clock),
          idDetails = None,
          chargeDetails = Some(chargeDetails),
          vapingProductsProduced = None,
          overDeclaration = None,
          underDeclaration = Some(sampleUnderDeclaration),
          spoiltProduct = None,
          totalDutyDue = None,
          totalDutyDueByTaxType = None,
          otherOptions = None,
          declaration = declaration
        )

        val returnDisplayResponse = ReturnDisplayResponse(returnDisplaySuccess)
        val viewModel = ViewIndividualReturnViewModel(returnDisplayResponse, obligations, returnsDateUtils)

        viewModel.adjustmentsSummaryList.rows.size mustBe 5
        viewModel.adjustmentsSummaryList.rows.head.value.content.asHtml.body must include("Yes")
        viewModel.adjustmentsSummaryList.rows(1).key.content.asHtml.body must include("Return period affected")
        viewModel.