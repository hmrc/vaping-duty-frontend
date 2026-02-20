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

package models.contactPreference

import base.SpecBase
import connectors.SubmitPreferencesConnector
import data.TestData
import models.emailverification.ErrorModel
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.AuditService

import scala.concurrent.Future

class PerformSubmissionSpec extends AnyFreeSpec with Matchers with TestData with SpecBase {

  implicit val request: DataRequest[?] = DataRequest(FakeRequest(), vpdId, userId, credId, emptyUserAnswers)

  val mockConnector: SubmitPreferencesConnector = mock[SubmitPreferencesConnector]
  val mockAuditService: AuditService = mock[AuditService]

  "PerformSubmission must" - {

    "redirect to confirmation page when submission is successful" in {

      when(mockConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Right(testSubmissionResponse)))

      val result = PerformSubmission(mockConnector, contactPreferenceSubmissionEmail, mockAuditService).getResult

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.contactPreference.routes.ConfirmationController.onPageLoad().url
   }

    "redirect to journey recovery when submission response is an error" in {

      when(mockConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, "There was a problem"))))

      val result = PerformSubmission(mockConnector, contactPreferenceSubmissionNewEmail, mockAuditService).getResult

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
    }
  }
}
