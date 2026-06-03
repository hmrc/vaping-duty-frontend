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

package controllers.returns.submit

import base.SpecBase
import models.obligations.{ObligationDetails, ObligationItem, ObligationStatus, ObligationsResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.ObligationService
import uk.gov.hmrc.http.InternalServerException
import viewmodels.returns.submit.SelectSpoiltPeriodViewModel
import views.html.returns.submit.SelectSpoiltPeriodView

import java.time.LocalDate
import scala.concurrent.Future

class SelectSpoiltPeriodControllerSpec extends SpecBase {

  private def createMultiYearObligationsResponse(): ObligationsResponse = {
    val currentDate = LocalDate.now()

    createMockObligationsResponse().copy(
      obligation = createMockObligationsResponse().obligation ++ Seq(
        ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = ObligationStatus.F.toString,
            iCFromDate = LocalDate.of(2027, 11, 1),
            iCToDate = LocalDate.of(2027, 11, 30),
            iCDateReceived = Some(currentDate),
            iCDueDate = currentDate.plusDays(10),
            periodKey = "27AK"
          )
        ),
        ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = ObligationStatus.F.toString,
            iCFromDate = LocalDate.of(2026, 10, 1),
            iCToDate = LocalDate.of(2026, 10, 31),
            iCDateReceived = Some(currentDate.minusMonths(1)),
            iCDueDate = currentDate.minusMonths(1),
            periodKey = "26AJ"
          )
        ),
        ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = ObligationStatus.F.toString,
            iCFromDate = LocalDate.of(2025, 9, 1),
            iCToDate = LocalDate.of(2025, 9, 30),
            iCDateReceived = Some(currentDate.minusMonths(2)),
            iCDueDate = currentDate.minusMonths(2),
            periodKey = "25AI"
          )
        )
      )
    )
  }

  "SelectSpoiltPeriodController" - {

    "must return OK and the correct view when no year parameter is provided" in {
      val mockService = mock[ObligationService]
      val obligationsResponse = createMultiYearObligationsResponse()

      when(mockService.getObligations(any())(using any())).thenReturn(Future.successful(obligationsResponse))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].to(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.SelectSpoiltPeriodController.onPageLoad(None).url)

        val result = route(application, request).value

        val vm = SelectSpoiltPeriodViewModel(obligationsResponse, None, periodKey)(messages(application))
        val view = application.injector.instanceOf[SelectSpoiltPeriodView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view when a specific year is provided" in {
      val mockService = mock[ObligationService]
      val obligationsResponse = createMultiYearObligationsResponse()
      val specificYear = 2026

      when(mockService.getObligations(any())(using any())).thenReturn(Future.successful(obligationsResponse))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].to(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.SelectSpoiltPeriodController.onPageLoad(Some(specificYear)).url)

        val result = route(application, request).value

        val vm = SelectSpoiltPeriodViewModel(obligationsResponse, Some(specificYear), periodKey)(messages(application))
        val view = application.injector.instanceOf[SelectSpoiltPeriodView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }

    "must redirect to JourneyRecovery when the service fails" in {
      val mockService = mock[ObligationService]

      when(mockService.getObligations(any())(using any())).thenReturn(Future.failed(InternalServerException("")))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[ObligationService].to(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.SelectSpoiltPeriodController.onPageLoad(None).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery when returns journey is disabled" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers), returnsEnabled = false).build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.routes.SelectSpoiltPeriodController.onPageLoad(None).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}