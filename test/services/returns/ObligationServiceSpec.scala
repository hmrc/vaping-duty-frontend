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
import models.identifiers.PeriodKey
import models.obligations.{Identification, ObligationDetails, ObligationItem, ObligationsResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import queries.returns.ObligationsQuery

import scala.concurrent.Future

class ObligationServiceSpec extends SpecBase with MockitoSugar {

  private val mockObligationsConnector: ObligationsConnector = mock[ObligationsConnector]

  private val obligations: Seq[ObligationDetails] = Seq(
    openObligation(january2026),
    openObligation(february2026),
    fulfilledObligation(march2026)
  )

  private val mockObligationsResponse = ObligationsResponse(obligations(obligations))

  private val mockObligationsResponseWithNonMatchingId = mockObligationsResponse.copy(
    obligation = Seq(ObligationItem(
      identification = Some(Identification(referenceType = "ZVPD", referenceNumber = "NonMatchingId", incomeSourceType = None)),
      obligationDetails = Seq(fulfilledObligation(october2027)))
    ) ++ obligations(obligations)
  )

  private val mockObligationsResponseWithNonMatchingType = mockObligationsResponse.copy(
    obligation = Seq(ObligationItem(
      identification = Some(Identification(referenceType = "OTHER_REGIME", referenceNumber = vpdId.value, incomeSourceType = None)),
      obligationDetails = Seq(fulfilledObligation(october2027)))
    ) ++ obligations(obligations)
  )

  "ObligationService" - {

    when(mockAppConfig.enrolmentIdentifierKey).thenReturn("ZVPD")
    
    "getObligations" - {
      "must return the obligations response" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(mockObligationsResponse))

        val service = new ObligationService(mockObligationsConnector, mockAppConfig)

        val result = service.getObligations(vpdId).futureValue

        result mustBe obligations
      }

      "must filter out obligation items without matching referenceNumber" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(mockObligationsResponseWithNonMatchingId))

        val service = new ObligationService(mockObligationsConnector, mockAppConfig)

        val result = service.getObligations(vpdId).futureValue

        result mustBe obligations
      }

      "must filter out obligation items without matching referenceType" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(mockObligationsResponseWithNonMatchingType))

        val service = new ObligationService(mockObligationsConnector, mockAppConfig)

        val result = service.getObligations(vpdId).futureValue

        result mustBe obligations
      }
    }

    "getObligationByPeriodKey" - {

      "must return the correct obligation when periodKey exists" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(mockObligationsResponse))

        val service = new ObligationService(mockObligationsConnector, mockAppConfig)

        val result = service.getObligationByPeriodKey(vpdId, PeriodKey("26KB")).futureValue

        result mustBe Some(openObligation(february2026))
      }

      "must filter out obligation items without matching vpdids" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(mockObligationsResponseWithNonMatchingId))

        val service = new ObligationService(mockObligationsConnector, mockAppConfig)

        val result = service.getObligationByPeriodKey(vpdId, PeriodKey("26KB")).futureValue

        result mustBe Some(openObligation(february2026))
      }

      "must return None when periodKey does not exist" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(mockObligationsResponse))

        val service = new ObligationService(mockObligationsConnector, mockAppConfig)

        val result = service.getObligationByPeriodKey(vpdId, PeriodKey("26XX")).futureValue

        result mustBe None
      }

      "must return None when obligations response is empty" in {
        val emptyResponse = ObligationsResponse(obligation = Seq.empty)

        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(emptyResponse))

        val service = new ObligationService(mockObligationsConnector, mockAppConfig)

        val result = service.getObligationByPeriodKey(vpdId, periodKey).futureValue

        result mustBe None
      }
    }

    "getCachedObligations" - {

      "must return the cached obligations without calling the connector when they are in the user answers" in {
        val connector = mock[ObligationsConnector]
        val service = new ObligationService(connector, mockAppConfig)

        val cachedAnswers = returnsUserAnswers.set(ObligationsQuery, obligations).success.value

        val result = service.getCachedObligations(cachedAnswers, vpdId).futureValue

        result mustBe obligations
        verify(connector, never()).getObligations(any())(using any())
      }

      "must call the connector and return the filtered obligations when they are not in the user answers" in {
        val connector = mock[ObligationsConnector]
        when(connector.getObligations(any())(using any()))
          .thenReturn(Future.successful(mockObligationsResponseWithNonMatchingId))

        val service = new ObligationService(connector, mockAppConfig)

        val result = service.getCachedObligations(returnsUserAnswers, vpdId).futureValue

        result mustBe obligations
        verify(connector).getObligations(any())(using any())
      }

      "must return an empty cached list without calling the connector" in {
        val connector = mock[ObligationsConnector]
        val service = new ObligationService(connector, mockAppConfig)

        val cachedAnswers = returnsUserAnswers.set(ObligationsQuery, Seq.empty[ObligationDetails]).success.value

        val result = service.getCachedObligations(cachedAnswers, vpdId).futureValue

        result mustBe Seq.empty
        verify(connector, never()).getObligations(any())(using any())
      }
    }

    "withCachedObligations" - {

      "must store the obligations in the user answers" in {
        val service = new ObligationService(mockObligationsConnector, mockAppConfig)

        val result = service.withCachedObligations(returnsUserAnswers, obligations)

        result.get(ObligationsQuery) mustBe Some(obligations)
      }

      "must replace any previously cached obligations" in {
        val service = new ObligationService(mockObligationsConnector, mockAppConfig)

        val previous = returnsUserAnswers.set(ObligationsQuery, Seq(openObligation(january2026))).success.value

        val result = service.withCachedObligations(previous, obligations)

        result.get(ObligationsQuery) mustBe Some(obligations)
      }
    }
  }
}
