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

import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainBytes, PlainContent, PlainText}

class NrsSubmissionWorkItemSpec extends SpecBase {

  // Mock crypto for testing
  private implicit val mockCrypto: Encrypter with Decrypter = new Encrypter with Decrypter {
    override def encrypt(plain: PlainContent): Crypted = plain match {
      case PlainText(text) => Crypted(s"encrypted-$text")
      case PlainBytes(bytes) => Crypted(s"encrypted-${new String(bytes)}")
    }
    override def decrypt(crypted: Crypted): PlainText = 
      PlainText(crypted.value.stripPrefix("encrypted-"))
    override def decryptAsBytes(crypted: Crypted): PlainBytes = 
      PlainBytes(crypted.value.stripPrefix("encrypted-").getBytes)
  }

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

  private val testPayload = NrsPayload(testEncodedPayload, testMetadata)

  "NrsSubmissionWorkItem" - {
    "must serialize to JSON correctly" in {
      val workItem = NrsSubmissionWorkItem(testPayload)
      
      val json = Json.toJson(workItem)
      
      (json \ "payload" \ "payload").as[String] mustBe testEncodedPayload
      (json \ "payload" \ "metadata" \ "businessId").as[String] mustBe "vpd"
      (json \ "payload" \ "metadata" \ "notableEvent").as[String] mustBe "vpd-return-submitted"
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "payload" -> Json.obj(
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
              "approvalId" -> testVpdId,
              "periodKey" -> testPeriodKey
            )
          )
        )
      )

      val result = json.as[NrsSubmissionWorkItem]
      
      result.payload.payload mustBe testEncodedPayload
      result.payload.metadata.businessId mustBe "vpd"
      result.payload.metadata.notableEvent mustBe "vpd-return-submitted"
    }

    "must round-trip through JSON" in {
      val workItem = NrsSubmissionWorkItem(testPayload)
      
      val json = Json.toJson(workItem)
      val result = json.as[NrsSubmissionWorkItem]
      
      result mustBe workItem
    }

    "must preserve all payload data through serialization" in {
      val workItem = NrsSubmissionWorkItem(testPayload)
      
      val json = Json.toJson(workItem)
      val result = json.as[NrsSubmissionWorkItem]
      
      result.payload.payload mustBe testPayload.payload
      result.payload.metadata.payloadSha256Checksum mustBe testPayload.metadata.payloadSha256Checksum
      result.payload.metadata.userSubmissionTimestamp mustBe testPayload.metadata.userSubmissionTimestamp
      result.payload.metadata.userAuthToken mustBe testPayload.metadata.userAuthToken
      result.payload.metadata.searchKeys mustBe testPayload.metadata.searchKeys
    }

    "must provide access to vpdId" in {
      val workItem = NrsSubmissionWorkItem(testPayload)
      
      workItem.vpdId mustBe testVpdId
    }

    "must provide access to periodKey" in {
      val workItem = NrsSubmissionWorkItem(testPayload)
      
      workItem.periodKey mustBe testPeriodKey
    }

    "mongoFormat must" - {
      "encrypt and decrypt the payload correctly" in {
        val workItem = NrsSubmissionWorkItem(testPayload)
        
        val json = NrsSubmissionWorkItem.mongoFormat.writes(workItem)
        val result = NrsSubmissionWorkItem.mongoFormat.reads(json).get
        
        result mustBe workItem
      }

      "round-trip through encryption" in {
        val workItem = NrsSubmissionWorkItem(testPayload)
        
        val encrypted = NrsSubmissionWorkItem.mongoFormat.writes(workItem)
        val decrypted = NrsSubmissionWorkItem.mongoFormat.reads(encrypted).get
        
        decrypted.payload.payload mustBe testPayload.payload
        decrypted.payload.metadata.businessId mustBe testPayload.metadata.businessId
        decrypted.payload.metadata.searchKeys mustBe testPayload.metadata.searchKeys
      }

      "store encrypted data" in {
        val workItem = NrsSubmissionWorkItem(testPayload)
        
        val json = NrsSubmissionWorkItem.mongoFormat.writes(workItem)
        val jsonString = Json.stringify(json)
        
        // The JSON should contain encrypted data (prefixed with "encrypted-")
        jsonString must include("encrypted-")
      }
    }
  }
}
