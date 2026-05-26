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

package viewmodels.returns

import base.SpecBase
import models.identifiers.VpdId
import models.obligations.ObligationDetails
import models.returns.ReturnsUserAnswers
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.{DeclareDutyPage, EnterDutyAmountPage}
import play.api.libs.json.JsObject
import services.returns.{DutyRateService, ObligationService}
import viewmodels.returns.submit.CheckYourAnswersViewModelProvider

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class CheckYourAnswersViewModelSpec extends SpecBase with MockitoSugar {

  private val mockDutyRateService = mock[DutyRateService]
  private val mockObligationService = mock[ObligationService]

  private val provider = new CheckYourAnswersViewModelProvider(
    mockDutyRateService,
    mockObligationService
  )

  private val testObligation = createMockObligationsResponse().obligation.head.obligationDetails

  "CheckYourAnswersViewModelProvider" - {

    "must create a view model with both summary lists" in {
      val ua = ReturnsUserAnswers("id", periodKey, JsObject.empty, Instant.now(), Instant.now())
        .set(DeclareDutyPage, true).success.value

      when(mockObligationService.getObligationByPeriodKey(eqTo(VpdId("id")), eqTo(periodKey))(using any()))
        .thenReturn(Future.successful(Some(testObligation)))

      when(mockDutyRateService.getRateForDate(any[LocalDate]))
        .thenReturn(220) // 220 pence = £2.20 per ml

      whenReady(provider(ua)) { vm =>
        vm.finalDutySummaryList.rows must not be empty
        vm.dutySuspendedSummaryList.rows must not be empty
        
        verify(mockObligationService).getObligationByPeriodKey(eqTo(VpdId("id")), eqTo(periodKey))(using any())
        verify(mockDutyRateService).getRateForDate(eqTo(testObligation.iCFromDate))
      }
    }

    "must create a view model with empty summary lists when no data exists" in {
      val ua = ReturnsUserAnswers("id", periodKey, JsObject.empty, Instant.now(), Instant.now())

      when(mockObligationService.getObligationByPeriodKey(eqTo(VpdId("id")), eqTo(periodKey))(using any()))
        .thenReturn(Future.successful(Some(testObligation)))

      when(mockDutyRateService.getRateForDate(any[LocalDate]))
        .thenReturn(220)

      whenReady(provider(ua)) { vm =>
        // Will always show two rows at least
        vm.finalDutySummaryList.rows.size mustBe 2
        vm.dutySuspendedSummaryList.rows.size mustBe 3
      }
    }

    "must calculate duty correctly with configurable rate" in {
      val ua = ReturnsUserAnswers("id", periodKey, JsObject.empty, Instant.now(), Instant.now())
        .set(DeclareDutyPage, true)
        .flatMap(_.set(EnterDutyAmountPage, 1000)).success.value

      when(mockObligationService.getObligationByPeriodKey(eqTo(VpdId("id")), eqTo(periodKey))(using any()))
        .thenReturn(Future.successful(Some(testObligation)))

      when(mockDutyRateService.getRateForDate(any[LocalDate]))
        .thenReturn(220) // 220 pence = £2.20 per ml

      whenReady(provider(ua)) { vm =>
        // 1000 ml * £2.20 = £2,200
        vm.dutyDue mustBe "£2,200"
        vm.dutyRate mustBe "£2.20"
        vm.finalDutySummaryList.rows.size mustBe 2
      }
    }

    "must calculate duty correctly with different rate" in {
      val ua = ReturnsUserAnswers("id", periodKey, JsObject.empty, Instant.now(), Instant.now())
        .set(EnterDutyAmountPage, 500).success.value

      when(mockObligationService.getObligationByPeriodKey(eqTo(VpdId("id")), eqTo(periodKey))(using any()))
        .thenReturn(Future.successful(Some(testObligation)))

      when(mockDutyRateService.getRateForDate(any[LocalDate]))
        .thenReturn(250) // 250 pence = £2.50 per ml

      whenReady(provider(ua)) { vm =>
        // 500 ml * £2.50 = £1,250
        vm.dutyDue mustBe "£1,250"
        vm.dutyRate mustBe "£2.50"
      }
    }

    "must return £0 when no duty amount is entered" in {
      val ua = ReturnsUserAnswers("id", periodKey, JsObject.empty, Instant.now(), Instant.now())

      when(mockObligationService.getObligationByPeriodKey(eqTo(VpdId("id")), eqTo(periodKey))(using any()))
        .thenReturn(Future.successful(Some(testObligation)))

      when(mockDutyRateService.getRateForDate(any[LocalDate]))
        .thenReturn(220)

      whenReady(provider(ua)) { vm =>
        vm.dutyDue mustBe "£0"
        vm.dutyRate mustBe "£2.20"
      }
    }
    
  }
}
