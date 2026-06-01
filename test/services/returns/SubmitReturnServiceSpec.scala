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
import config.FrontendAppConfig
import connectors.returns.SubmitReturnConnector
import models.obligations.ObligationDetails
import data.TestData
import models.requests.returns.ReturnsDataRequest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate
import scala.concurrent.Future

class SubmitReturnServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockConnector = mock[SubmitReturnConnector]
  private val mockDutyRateService = mock[DutyRateService]
  private val mockObligationService = mock[ObligationService]
  private val mockAppConfig = mock[FrontendAppConfig]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector, mockDutyRateService, mockObligationService, mockAppConfig)
  }

  private val testObligation = ObligationDetails(
    openOrFulfilledStatus = "O",
    iCFromDate = LocalDate.of(2026, 1, 1),
    iCToDate = LocalDate.of(2026, 1, 31),
    iCDateReceived = None,
    iCDueDate = LocalDate.of(2026, 2, 28),
    periodKey = periodKey
  )

  given ReturnsDataRequest[?] = ReturnsDataRequest(
    FakeRequest(),
    vpdId,
    internalId,
    credId,
    periodKey,
    returnsUserAnswers
  )

  "SubmitReturnService" - {

    "submit" - {

      "must return success when a return is submitted successfully" in {
        when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(periodKey))(using any()))
          .thenReturn(Future.successful(Some(testObligation)))

        when(mockDutyRateService.getRateForDate(eqTo(testObligation.iCFromDate)))
          .thenReturn(22)

        when(mockAppConfig.taxType).thenReturn("VPD")

        when(mockConnector.submitReturn(any(), eqTo(vpdId))(any()))
          .thenReturn(Future.successful(testReturnSubmissionResponse))

  val mockConnector: SubmitReturnConnector = mock[SubmitReturnConnector]

  given ReturnsDataRequest[?] = ReturnsDataRequest(FakeRequest(), vpdId, internalId, credId, PeriodKey(periodKey), returnsUserAnswers)
        val service = new SubmitReturnService(
          mockConnector,
          mockDutyRateService,
          mockObligationService,
          mockAppConfig
        )

        val result = service.submit(returnsUserAnswers)

        whenReady(result) { response =>
          response mustBe testReturnSubmissionResponse
        }
      }

      "must use the correct duty rate from the obligation's start date" in {
        when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(periodKey))(using any()))
          .thenReturn(Future.successful(Some(testObligation)))

        when(mockDutyRateService.getRateForDate(eqTo(testObligation.iCFromDate)))
          .thenReturn(30)

        when(mockAppConfig.taxType).thenReturn("VPD")

        when(mockConnector.submitReturn(any(), eqTo(vpdId))(any()))
          .thenReturn(Future.successful(testReturnSubmissionResponse))

        val service = new SubmitReturnService(
          mockConnector,
          mockDutyRateService,
          mockObligationService,
          mockAppConfig
        )

        val result = service.submit(returnsUserAnswers)

        whenReady(result) { _ =>
          // Verify that the duty rate service was called with the correct date
          org.mockito.Mockito.verify(mockDutyRateService)
            .getRateForDate(eqTo(testObligation.iCFromDate))
        }
      }

      "must return failure when there was an error submitting a return" in {
        when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(periodKey))(using any()))
          .thenReturn(Future.successful(Some(testObligation)))

        when(mockDutyRateService.getRateForDate(eqTo(testObligation.iCFromDate)))
          .thenReturn(22)

        when(mockAppConfig.taxType).thenReturn("VPD")

        when(mockConnector.submitReturn(any(), eqTo(vpdId))(any()))
          .thenReturn(Future.failed(InternalServerException("error")))

        val service = new SubmitReturnService(
          mockConnector,
          mockDutyRateService,
          mockObligationService,
          mockAppConfig
        )

        val result = service.submit(returnsUserAnswers)

        whenReady(result.failed) { exception =>
          exception mustBe an[InternalServerException]
        }
      }

      "must return failure when obligation is not found" in {
        when(mockObligationService.getObligationByPeriodKey(eqTo(vpdId), eqTo(periodKey))(using any()))
          .thenReturn(Future.successful(None))

        val service = new SubmitReturnService(
          mockConnector,
          mockDutyRateService,
          mockObligationService,
          mockAppConfig
        )

        val result = service.submit(returnsUserAnswers)

        whenReady(result.failed) { exception =>
          exception mustBe a[IllegalStateException]
        }
      }
    }
  }
}