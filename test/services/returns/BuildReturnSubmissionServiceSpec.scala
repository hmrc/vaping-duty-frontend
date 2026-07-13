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

package services.returns

import base.SpecBase
import builders.ObligationsBuilders
import config.FrontendAppConfig
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.ObligationDetails
import models.returns.*
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import models.returns.submit.ReturnCreateRequest
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.*
import pages.returns.adjustments.{AdjustmentListPage, AdjustmentReasonPage}

import java.time.LocalDate

class BuildReturnSubmissionServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ObligationsBuilders {

  private val mockTotalDutyDueCalculationService = TotalDutyDueCalculationService()
  private val mockConfig = mock[FrontendAppConfig]

  private val service = new BuildReturnSubmissionService(
    mockTotalDutyDueCalculationService,
    mockConfig
  )

  private val vpdId = VpdId("GBWK1234567WK")
  private val periodKey = PeriodKey("24KA")
  private val taxType = "641"

  private val obligation = ObligationDetails(
    openOrFulfilledStatus = "O",
    iCFromDate = LocalDate.of(2024, 1, 1),
    iCToDate = LocalDate.of(2024, 1, 31),
    iCDateReceived = None,
    iCDueDate = LocalDate.of(2024, 2, 28),
    periodKey = periodKey.value
  )

