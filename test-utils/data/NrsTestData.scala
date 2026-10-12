/*
 * Copyright 2025 HM Revenue & Customs
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

package data

import models.nrs.{IdentityData, NrsMetadata}
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, User}

import java.time.Instant

trait NrsTestData {
  self: TestData =>

  val sampleIdentityData: IdentityData = IdentityData(
    internalId = Some("test-internal-id"),
    optionalCredentials = Some(Credentials("test-cred-id", "GovernmentGateway")),
    confidenceLevel = ConfidenceLevel.L50,
    groupIdentifier = Some("test-group-id"),
    credentialRole = Some(User),
    affinityGroup = Some(AffinityGroup.Organisation),
    credentialStrength = Some("strong")
  )

  val sampleNrsMeta: NrsMetadata = NrsMetadata(
    businessId = "vpd",
    notableEvent = "vaping-duty-return",
    payloadContentType = "application/json",
    payloadSha256Checksum = "test-checksum",
    userSubmissionTimestamp = Instant.now(clock).toString,
    identityData = sampleIdentityData,
    userAuthToken = "test-auth-token",
    headerData = Map.empty[String, String],
    searchKeys = Map("approvalId" -> vpdId.value, "periodKey" -> periodKey.value)
  )
}
