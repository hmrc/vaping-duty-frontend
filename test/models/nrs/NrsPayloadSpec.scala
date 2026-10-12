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

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import base.SpecBase

class NrsPayloadSpec extends SpecBase {

  private val testEncodedPayload = "eyJ0ZXN0IjoicGF5bG9hZCJ9"
  private val testSha256Hash = "a7c976db1723adb41274178dc82e9b777941ab201c69de61d0f2bc6d27a32534"
  private val testTimestamp = "2024-01-15T10:30:00Z"
  private val testAuthToken = "Bearer test-token"
  private val testVpdId = "XMVPD0000000123"

  private val testIdentityData = IdentityData(
    internalId = Some("int-id-123"),
    confidenceLevel = ConfidenceLevel.L200
  )

  private val testPeriodKey = "24AF"

  private val testMetadata = NrsMetadata.create(
    payLoad = """{"test":"payload"}""",
    sha256Hash = testSha256Hash,
    identityData = testIdentityData,
    submissionTimeStamp = testTimestamp,
    userAuthToken = testAuthToken,
    userHeaderData = Map("X-Request-ID" -> "test-request-id"),
    vpdId = testVpdId,
    periodKey = testPeriodKey
  )

  "NrsPayload" - {
    "toJsObject" - {
      "must convert NrsPayload to JsObject" in {
        val payload = NrsPayload(testEncodedPayload, testMetadata)
        
        val result = payload.toJsObject
        
        result mustBe a[JsObject]
        (result \ "payload").as[String] mustBe testEncodedPayload
        (result \ "metadata" \ "businessId").as[String] mustBe "vpd"
        (result \ "metadata" \ "notableEvent").as[String] mustBe "vpd-return-submitted"
      }

      "must include all metadata fields in JsObject" in {
        val payload = NrsPayload(testEncodedPayload, testMetadata)
        
        val result = payload.toJsObject
        
        (result \ "metadata" \ "payloadContentType").as[String] mustBe "application/json"
        (result \ "metadata" \ "payloadSha256Checksum").as[String] mustBe testSha256Hash
        (result \ "metadata" \ "userSubmissionTimestamp").as[String] mustBe testTimestamp
        (result \ "metadata" \ "userAuthToken").as[String] mustBe testAuthToken
        (result \ "metadata" \ "searchKeys" \ "approvalId").as[String] mustBe testVpdId
        (result \ "metadata" \ "searchKeys" \ "periodKey").as[String] mustBe testPeriodKey
      }
    }

    "must serialize to JSON correctly" in {
      val payload = NrsPayload(testEncodedPayload, testMetadata)
      
      val json = Json.toJson(payload)
      
      (json \ "payload").as[String] mustBe testEncodedPayload
      (json \ "metadata" \ "businessId").as[String] mustBe "vpd"
      (json \ "metadata" \ "notableEvent").as[String] mustBe "vpd-return-submitted"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "payload" -> testEncodedPayload,
        "metadata" -> Json.obj(
          "businessId" -> "vpd",
          "notableEvent" -> "vpd-return-submitted",
          "payloadContentType" -> "application/json",
          "payloadSha256Checksum" -> testSha256Hash,
          "userSubmissionTimestamp" -> testTimestamp,
          "identityData" -> Json.obj(
            "confidenceLevel" -> 200,
            "agentInformation" -> Json.obj()
          ),
          "userAuthToken" -> testAuthToken,
          "headerData" -> Json.obj(
            "X-Request-ID" -> "test-request-id"
          ),
          "searchKeys" -> Json.obj(
            "vpdId" -> testVpdId
          )
        )
      )

      val result = json.as[NrsPayload]
      
      result.payload mustBe testEncodedPayload
      result.metadata.businessId mustBe "vpd"
      result.metadata.notableEvent mustBe "vpd-return-submitted"
    }

    "must round-trip through JSON" in {
      val payload = NrsPayload(testEncodedPayload, testMetadata)
      
      val json = Json.toJson(payload)
      val result = json.as[NrsPayload]
      
      result mustBe payload
    }

    "must round-trip through toJsObject" in {
      val payload = NrsPayload(testEncodedPayload, testMetadata)
      
      val jsObject = payload.toJsObject
      val result = jsObject.as[NrsPayload]
      
      result mustBe payload
    }
  }
}