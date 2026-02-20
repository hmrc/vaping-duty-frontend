/*
 * Copyright 2024 HM Revenue & Customs
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
import models.contactPreference.PaperlessPreference.*
import models.audit.*
import models.audit.Actions.ChangeToPost
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.verify
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import java.time.Instant

class AuditServiceSpec extends SpecBase {

  private val mockAuditConnector = mock[AuditConnector]
  private val auditService       = new AuditService(mockAuditConnector)

  "AuditService" - {

    "send JourneyOutcome correctly" in {
      val testDetail = JourneyOutcome(
        timeStarted = Instant.now().toString,
        credentialId = credId,
        vpdId = vpdId,
        originalContactPreference = Email.toString,
        originalContactPreferenceValue = emailAddress,
        contactPreferenceChange = ChangeToPost.toString,
        contactPreferenceInput = Some(ContactPreferenceInput(
          userAnswersPostWithEmail.emailAddress.getOrElse(""),
          userAnswersPostWithEmail.subscriptionSummary.correspondenceAddress.replace("\n", ", ")
        ))
      )

      auditService.audit(testDetail)

      verify(mockAuditConnector)
        .sendExplicitAudit(eqTo(AuditType.ContactPreference.toString), eqTo(Json.toJson(testDetail)))(any(), any(), any())
    }
  }
}
