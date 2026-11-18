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
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.ObligationDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.adjustments.DeclareAdjustmentPage
import pages.returns.{DeclareDutyPage, DeclareDutySuspensePage, DeclareSpoiltProductsPage}

import scala.concurrent.Future

class YourDetailsServiceSpec extends SpecBase with MockitoSugar {

  private val dutyAndSuspenseCompleted = returnsUserAnswers
    .set(DeclareDutyPage, false).success.value
    .set(DeclareDutySuspensePage, false).success.value

  private val notEligible = Seq(openObligation(periodKey))
  private val eligible = Seq(openObligation(periodKey), fulfilledObligation(PeriodKey("26KE")))

  private def serviceWith(obligations: Seq[ObligationDetails]): YourDetailsService = {
    val mockObligationService = mock[ObligationService]
    when(mockObligationService.getObligations(any[VpdId]())(using any()))
      .thenReturn(Future.successful(obligations))
    new YourDetailsService(mockObligationService)
  }

  "YourDetailsService.allTasksCompleted" - {

    "must return true when all tasks are completed" in {
      serviceWith(notEligible).allTasksCompleted(dutyAndSuspenseCompleted, vpdId).futureValue mustBe true
    }

    "must return false when a task is not yet completed" in {
      serviceWith(notEligible).allTasksCompleted(returnsUserAnswers, vpdId).futureValue mustBe false
    }

    "must return false when adjustments are eligible and the adjustment tasks are not completed" in {
      serviceWith(eligible).allTasksCompleted(dutyAndSuspenseCompleted, vpdId).futureValue mustBe false
    }

    "must return true when adjustments are eligible and every task is completed" in {
      val allCompleted = dutyAndSuspenseCompleted
        .set(DeclareSpoiltProductsPage, false).success.value
        .set(DeclareAdjustmentPage, false).success.value

      serviceWith(eligible).allTasksCompleted(allCompleted, vpdId).futureValue mustBe true
    }
  }
}
