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
import models.obligations.ObligationDetails
import models.requests.returns.ReturnsDataRequest
import models.returns.DutySuspenseVolumes
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.adjustments.DeclareAdjustmentPage
import pages.returns.yourdetails.{YourBusinessEmailPage, YourFullNamePage, YourRolePage}
import pages.returns.{DeclareDutyPage, DeclareDutySuspensePage, DeclareSpoiltProductsPage, EnterDutyAmountPage, EnterDutySuspensePage}
import play.api.http.Status.BAD_GATEWAY
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.UpstreamErrorResponse
import viewmodels.returns.submit.DeclarationViewModel

import scala.concurrent.Future

class DeclarationServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val JUNE = "June"
  private val YEAR_2026 = "2026"
  private val FAILURE_MESSAGE = "boom"

  private val mockSubmitReturnService: SubmitReturnService = mock[SubmitReturnService]
  private val mockCachedObligationsService: CachedObligationsService = mock[CachedObligationsService]

  private val service = new DeclarationService(
    mockObligationService,
    mockCachedObligationsService,
    mockReturnsUserAnswersService,
    mockSubmitReturnService,
    returnsDateUtils
  )

  private val openObligations: Seq[ObligationDetails] = Seq(openObligation(periodKey))

  private given request: ReturnsDataRequest[AnyContentAsEmpty.type] =
    ReturnsDataRequest(FakeRequest(), vpdId, groupId, internalId, credId, periodKey, returnsUserAnswers)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockObligationService, mockCachedObligationsService, mockReturnsUserAnswersService, mockSubmitReturnService)
  }

  private def stubLiveObligations(): Unit = {
    when(mockObligationService.getObligations(eqTo(vpdId))(using any()))
      .thenReturn(Future.successful(openObligations))
    ()
  }

  private def stubCachedObligations(): Unit = {
    when(mockCachedObligationsService.getObligations(eqTo(returnsUserAnswers), eqTo(vpdId))(using any()))
      .thenReturn(Future.successful(openObligations))
    ()
  }

  private def stubSaveObligations(result: Future[Unit]): Unit = {
    when(mockCachedObligationsService.saveObligations(any(), any())(using any())).thenReturn(result)
    ()
  }

  "DeclarationService" - {

    "buildViewModel" - {

      "must return the view model for the period" in {
        stubLiveObligations()
        stubSaveObligations(Future.successful(()))

        val completedAnswers = returnsUserAnswers
          .set(DeclareDutyPage, true)
          .flatMap(_.set(EnterDutyAmountPage, BigDecimal(1000)))
          .flatMap(_.set(DeclareDutySuspensePage, true))
          .flatMap(_.set(EnterDutySuspensePage, DutySuspenseVolumes(100, 50)))
          .flatMap(_.set(DeclareSpoiltProductsPage, false))
          .flatMap(_.set(DeclareAdjustmentPage, false))
          .flatMap(_.set(YourFullNamePage, "John Smith"))
          .flatMap(_.set(YourRolePage, "Director"))
          .flatMap(_.set(YourBusinessEmailPage, "john@example.com"))
          .success
          .value

        service.buildViewModel(vpdId, periodKey, completedAnswers).futureValue mustBe Some(DeclarationViewModel(periodKey, JUNE, YEAR_2026))
      }

      "must return None when answers are not completed for the period" in {
        stubLiveObligations()
        stubSaveObligations(Future.successful(()))

        val incompleteAnswers = returnsUserAnswers
        
        service.buildViewModel(vpdId, periodKey, incompleteAnswers).futureValue mustBe None
      }

      "must save the live obligations in the user answers" in {
        stubLiveObligations()
        stubSaveObligations(Future.successful(()))

        service.buildViewModel(vpdId, periodKey, returnsUserAnswers).futureValue

        verify(mockCachedObligationsService).saveObligations(eqTo(openObligations), eqTo(returnsUserAnswers))(using any())
      }

      "must fail when saving the obligations fails" in {
        stubLiveObligations()
        stubSaveObligations(Future.failed(new RuntimeException(FAILURE_MESSAGE)))

        service.buildViewModel(vpdId, periodKey, returnsUserAnswers).failed.futureValue mustBe a[RuntimeException]
      }
    }

    "submit" - {

      "must submit the return with the obligations from the user answers, clear the answers and return Right" in {
        stubCachedObligations()
        when(mockSubmitReturnService.submit(any(), any())(using any()))
          .thenReturn(Future.successful(testReturnSubmissionResponse))
        when(mockReturnsUserAnswersService.clear(any(), any())(any()))
          .thenReturn(Future.successful(Right(())))

        service.submit(vpdId, periodKey, returnsUserAnswers).futureValue mustBe Right(())

        verify(mockSubmitReturnService).submit(eqTo(returnsUserAnswers), eqTo(openObligations))(using any())
        verify(mockReturnsUserAnswersService).clear(eqTo(vpdId), eqTo(periodKey))(any())
      }

      "must return Left and not clear the answers when the submission fails" in {
        stubCachedObligations()
        when(mockSubmitReturnService.submit(any(), any())(using any()))
          .thenReturn(Future.failed(new IllegalStateException(FAILURE_MESSAGE)))

        service.submit(vpdId, periodKey, returnsUserAnswers).futureValue.isLeft mustBe true

        verify(mockReturnsUserAnswersService, never()).clear(any(), any())(any())
      }

      "must return Left and not submit when getting the obligations fails" in {
        when(mockCachedObligationsService.getObligations(any(), any())(using any()))
          .thenReturn(Future.failed(UpstreamErrorResponse(FAILURE_MESSAGE, BAD_GATEWAY)))

        service.submit(vpdId, periodKey, returnsUserAnswers).futureValue.isLeft mustBe true

        verify(mockSubmitReturnService, never()).submit(any(), any())(using any())
        verify(mockReturnsUserAnswersService, never()).clear(any(), any())(any())
      }
    }
  }
}
