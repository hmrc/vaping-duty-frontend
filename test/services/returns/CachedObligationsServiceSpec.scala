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
import models.emailverification.ErrorModel
import models.identifiers.VpdId
import models.obligations.ObligationDetails
import models.returns.ReturnsUserAnswers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_GATEWAY, INTERNAL_SERVER_ERROR, OK}
import queries.returns.ObligationsQuery
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future

class CachedObligationsServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private val SAVE_ERROR_MESSAGE = "failed"
  private val FAILURE_MESSAGE = "boom"

  private val service = new CachedObligationsService(mockObligationService, mockReturnsUserAnswersService)

  private val openObligations: Seq[ObligationDetails] = Seq(openObligation(periodKey))
  private val staleObligations: Seq[ObligationDetails] = Seq(fulfilledObligation(periodKey))

  private val savedAnswers: ReturnsUserAnswers = returnsUserAnswers.set(ObligationsQuery, openObligations).success.value

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockObligationService, mockReturnsUserAnswersService)
  }

  private def stubSet(result: Future[Either[ErrorModel, HttpResponse]]): Unit = {
    when(mockReturnsUserAnswersService.set(any())(any())).thenReturn(result)
    ()
  }

  private def capturedSetAnswers(): ReturnsUserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[ReturnsUserAnswers])
    verify(mockReturnsUserAnswersService).set(captor.capture())(any())
    captor.getValue
  }

  "CachedObligationsService" - {

    "getObligations" - {

      "must return the obligations saved in the user answers without calling the Obligations API" in {
        service.getObligations(savedAnswers, vpdId).futureValue mustBe openObligations

        verify(mockObligationService, never()).getObligations(any[VpdId]())(using any())
      }

      "must call the Obligations API when no obligations are saved in the user answers" in {
        when(mockObligationService.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.successful(openObligations))

        service.getObligations(returnsUserAnswers, vpdId).futureValue mustBe openObligations

        verify(mockObligationService).getObligations(eqTo(vpdId))(using any())
      }

      "must fail when no obligations are saved and the Obligations API call fails" in {
        when(mockObligationService.getObligations(eqTo(vpdId))(using any()))
          .thenReturn(Future.failed(UpstreamErrorResponse(FAILURE_MESSAGE, BAD_GATEWAY)))

        service.getObligations(returnsUserAnswers, vpdId).failed.futureValue mustBe an[UpstreamErrorResponse]
      }
    }

    "saveObligations" - {

      "must save the obligations in the user answers" in {
        stubSet(Future.successful(Right(HttpResponse(OK))))

        service.saveObligations(openObligations, returnsUserAnswers).futureValue mustBe ()

        capturedSetAnswers().get(ObligationsQuery).value mustBe openObligations
      }

      "must replace obligations already saved in the user answers" in {
        stubSet(Future.successful(Right(HttpResponse(OK))))
        val staleAnswers = returnsUserAnswers.set(ObligationsQuery, staleObligations).success.value

        service.saveObligations(openObligations, staleAnswers).futureValue mustBe ()

        capturedSetAnswers().get(ObligationsQuery).value mustBe openObligations
      }

      "must succeed when saving the user answers returns an error" in {
        stubSet(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, SAVE_ERROR_MESSAGE))))

        service.saveObligations(openObligations, returnsUserAnswers).futureValue mustBe ()
      }

      "must fail when saving the user answers fails" in {
        stubSet(Future.failed(new RuntimeException(FAILURE_MESSAGE)))

        service.saveObligations(openObligations, returnsUserAnswers).failed.futureValue mustBe a[RuntimeException]
      }
    }
  }
}
