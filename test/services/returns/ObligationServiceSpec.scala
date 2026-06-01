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
import connectors.returns.ObligationsConnector
import models.obligations.{ObligationDetails, ObligationItem, ObligationStatus, ObligationsResponse}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import java.time.LocalDate
import scala.concurrent.Future

class ObligationServiceSpec extends SpecBase with MockitoSugar {

  private val mockObligationsConnector = mock[ObligationsConnector]
  private val mockDutyRateService = mock[DutyRateService]
  
  private val obligation1 = ObligationDetails(
    openOrFulfilledStatus = ObligationStatus.O.toString,
    iCFromDate = LocalDate.of(2026, 1, 1),
    iCToDate = LocalDate.of(2026, 1, 31),
    iCDateReceived = None,
    iCDueDate = LocalDate.of(2026, 2, 28),
    periodKey = "26AA"
  )
  
  private val obligation2 = ObligationDetails(
    openOrFulfilledStatus = ObligationStatus.O.toString,
    iCFromDate = LocalDate.of(2026, 2, 1),
    iCToDate = LocalDate.of(2026, 2, 28),
    iCDateReceived = None,
    iCDueDate = LocalDate.of(2026, 3, 31),
    periodKey = "26AB"
  )
  
  private val obligation3 = ObligationDetails(
    openOrFulfilledStatus = ObligationStatus.F.toString,
    iCFromDate = LocalDate.of(2026, 3, 1),
    iCToDate = LocalDate.of(2026, 3, 31),
    iCDateReceived = Some(LocalDate.of(2026, 4, 15)),
    iCDueDate = LocalDate.of(2026, 4, 30),
    periodKey = "26AC"
  )

  private val mockObligationsResponse = ObligationsResponse(
    obligation = Seq(
      ObligationItem(identification = None, obligationDetails = obligation1),
      ObligationItem(identification = None, obligationDetails = obligation2),
      ObligationItem(identification = None, obligationDetails = obligation3)
    )
  )

  "ObligationService" - {

    "getObligations" - {
      "must return the obligations response" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(mockObligationsResponse))
        
        val service = new ObligationService(mockObligationsConnector, mockDutyRateService)
        
        val result = service.getObligations(vpdId).futureValue
        
        result mustBe mockObligationsResponse
      }
    }

    "getObligationByPeriodKey" - {

      "must return the correct obligation when periodKey exists" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(mockObligationsResponse))
        
        val service = new ObligationService(mockObligationsConnector, mockDutyRateService)
        
        val result = service.getObligationByPeriodKey(vpdId, "26AB").futureValue
        
        result mustBe Some(obligation2)
      }

      "must return None when periodKey does not exist" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(mockObligationsResponse))
        
        val service = new ObligationService(mockObligationsConnector, mockDutyRateService)
        
        val result = service.getObligationByPeriodKey(vpdId, "26XX").futureValue
        
        result mustBe None
      }

      "must return the first matching obligation when multiple have the same periodKey" in {
        val duplicateObligation = obligation1.copy(iCFromDate = LocalDate.of(2026, 1, 15))
        val responseWithDuplicates = ObligationsResponse(
          obligation = Seq(
            ObligationItem(identification = None, obligationDetails = obligation1),
            ObligationItem(identification = None, obligationDetails = duplicateObligation)
          )
        )
        
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(responseWithDuplicates))
        
        val service = new ObligationService(mockObligationsConnector, mockDutyRateService)
        
        val result = service.getObligationByPeriodKey(vpdId, "26AA").futureValue
        
        result mustBe Some(obligation1)
      }

      "must return None when obligations response is empty" in {
        val emptyResponse = ObligationsResponse(obligation = Seq.empty)
        
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(emptyResponse))
        
        val service = new ObligationService(mockObligationsConnector, mockDutyRateService)
        
        val result = service.getObligationByPeriodKey(vpdId, "26AA").futureValue
        
        result mustBe None
      }
    }

    "getDutyRateForPeriod" - {
      "must return the calculated duty rate when obligation exists" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(mockObligationsResponse))
        when(mockDutyRateService.getRateForDate(eqTo(obligation1.iCFromDate)))
          .thenReturn(315)
        
        val service = new ObligationService(mockObligationsConnector, mockDutyRateService)
        
        val result = service.getDutyRateForPeriod(vpdId, "26AA").futureValue
        
        result mustBe Some(BigDecimal("3.15"))
      }

      "must return None when the obligation does not exist" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(mockObligationsResponse))
        
        val service = new ObligationService(mockObligationsConnector, mockDutyRateService)
        
        val result = service.getDutyRateForPeriod(vpdId, "26XX").futureValue
        
        result mustBe None
      }

      "must return None when obligations response is empty" in {
        val noObligations = ObligationsResponse(obligation = Seq.empty)
        
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(noObligations))
        
        val service = new ObligationService(mockObligationsConnector, mockDutyRateService)
        
        val result = service.getDutyRateForPeriod(vpdId, "26AA").futureValue
        
        result mustBe None
      }
    }
  }
}