  private val declaration = DeclarationDetails(
    fullName = "John Smith",
    capacityInWhichSigned = "Director",
    signeesEmailAddress = "john.smith@example.com"
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockConfig.taxType).thenReturn(taxType)
  }

  private val yes = "1"
  private val no = "0"

  "SubmitReturnService" - {

    "submit must" - {

      "successfully build a return submission with duty declared" in {
        val userAnswers = nilReturnUserAnswers(vpdId, periodKey)
          .set(DeclareDutyPage, true).success.value
          .set(EnterDutyAmountPage, BigDecimal("1000")).success.value
          .set(DeclarationPage, declaration).success.value

        val returnCreateRequest = service.buildSubmission(userAnswers, obligation, vpdId, Map(PeriodKey(obligation.periodKey) -> 1050))
        
        returnCreateRequest.vapingProductsProduced.vapingProdManufactured mustBe yes
        returnCreateRequest.vapingProductsProduced.returns.size mustBe 1
        returnCreateRequest.vapingProductsProduced.returns.head mustBe RegularReturn("641", 105.0, 1, 10500)

        returnCreateRequest.spoiltProduct.get.spoiltProductFilled mustBe no
        returnCreateRequest.underDeclaration.get.underDeclFilled mustBe no
        returnCreateRequest.overDeclaration.get.overDeclFilled mustBe no
        
        returnCreateRequest.otherOptions.get.vapingProductUnderDutySuspense mustBe no

        returnCreateRequest.periodKey mustBe periodKey.value
        returnCreateRequest.declaration.fullName mustBe "John Smith"
        returnCreateRequest.declaration.capacityInWhichSigned mustBe "Director"
        returnCreateRequest.declaration.signeesEmailAddress mustBe "john.smith@example.com"

        returnCreateRequest.totalDutyDue.totalDutyDueVapingProducts mustBe 10500
        returnCreateRequest.totalDutyDue.totalDutySpoiltProduct mustBe 0
        returnCreateRequest.totalDutyDue.totalDutyUnderDeclaration mustBe 0
        returnCreateRequest.totalDutyDue.totalDutyOverDeclaration mustBe 0
        returnCreateRequest.totalDutyDue.totalDue mustBe 10500
      }

      "successfully build a return submission for a nil return" in {
        val userAnswers = nilReturnUserAnswers(vpdId, periodKey)
          .set(DeclareDutyPage, false).success.value
          .set(DeclarationPage, declaration).success.value


        val returnCreateRequest = service.buildSubmission(userAnswers, obligation, vpdId, Map(PeriodKey(obligation.periodKey) -> 1050))

        returnCreateRequest.vapingProductsProduced.vapingProdManufactured mustBe no
        returnCreateRequest.vapingProductsProduced.returns.size mustBe 0

        returnCreateRequest.spoiltProduct.get.spoiltProductFilled mustBe no
        returnCreateRequest.underDeclaration.get.underDeclFilled mustBe no
        returnCreateRequest.overDeclaration.get.overDeclFilled mustBe no

        returnCreateRequest.otherOptions.get.vapingProductUnderDutySuspense mustBe no

        returnCreateRequest.periodKey mustBe periodKey.value
        returnCreateRequest.declaration.fullName mustBe "John Smith"
        returnCreateRequest.declaration.capacityInWhichSigned mustBe "Director"
        returnCreateRequest.declaration.signeesEmailAddress mustBe "john.smith@example.com"

        returnCreateRequest.totalDutyDue.totalDutyDueVapingProducts mustBe 0
        returnCreateRequest.totalDutyDue.totalDutySpoiltProduct mustBe 0
        returnCreateRequest.totalDutyDue.totalDutyUnderDeclaration mustBe 0
        returnCreateRequest.totalDutyDue.totalDutyOverDeclaration mustBe 0
        returnCreateRequest.totalDutyDue.totalDue mustBe 0
      }

      "successfully build a return submission with spoilt products" in {
        val spoiltVolumes = List(
          SpoiltVolumeByPeriod(volume = 500, periodKey = november2026),
          SpoiltVolumeByPeriod(volume = 300, periodKey = october2026)
        )

        val userAnswers = nilReturnUserAnswers(vpdId, december2026)
          .set(DeclareDutyPage, true).success.value
          .set(EnterDutyAmountPage, BigDecimal("1000")).success.value
          .set(SpoiltVolumeByPeriodPage, spoiltVolumes).success.value
          .set(DeclarationPage, declaration).success.value

        val returnCreateRequest = service.buildSubmission(userAnswers, openObligation(december2026), vpdId, Map(october2026 -> 1050, november2026 -> 1050, december2026 -> 1050))

        returnCreateRequest.vapingProductsProduced.vapingProdManufactured mustBe yes
        returnCreateRequest.vapingProductsProduced.returns.size mustBe 1
        returnCreateRequest.vapingProductsProduced.returns.head mustBe RegularReturn("641", 105.0, 1, 10500)

        returnCreateRequest.spoiltProduct.get.spoiltProductFilled mustBe yes
        returnCreateRequest.underDeclaration.get.underDeclFilled mustBe no
        returnCreateRequest.overDeclaration.get.overDeclFilled mustBe no

        returnCreateRequest.otherOptions.get.vapingProductUnderDutySuspense mustBe no

        returnCreateRequest.periodKey mustBe december2026.value
        returnCreateRequest.declaration.fullName mustBe "John Smith"
        returnCreateRequest.declaration.capacityInWhichSigned mustBe "Director"
        returnCreateRequest.declaration.signeesEmailAddress mustBe "john.smith@example.com"

        returnCreateRequest.totalDutyDue.totalDutyDueVapingProducts mustBe 10500
        returnCreateRequest.totalDutyDue.totalDutySpoiltProduct mustBe 8400
        returnCreateRequest.totalDutyDue.totalDutyUnderDeclaration mustBe 0
        returnCreateRequest.totalDutyDue.totalDutyOverDeclaration mustBe 0
        returnCreateRequest.totalDutyDue.totalDue mustBe 2100
      }

      "successfully build a return submission with duty suspense movements" in {
        val dutySuspenseVolumes = DutySuspenseVolumes(volumeReceived = 1000, volumeMoved = 500)

        val userAnswers = nilReturnUserAnswers(vpdId, periodKey)
          .set(DeclareDutyPage, true).success.value
          .set(EnterDutyAmountPage, BigDecimal("1000")).success.value
          .set(DeclareDutySuspensePage, true).success.value
          .set(EnterDutySuspensePage, dutySuspenseVolumes).success.value
          .set(DeclarationPage, declaration).success.value

        val returnCreateRequest = service.buildSubmission(userAnswers, obligation, vpdId, Map(PeriodKey(obligation.periodKey) -> 1050))

        returnCreateRequest.vapingProductsProduced.vapingProdManufactured mustBe yes
        returnCreateRequest.vapingProductsProduced.returns.size mustBe 1
        returnCreateRequest.vapingProductsProduced.returns.head mustBe RegularReturn("641", 105.0, 1, 10500)

        returnCreateRequest.spoiltProduct.get.spoiltProductFilled mustBe no
        returnCreateRequest.underDeclaration.get.underDeclFilled mustBe no
        returnCreateRequest.overDeclaration.get.overDeclFilled mustBe no

        returnCreateRequest.otherOptions.get.vapingProductUnderDutySuspense mustBe yes
        returnCreateRequest.otherOptions.get.volumeMovedToDutySuspense mustBe Some(0.5)
        returnCreateRequest.otherOptions.get.volumeMovedFromDutySuspense mustBe Some(1.0)

        returnCreateRequest.periodKey mustBe periodKey.value
        returnCreateRequest.declaration.fullName mustBe "John Smith"
        returnCreateRequest.declaration.capacityInWhichSigned mustBe "Director"
        returnCreateRequest.declaration.signeesEmailAddress mustBe "john.smith@example.com"

        returnCreateRequest.totalDutyDue.totalDutyDueVapingProducts mustBe 10500
        returnCreateRequest.totalDutyDue.totalDutySpoiltProduct mustBe 0
        returnCreateRequest.totalDutyDue.totalDutyUnderDeclaration mustBe 0
        returnCreateRequest.totalDutyDue.totalDutyOverDeclaration mustBe 0
        returnCreateRequest.totalDutyDue.totalDue mustBe 10500
      }

      "must only save reasonForUnderDecl when only the under-declared duty total meets the £1000 threshold" in {
        val adjustmentList = AdjustmentList(Seq(
          AdjustmentEntry(periodKey, AdjustmentType.UnderDeclared, BigDecimal("1000")),
          AdjustmentEntry(periodKey, AdjustmentType.OverDeclared, BigDecimal("500"))
        ))

        val userAnswers = nilReturnUserAnswers(vpdId, periodKey)
          .set(AdjustmentListPage, adjustmentList).success.value
          .set(AdjustmentReasonPage, "a reason").success.value
          .set(DeclarationPage, declaration).success.value

        val returnCreateRequest = service.buildSubmission(userAnswers, obligation, vpdId, Map(periodKey -> 100))

        returnCreateRequest.underDeclaration.get.underDeclFilled mustBe yes
        returnCreateRequest.underDeclaration.get.reasonForUnderDecl mustBe Some("a reason")
        returnCreateRequest.overDeclaration.get.overDeclFilled mustBe yes
        returnCreateRequest.overDeclaration.get.reasonForOverDecl mustBe None
      }

      "must save both reasons when both under-declared and over-declared duty totals meet the £1000 threshold" in {
        val adjustmentList = AdjustmentList(Seq(
          AdjustmentEntry(periodKey, AdjustmentType.UnderDeclared, BigDecimal("1000")),
          AdjustmentEntry(periodKey, AdjustmentType.OverDeclared, BigDecimal("1000"))
        ))

        val userAnswers = nilReturnUserAnswers(vpdId, periodKey)
          .set(AdjustmentListPage, adjustmentList).success.value
          .set(AdjustmentReasonPage, "a reason").success.value
          .set(DeclarationPage, declaration).success.value

        val returnCreateRequest = service.buildSubmission(userAnswers, obligation, vpdId, Map(periodKey -> 100))

        returnCreateRequest.underDeclaration.get.reasonForUnderDecl mustBe Some("a reason")
        returnCreateRequest.overDeclaration.get.reasonForOverDecl mustBe Some("a reason")
      }

      "must save neither reason when neither type's duty total meets the £1000 threshold, even though both are filled" in {
        val adjustmentList = AdjustmentList(Seq(
          AdjustmentEntry(periodKey, AdjustmentType.UnderDeclared, BigDecimal("500")),
          AdjustmentEntry(periodKey, AdjustmentType.OverDeclared, BigDecimal("500"))
        ))

        val userAnswers = nilReturnUserAnswers(vpdId, periodKey)
          .set(AdjustmentListPage, adjustmentList).success.value
          .set(AdjustmentReasonPage, "a reason").success.value
          .set(DeclarationPage, declaration).success.value

        val returnCreateRequest = service.buildSubmission(userAnswers, obligation, vpdId, Map(periodKey -> 100))

        returnCreateRequest.underDeclaration.get.underDeclFilled mustBe yes
        returnCreateRequest.underDeclaration.get.reasonForUnderDecl mustBe None
        returnCreateRequest.overDeclaration.get.overDeclFilled mustBe yes
        returnCreateRequest.overDeclaration.get.reasonForOverDecl mustBe None
      }

      "fail to build a return submission when declaration details are missing" in {
        val userAnswers = nilReturnUserAnswers(vpdId, periodKey)
          .set(DeclareDutyPage, true).success.value
          .set(EnterDutyAmountPage, BigDecimal("1000")).success.value

        val exception = intercept[IllegalStateException] {
          service.buildSubmission(userAnswers, obligation, vpdId, Map(PeriodKey(obligation.periodKey) -> 1050))
        }

        exception.getMessage must include("Declaration details are required")
      }
    }
  }

  private def nilReturnUserAnswers(vpdId: VpdId, periodKey: PeriodKey) = {
    ReturnsUserAnswers.getEmptyReturnsUA(vpdId, periodKey)
      .set(DeclareDutyPage          , false).success.value
      .set(DeclareSpoiltProductsPage, false).success.value
      .set(DeclareDutySuspensePage  , false).success.value
  }
}
