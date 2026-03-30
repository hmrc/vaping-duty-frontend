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

package services

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
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.test.FakeRequest

import scala.concurrent.Future

class PerformSubmissionServiceSpec extends AnyFreeSpec with Matchers with TestData with SpecBase {

  implicit val request: DataRequest[?] = DataRequest(FakeRequest(), vpdId, internalId, credId, emptyUserAnswers)

  val mockConnector: SubmitPreferencesConnector = mock[SubmitPreferencesConnector]
  val mockAuditService: AuditService = mock[AuditService]

  "PerformSubmission must" - {

    "return Success when a preference is submitted successfully" in {

      when(mockConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Right(testSubmissionResponse)))

      val result = PerformSubmissionService(mockConnector, mockAuditService).submit(contactPreferenceSubmissionEmail, request)

      whenReady(result) {
        _.isInstanceOf[Success] mustBe true
      }
   }

    "return Failure when there was an error submitting a preference" in {

      when(mockConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, "There was a problem"))))

      val result = PerformSubmissionService(mockConnector, mockAuditService).submit(contactPreferenceSubmissionNewEmail, request)

      whenReady(result) {
        _.isInstanceOf[Failure] mustBe true
      }
    }
  }
}
