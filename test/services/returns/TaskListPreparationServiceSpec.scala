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
import models.returns.{AdjustmentsEligibility, ReturnsUserAnswers}
import org.apache.pekko.http.scaladsl.model.HttpResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.DeclareSpoiltProductsPage
import play.api.http.Status.OK

import scala.concurrent.Future

class TaskListPreparationServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val mockRepository = mock[ReturnsUserAnswersService]
  private val service = new TaskListPreparationService(mockRepository)

  private val emptyAnswers = ReturnsUserAnswers(
    vpdId = "test-id",
    periodKey = periodKey.value,
    startedTime = clock.instant(),
    lastUpdated = clock.instant()
  )

  override def beforeEach(): Unit = reset(mockRepository)

  "prepareUserAnswers" - {

    "when user is not eligible for adjustments" - {

      "must set DeclareSpoiltProductsPage to false when not answered" in {
        when(mockRepository.set(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK))))

        val result = service.prepareUserAnswers(emptyAnswers, AdjustmentsEligibility.NotEligible).futureValue

        result.get(DeclareSpoiltProductsPage) mustBe Some(false)
        verify(mockRepository).set(any())(any())
      }

      "must set DeclareSpoiltProductsPage to false when answered true" in {
        val answersWithTrue = emptyAnswers.set(DeclareSpoiltProductsPage, true).success.value
        when(mockRepository.set(any())(any())).thenReturn(Future.successful(Right(HttpResponse(OK))))

        val result = service.prepareUserAnswers(answersWithTrue, AdjustmentsEligibility.NotEligible).futureValue

        result.get(DeclareSpoiltProductsPage) mustBe Some(false)
        verify(mockRepository).set(any())(any())
      }

      "must not update DeclareSpoiltProductsPage when already false" in {
        val answersWithFalse = emptyAnswers.set(DeclareSpoiltProductsPage, false).success.value

        val result = service.prepareUserAnswers(answersWithFalse, AdjustmentsEligibility.NotEligible).futureValue

        result.get(DeclareSpoiltProductsPage) mustBe Some(false)
        result mustBe answersWithFalse
      }
    }

    "when user is eligible for adjustments" - {

      "must return answers unchanged when not answered" in {
        val result = service.prepareUserAnswers(emptyAnswers, AdjustmentsEligibility.Eligible).futureValue

        result mustBe emptyAnswers
        result.get(DeclareSpoiltProductsPage) mustBe None
      }

      "must return answers unchanged when answered true" in {
        val answersWithTrue = emptyAnswers.set(DeclareSpoiltProductsPage, true).success.value

        val result = service.prepareUserAnswers(answersWithTrue, AdjustmentsEligibility.Eligible).futureValue

        result mustBe answersWithTrue
        result.get(DeclareSpoiltProductsPage) mustBe Some(true)
      }

      "must return answers unchanged when answered false" in {
        val answersWithFalse = emptyAnswers.set(DeclareSpoiltProductsPage, false).success.value

        val result = service.prepareUserAnswers(answersWithFalse, AdjustmentsEligibility.Eligible).futureValue

        result mustBe answersWithFalse
        result.get(DeclareSpoiltProductsPage) mustBe Some(false)
      }
    }
  }
}
