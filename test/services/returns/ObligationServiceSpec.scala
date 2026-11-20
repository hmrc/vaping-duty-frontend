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
import models.obligations.Obligation
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import queries.returns.ObligationsQuery

import scala.concurrent.Future

class ObligationServiceSpec extends SpecBase with MockitoSugar {

  private val mockObligationsConnector: ObligationsConnector = mock[ObligationsConnector]

  private val obligations: Seq[Obligation] = Seq(
    openObligation(january2026),
    openObligation(february2026),
    fulfilledObligation(march2026)
  )

  private val service = new ObligationService(mockObligationsConnector)

  "ObligationService" - {

    "getObligations with user answers" - {

      "must return the obligations saved in the user answers without calling the Obligations API" in {
        reset(mockObligationsConnector)

        val cachedAnswers = returnsUserAnswers.set(ObligationsQuery, obligations).success.value

        service.getObligations(cachedAnswers, vpdId).futureValue mustBe obligations
        verify(mockObligationsConnector, never()).getObligations(any())(using any())
      }

      "must call the Obligations API when no obligations are saved in the user answers" in {
        reset(mockObligationsConnector)
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(obligations))

        service.getObligations(returnsUserAnswers, vpdId).futureValue mustBe obligations
        verify(mockObligationsConnector).getObligations(any())(using any())
      }
    }

    "getObligations" - {
      "must return the obligations from the Obligations API" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(obligations))

        service.getObligations(vpdId).futureValue mustBe obligations
      }
    }

    "getObligationByPeriodKey" - {

      "must return the correct obligation when periodKey exists" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(obligations))

        service.getObligationByPeriodKey(vpdId, PeriodKey("26KB")).futureValue mustBe Some(openObligation(february2026))
      }

      "must return None when periodKey does not exist" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(obligations))

        service.getObligationByPeriodKey(vpdId, PeriodKey("26XX")).futureValue mustBe None
      }

      "must return None when there are no obligations" in {
        when(mockObligationsConnector.getObligations(any())(using any()))
          .thenReturn(Future.successful(Seq.empty))

        service.getObligationByPeriodKey(vpdId, periodKey).futureValue mustBe None
      }
    }
  }
}
