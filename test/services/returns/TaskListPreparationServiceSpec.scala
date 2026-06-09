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
import org.mockito.Mockito.{reset, times, verify, when}
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
        verify(mockRepository, times(0)).set(any())(any())
      }
    }

    "when user is eligible for adjustments" - {

      "must return answers unchanged when not answered" in {
        val result = service.prepareUserAnswers(emptyAnswers, AdjustmentsEligibility.Eligible).futureValue

        result mustBe emptyAnswers
        result.get(DeclareSpoiltProductsPage) mustBe None
        verify(mockRepository, times(0)).set(any())(any())
      }

      "must return answers unchanged when answered true" in {
        val answersWithTrue = emptyAnswers.set(DeclareSpoiltProductsPage, true).success.value

        val result = service.prepareUserAnswers(answersWithTrue, AdjustmentsEligibility.Eligible).futureValue

        result mustBe answersWithTrue
        result.get(DeclareSpoiltProductsPage) mustBe Some(true)
        verify(mockRepository, times(0)).set(any())(any())
      }

      "must return answers unchanged when answered false" in {
        val answersWithFalse = emptyAnswers.set(DeclareSpoiltProductsPage, false).success.value

        val result = service.prepareUserAnswers(answersWithFalse, AdjustmentsEligibility.Eligible).futureValue

        result mustBe answersWithFalse
        result.get(DeclareSpoiltProductsPage) mustBe Some(false)
        verify(mockRepository, times(0)).set(any())(any())
      }
    }
  }

  "updateUserAnswers" - {

    "when it is the user's first return and they are not eligible for adjustments" - {

      "must set DeclareSpoiltProductsPage to false when not answered" in {
        val result = TaskListPreparationService.updateUserAnswers(emptyAnswers, AdjustmentsEligibility.NotEligible)

        result.get(DeclareSpoiltProductsPage) mustBe Some(false)
      }

      "must set DeclareSpoiltProductsPage to false when answered true" in {
        val answersWithTrue = emptyAnswers.set(DeclareSpoiltProductsPage, true).success.value

        val result = TaskListPreparationService.updateUserAnswers(answersWithTrue, AdjustmentsEligibility.NotEligible)

        result.get(DeclareSpoiltProductsPage) mustBe Some(false)
      }

      "must not update DeclareSpoiltProductsPage when already false" in {
        val answersWithFalse = emptyAnswers.set(DeclareSpoiltProductsPage, false).success.value

        val result = TaskListPreparationService.updateUserAnswers(answersWithFalse, AdjustmentsEligibility.NotEligible)

        result mustBe answersWithFalse
      }
    }

    "when it is not the user's first return and they are eligible for adjustments" - {

      "must return answers unchanged when not answered" in {
        val result = TaskListPreparationService.updateUserAnswers(emptyAnswers, AdjustmentsEligibility.Eligible)

        result mustBe emptyAnswers
      }

      "must return answers unchanged when answered true" in {
        val answersWithTrue = emptyAnswers.set(DeclareSpoiltProductsPage, true).success.value

        val result = TaskListPreparationService.updateUserAnswers(answersWithTrue, AdjustmentsEligibility.Eligible)

        result mustBe answersWithTrue
      }

      "must return answers unchanged when answered false" in {
        val answersWithFalse = emptyAnswers.set(DeclareSpoiltProductsPage, false).success.value

        val result = TaskListPreparationService.updateUserAnswers(answersWithFalse, AdjustmentsEligibility.Eligible)

        result mustBe answersWithFalse
      }
    }
  }

}
