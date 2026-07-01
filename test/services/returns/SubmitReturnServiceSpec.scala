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
import connectors.returns.SubmitReturnConnector
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.ObligationDetails
import models.requests.returns.ReturnsDataRequest
import models.returns.{DeclarationDetails, DutySuspenseVolumes, RegularReturn, ReturnsUserAnswers, SpoiltVolumeByPeriod}
import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.{DeclarationPage, DeclareDutyPage, DeclareDutySuspensePage, EnterDutyAmountPage, EnterDutySuspensePage, SpoiltVolumeByPeriodPage}
import play.api.libs.json.JsObject
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import services.contactPreference.AuditService

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class SubmitReturnServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with ObligationsBuilders {

  private val mockSubmitReturnConnector = mock[SubmitReturnConnector]
  private val mockDutyRateService = mock[DutyRateService]
  private val mockObligationService = mock[ObligationService]
  private val mockTotalDutyDueCalculationService = TotalDutyDueCalculationService()
  private val mockAuditService = mock[AuditService]
  private val mockConfig = mock[FrontendAppConfig]

  private val service = new SubmitReturnService(
    mockSubmitReturnConnector,
    mockDutyRateService,
    mockObligationService,
    mockTotalDutyDueCalculationService,
    mockAuditService,
    mockConfig
  )

  private val vpdId = VpdId("GBWK1234567WK")
  private val periodKey = PeriodKey("24KA")
  private val dutyRateInPence = 1050
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

  private val submittedResponse = ReturnSubmittedResponse(
    processingDate = Instant.parse("2026-05-08T10:30:00Z"),
    vpdReferenceNumber = vpdId.value,
    submissionId = Some("123456789012"),
    chargeReference = Some("AB123456789012"),
    amount = BigDecimal("100.00"),
    paymentDueDate = Some(LocalDate.parse("2026-06-07"))
  )

  val nilReturnTotals = models.returns.TotalDutyDue(
    totalDutyDueVapingProducts = BigDecimal("0"),
    totalDutyOverDeclaration   = BigDecimal("0"),
    totalDutyUnderDeclaration  = BigDecimal("0"),
    totalDutySpoiltProduct     = BigDecimal("0"),
    totalDue                   = BigDecimal("0")
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockConfig.taxType).thenReturn(taxType)
    when(mockDutyRateService.getDutyRatesInPencePerMlForPeriodKeys(Seq(obligation)))
      .thenReturn(Map(PeriodKey(obligation.periodKey) -> dutyRateInPence))
    reset(mockAuditService)
  }

  private val yes = "1"
  private val no = "0"

  "SubmitReturnService" - {

    "submit must" - {

      "successfully submit a return with duty declared" in {
        val userAnswers = ReturnsUserAnswers.getEmptyReturnsUA(vpdId, periodKey)
          .set(DeclareDutyPage, true).success.value
          .set(EnterDutyAmountPage, BigDecimal("1000")).success.value
          .set(DeclarationPage, declaration).success.value

        given ReturnsDataRequest[AnyContentAsEmpty.type] = buildReturnsDataRequest(userAnswers, periodKey)

        stubManufacturersObligations(vpdId, Seq(obligation))
        when(mockSubmitReturnConnector.submitReturn(any(), eqTo(vpdId))(any()))
          .thenReturn(Future.successful(submittedResponse))

        val result = service.submit(userAnswers).futureValue

        result mustBe submittedResponse
        verify(mockSubmitReturnConnector).submitReturn(any[ReturnCreateRequest], eqTo(vpdId))(any())
        verify(mockAuditService).auditReturnSubmitted(any[JsObject])(any())
      }

      "successfully build a return submission with duty declared" in {
        val userAnswers = ReturnsUserAnswers.getEmptyReturnsUA(vpdId, periodKey)
          .set(DeclareDutyPage, true).success.value
          .set(EnterDutyAmountPage, BigDecimal("1000")).success.value
          .set(DeclarationPage, declaration).success.value

        val returnCreateRequest = service.buildSubmission(userAnswers, obligation, vpdId, Map(PeriodKey(obligation.periodKey) -> 1050))
        
        returnCreateRequest.vapingProductsProduced.vapingProdManufactured mustBe yes
        returnCreateRequest.vapingProductsProduced.returns.size mustBe 1
        returnCreateRequest.vapingProductsProduced.returns.head mustBe RegularReturn("641", 105.0, 1, 10500)

//        returnCreateRequest.spoiltProduct.get.spoiltProductFilled mustBe no
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

      "successfully submit a nil return" in {
        val userAnswers = ReturnsUserAnswers.getEmptyReturnsUA(vpdId, periodKey)
          .set(DeclareDutyPage, false).success.value
          .set(DeclarationPage, declaration).success.value

        given ReturnsDataRequest[AnyContentAsEmpty.type] = buildReturnsDataRequest(userAnswers, periodKey)

        stubManufacturersObligations(vpdId, Seq(obligation))
        when(mockSubmitReturnConnector.submitReturn(any(), eqTo(vpdId))(any()))
          .thenReturn(Future.successful(submittedResponse))

        val result = service.submit(userAnswers).futureValue

        result mustBe submittedResponse
        verify(mockAuditService).auditReturnSubmitted(any[JsObject])(any())
      }

      "successfully build a return submission for a nil return" in {
        val userAnswers = ReturnsUserAnswers.getEmptyReturnsUA(vpdId, periodKey)
          .set(DeclareDutyPage, false).success.value
          .set(DeclarationPage, declaration).success.value


        val returnCreateRequest = service.buildSubmission(userAnswers, obligation, vpdId, Map(PeriodKey(obligation.periodKey) -> 1050))

        returnCreateRequest.vapingProductsProduced.vapingProdManufactured mustBe no
        returnCreateRequest.vapingProductsProduced.returns.size mustBe 0

        //        returnCreateRequest.spoiltProduct.get.spoiltProductFilled mustBe no
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

      "successfully submit a return with spoilt products" in {
        val spoiltVolumes = List(
          SpoiltVolumeByPeriod(volume = 500, periodKey = november2026),
          SpoiltVolumeByPeriod(volume = 300, periodKey = october2026)
        )

        val userAnswers = ReturnsUserAnswers.getEmptyReturnsUA(vpdId, december2026)
          .set(DeclareDutyPage, true).success.value
          .set(EnterDutyAmountPage, BigDecimal("1000")).success.value
          .set(SpoiltVolumeByPeriodPage, spoiltVolumes).success.value
          .set(DeclarationPage, declaration).success.value

        given ReturnsDataRequest[AnyContentAsEmpty.type] = buildReturnsDataRequest(userAnswers, december2026)

        stubManufacturersObligations(vpdId, Seq(fulfilledObligation(october2026), fulfilledObligation(november2026), openObligation(december2026)))
        when(mockDutyRateService.getDutyRatesInPencePerMlForPeriodKeys(Seq(fulfilledObligation(october2026), fulfilledObligation(november2026), openObligation(december2026))))
          .thenReturn(Map(october2026 -> 1050, november2026 -> 1050, december2026 -> 1050))
        when(mockSubmitReturnConnector.submitReturn(any(), eqTo(vpdId))(any()))
          .thenReturn(Future.successful(submittedResponse))

        val result = service.submit(userAnswers).futureValue

        result mustBe submittedResponse
        verify(mockAuditService).auditReturnSubmitted(any[JsObject])(any())
      }

      "successfully build a return submission with spoilt products" in {
        val spoiltVolumes = List(
          SpoiltVolumeByPeriod(volume = 500, periodKey = november2026),
          SpoiltVolumeByPeriod(volume = 300, periodKey = october2026)
        )

        val userAnswers = ReturnsUserAnswers.getEmptyReturnsUA(vpdId, december2026)
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

      "successfully submit a return with duty suspense" in {
        val dutySuspenseVolumes = DutySuspenseVolumes(volumeReceived = 1000, volumeMoved = 500)

        val userAnswers = ReturnsUserAnswers.getEmptyReturnsUA(vpdId, periodKey)
          .set(DeclareDutyPage, true).success.value
          .set(EnterDutyAmountPage, BigDecimal("1000")).success.value
          .set(DeclareDutySuspensePage, true).success.value
          .set(EnterDutySuspensePage, dutySuspenseVolumes).success.value
          .set(DeclarationPage, declaration).success.value

        given ReturnsDataRequest[AnyContentAsEmpty.type] = buildReturnsDataRequest(userAnswers, periodKey)

        stubManufacturersObligations(vpdId, Seq(obligation))
        when(mockSubmitReturnConnector.submitReturn(any(), eqTo(vpdId))(any()))
          .thenReturn(Future.successful(submittedResponse))

        val result = service.submit(userAnswers).futureValue

        result mustBe submittedResponse
        verify(mockAuditService).auditReturnSubmitted(any[JsObject])(any())
      }

      "successfully build a return submission with duty suspense movements" in {
        val dutySuspenseVolumes = DutySuspenseVolumes(volumeReceived = 1000, volumeMoved = 500)

        val userAnswers = ReturnsUserAnswers.getEmptyReturnsUA(vpdId, periodKey)
          .set(DeclareDutyPage, true).success.value
          .set(EnterDutyAmountPage, BigDecimal("1000")).success.value
          .set(DeclareDutySuspensePage, true).success.value
          .set(EnterDutySuspensePage, dutySuspenseVolumes).success.value
          .set(DeclarationPage, declaration).success.value

        val returnCreateRequest = service.buildSubmission(userAnswers, obligation, vpdId, Map(PeriodKey(obligation.periodKey) -> 1050))

        returnCreateRequest.vapingProductsProduced.vapingProdManufactured mustBe yes
        returnCreateRequest.vapingProductsProduced.returns.size mustBe 1
        returnCreateRequest.vapingProductsProduced.returns.head mustBe RegularReturn("641", 105.0, 1, 10500)

        //        returnCreateRequest.spoiltProduct.get.spoiltProductFilled mustBe no
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

      "fail when no obligation found" in {
        val userAnswers = ReturnsUserAnswers.getEmptyReturnsUA(vpdId, periodKey)
          .set(DeclarationPage, declaration).success.value

        given ReturnsDataRequest[AnyContentAsEmpty.type] = buildReturnsDataRequest(userAnswers, periodKey)

        when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(Seq()))

        val exception = service.submit(userAnswers).failed.futureValue

        exception mustBe an[IllegalStateException]
        exception.getMessage must include("No obligation found")
        verify(mockAuditService, never()).auditReturnSubmitted(any[JsObject])(any())
      }

      "fail when declaration details are missing" in {
        val userAnswers = ReturnsUserAnswers.getEmptyReturnsUA(vpdId, periodKey)
          .set(DeclareDutyPage, true).success.value
          .set(EnterDutyAmountPage, BigDecimal("1000")).success.value

        given ReturnsDataRequest[AnyContentAsEmpty.type] = buildReturnsDataRequest(userAnswers, periodKey)

        stubManufacturersObligations(vpdId, Seq(obligation))

        val exception = service.submit(userAnswers).failed.futureValue

        exception mustBe an[IllegalStateException]
        exception.getMessage must include("Declaration details are required")
        verify(mockAuditService, never()).auditReturnSubmitted(any[JsObject])(any())
      }

      "fail to build a return submission when declaration details are missing" in {
        val userAnswers = ReturnsUserAnswers.getEmptyReturnsUA(vpdId, periodKey)
          .set(DeclareDutyPage, true).success.value
          .set(EnterDutyAmountPage, BigDecimal("1000")).success.value

        val exception = intercept[IllegalStateException] {
          service.buildSubmission(userAnswers, obligation, vpdId, Map(PeriodKey(obligation.periodKey) -> 1050))
        }

        exception.getMessage must include("Declaration details are required")
        verify(mockAuditService, never()).auditReturnSubmitted(any[JsObject])(any())
      }
    }
  }

  private def stubManufacturersObligations(vpdId: VpdId, obligations: Seq[ObligationDetails]) = {
    when(mockObligationService.getObligationsDirectly(eqTo(vpdId))(using any()))
      .thenReturn(Future.successful(obligations))
  }

  private def buildReturnsDataRequest(answers: ReturnsUserAnswers, periodKey: PeriodKey) = {
    ReturnsDataRequest(FakeRequest(), vpdId, groupId, internalId, credId, periodKey, answers)
  }
}
