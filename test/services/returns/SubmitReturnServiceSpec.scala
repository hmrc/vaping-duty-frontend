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
import connectors.returns.SubmitReturnConnector
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.ObligationDetails
import models.requests.returns.ReturnsDataRequest
import models.returns.{DeclarationDetails, DutySuspenseVolumes, ReturnsUserAnswers, SpoiltVolumeByPeriod}
import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import models.returns.view.{OtherOptions, OverDeclaration, SpoiltProduct, UnderDeclaration}
import models.returns.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.*
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
  private val mockBuildReturnSubmissionService: BuildReturnSubmissionService = mock[BuildReturnSubmissionService]
  private val mockAuditService = mock[AuditService]

  private val service = new SubmitReturnService(
    mockSubmitReturnConnector,
    mockDutyRateService,
    mockObligationService,
    mockBuildReturnSubmissionService,
    mockAuditService
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
    when(mockDutyRateService.getDutyRatesInPencePerMlForPeriodKeys(Seq(obligation)))
      .thenReturn(Map(PeriodKey(obligation.periodKey) -> dutyRateInPence))
    reset(mockAuditService)
    reset(mockSubmitReturnConnector)
  }

  private val yes = "1"

  private val nilTotalDutyDue = TotalDutyDue(0.0, 0.0, 0.0, 0.0, 0.0)

  private val nilReturnCreatedRequest = ReturnCreateRequest(
    periodKey.value,
    VapingProductsProduced("0", Seq()),
    Some(UnderDeclaration("0", None, Some(Seq()))),
    Some(OverDeclaration("0", None, Some(Seq()))),
    Some(SpoiltProduct("0", Some(Seq()))),
    nilTotalDutyDue,
    Some(OtherOptions("0", None, None)),
    declaration
  )

  private val nonNilReturnCreatedRequest = nilReturnCreatedRequest.copy(
    vapingProductsProduced = VapingProductsProduced(yes, Seq(RegularReturn(taxType, dutyRate = 2.20, amountProducedLiquid = 1, dutyDue = 220.00))),
    totalDutyDue = nilTotalDutyDue.copy(totalDutyDueVapingProducts = 220.00, totalDue = 220.00)
  )

  "SubmitReturnService" - {

    "submit must" - {

      "successfully submit a return and send an audit event" in {
        val userAnswers = ReturnsUserAnswers.getEmptyReturnsUA(vpdId, periodKey)
          .set(DeclareDutyPage, true).success.value
          .set(EnterDutyAmountPage, BigDecimal("1000")).success.value
          .set(DeclarationPage, declaration).success.value

        given ReturnsDataRequest[AnyContentAsEmpty.type] = buildReturnsDataRequest(userAnswers, periodKey)

        stubManufacturersObligations(vpdId, Seq(obligation))
        when(mockBuildReturnSubmissionService.buildSubmission(userAnswers, obligation, vpdId, Map(PeriodKey(obligation.periodKey) -> dutyRateInPence)))
          .thenReturn(nonNilReturnCreatedRequest)
        when(mockSubmitReturnConnector.submitReturn(any(), eqTo(vpdId))(any()))
          .thenReturn(Future.successful(submittedResponse))

        val result = service.submit(userAnswers).futureValue

        result mustBe submittedResponse
        verify(mockSubmitReturnConnector).submitReturn(eqTo(nonNilReturnCreatedRequest), eqTo(vpdId))(any())
        verify(mockAuditService).auditReturnSubmitted(any[JsObject])(any())
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
