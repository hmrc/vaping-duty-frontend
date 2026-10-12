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

package models.nrs

import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, User}
import base.SpecBase

class IdentityDataSpec extends SpecBase {

  // Only test the 7 fields actually retrieved from auth in NrsService.retrieveIdentityData()
  private val testIdentityData = IdentityData(
    internalId = Some("int-id-123"),
    optionalCredentials = Some(Credentials("cred-id", "GovernmentGateway")),
    confidenceLevel = ConfidenceLevel.L200,
    groupIdentifier = Some("group-123"),
    credentialRole = Some(User),
    affinityGroup = Some(AffinityGroup.Individual),
    credentialStrength = Some("strong")
  )

  "IdentityData" - {
    "must serialize to JSON correctly with fields used in application" in {
      val json = Json.toJson(testIdentityData)
      
      (json \ "internalId").as[String] mustBe "int-id-123"
      (json \ "confidenceLevel").as[Int] mustBe 200
      (json \ "groupIdentifier").as[String] mustBe "group-123"
      (json \ "credentialRole").as[String] mustBe "User"
      (json \ "affinityGroup").as[String] mustBe "Individual"
      (json \ "credentialStrength").as[String] mustBe "strong"
      (json \ "optionalCredentials" \ "providerId").as[String] mustBe "cred-id"
      (json \ "optionalCredentials" \ "providerType").as[String] mustBe "GovernmentGateway"
    }

    "must deserialize from JSON correctly with fields used in application" in {
      val json = Json.obj(
        "internalId" -> "int-id-123",
        "optionalCredentials" -> Json.obj(
          "providerId" -> "cred-id",
          "providerType" -> "GovernmentGateway"
        ),
        "confidenceLevel" -> 200,
        "groupIdentifier" -> "group-123",
        "credentialRole" -> "User",
        "affinityGroup" -> "Individual",
        "credentialStrength" -> "strong"
      )

      val result = json.as[IdentityData]
      
      result.internalId mustBe Some("int-id-123")
      result.optionalCredentials mustBe Some(Credentials("cred-id", "GovernmentGateway"))
      result.confidenceLevel mustBe ConfidenceLevel.L200
      result.groupIdentifier mustBe Some("group-123")
      result.credentialRole mustBe Some(User)
      result.affinityGroup mustBe Some(AffinityGroup.Individual)
      result.credentialStrength mustBe Some("strong")
    }

    "must handle minimal IdentityData with only required field" in {
      val minimalData = IdentityData(
        confidenceLevel = ConfidenceLevel.L50
      )

      val json = Json.toJson(minimalData)
      val result = json.as[IdentityData]
      
      result.internalId mustBe None
      result.optionalCredentials mustBe None
      result.groupIdentifier mustBe None
      result.credentialRole mustBe None
      result.affinityGroup mustBe None
      result.credentialStrength mustBe None
      result.confidenceLevel mustBe ConfidenceLevel.L50
    }

    "must round-trip through JSON" in {
      val json = Json.toJson(testIdentityData)
      val result = json.as[IdentityData]
      
      result mustBe testIdentityData
    }
  }
}
