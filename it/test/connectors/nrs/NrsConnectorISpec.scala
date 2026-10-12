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

package connectors.nrs

import base.ISpecBase
import models.nrs.{IdentityData, NrsMetadata, NrsPayload, NrsSubmissionResult}
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE}
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.{ConfidenceLevel, CredentialStrength}
import uk.gov.hmrc.http.HeaderCarrier
import utils.ConnectorTestHelpers

import java.time.Instant

class NrsConnectorISpec extends ISpecBase with ConnectorTestHelpers {

  protected val endpointName = "nrs-orchestrator"

  "NrsConnector must" - {
    "return Success when NRS accepts the submission" in new SetUp {
      val nrsPayload: NrsPayload = createTestPayload()

      stubPost(url, ACCEPTED, Json.toJson(nrsPayload).toString, "")

      whenReady(connector.submitToNrs(nrsPayload)) { result =>
        result mustBe NrsSubmissionResult.Success
        verifyPost(url)
      }
    }

    "return PermanentFailure when NRS returns BAD_REQUEST" in new SetUp {
      val nrsPayload: NrsPayload = createTestPayload()

      stubPost(url, BAD_REQUEST, Json.toJson(nrsPayload).toString, "Bad request")

      whenReady(connector.submitToNrs(nrsPayload)) { result =>
        result mustBe NrsSubmissionResult.PermanentFailure
        verifyPost(url)
      }
    }

    "return RetryableFailure when NRS returns INTERNAL_SERVER_ERROR" in new SetUp {
      val nrsPayload: NrsPayload = createTestPayload()

      stubPost(url, INTERNAL_SERVER_ERROR, Json.toJson(nrsPayload).toString, "Internal server error")

      whenReady(connector.submitToNrs(nrsPayload)) { result =>
        result mustBe NrsSubmissionResult.RetryableFailure
        verifyPost(url)
      }
    }

    "return RetryableFailure when NRS returns an unexpected 5xx error" in new SetUp {
      val nrsPayload: NrsPayload = createTestPayload()

      stubPost(url, SERVICE_UNAVAILABLE, Json.toJson(nrsPayload).toString, "Unexpected error")

      whenReady(connector.submitToNrs(nrsPayload)) { result =>
        result mustBe NrsSubmissionResult.RetryableFailure
        verifyPost(url)
      }
    }

    "return RetryableFailure when a network fault occurs" in new SetUp {
      val nrsPayload: NrsPayload = createTestPayload()

      import com.github.tomakehurst.wiremock.http.Fault

      val requestBody: String = Json.toJson(nrsPayload).toString
      stubPostFault(url, requestBody, Fault.CONNECTION_RESET_BY_PEER)

      whenReady(connector.submitToNrs(nrsPayload)) { result =>
        result mustBe NrsSubmissionResult.RetryableFailure
        verifyPost(url)
      }
    }

    "use existing correlation ID when present in header carrier" in new SetUp {
      val nrsPayload: NrsPayload = createTestPayload()
      val existingCorrelationId = "existing-correlation-id-123"
      val hcWithCorrelationId: HeaderCarrier = hc.copy(
        otherHeaders = hc.otherHeaders :+ ("X-Correlation-Id" -> existingCorrelationId)
      )

      stubPost(url, ACCEPTED, Json.toJson(nrsPayload).toString, "")

      whenReady(connector.submitToNrs(nrsPayload)(hcWithCorrelationId)) { result =>
        result mustBe NrsSubmissionResult.Success
        verifyPostWithHeader(url, "X-Correlation-Id", existingCorrelationId)
      }
    }
  }

  class SetUp extends ConnectorFixture {
    val connector: NrsConnector = app.injector.instanceOf[NrsConnector]
    val url = "/nrs-orchestrator/submission"

    def createTestPayload(): NrsPayload = NrsPayload(
      payload = "encodedPayload",
      metadata = NrsMetadata(
        businessId = "vpd",
        notableEvent = "vaping-duty-return-submitted",
        payloadContentType = "application/json",
        payloadSha256Checksum = "checksum123",
        userSubmissionTimestamp = Instant.now(clock).toString,
        identityData = IdentityData(
          internalId = Some("Int-123"),
          optionalCredentials = None,
          confidenceLevel = ConfidenceLevel.L50,
          groupIdentifier = None,
          credentialRole = None,
          affinityGroup = Some(Organisation),
          credentialStrength = Some(CredentialStrength.strong)
        ),
        userAuthToken = "Bearer token123",
        headerData = Map.empty[String, String],
        searchKeys = Map("approvalId" -> "XMVPD0000000123")
      )
    )
  }
}