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

package services.nrs

import models.identifiers.{PeriodKey, VpdId}
import models.nrs.{IdentityData, NrsMetadata, NrsPayload, NrsSubmissionWorkItem}
import models.returns.submit.ReturnCreateRequest
import play.api.libs.json.Json

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.inject.{Inject, Singleton}

@Singleton
class NrsPayloadBuilder @Inject()() {

  private val BUSINESS_ID = "vpd"
  private val NOTABLE_EVENT = "vpd-return-submitted"
  private val PAYLOAD_CONTENT_TYPE = "application/json"

  def buildWorkItem(
    returnData: ReturnCreateRequest,
    vpdId: VpdId,
    periodKey: PeriodKey,
    identityData: IdentityData,
    authToken: String,
    headers: Map[String, String]
  ): NrsSubmissionWorkItem = {
    
    val payloadString = Json.stringify(Json.toJson(returnData))
    val checksum = calculateChecksum(payloadString)
    val encodedPayload = encodePayload(payloadString)
    val timestamp = generateTimestamp()
    
    val metadata = NrsMetadata(
      businessId = BUSINESS_ID,
      notableEvent = NOTABLE_EVENT,
      payloadContentType = PAYLOAD_CONTENT_TYPE,
      payloadSha256Checksum = checksum,
      userSubmissionTimestamp = timestamp,
      identityData = identityData,
      userAuthToken = authToken,
      headerData = headers,
      searchKeys = Map(
        "zvpd" -> vpdId.value,
        "periodKey" -> periodKey.value
      )
    )
    
    NrsSubmissionWorkItem(NrsPayload(encodedPayload, metadata))
  }

  private def encodePayload(jsonString: String): String = {
    Base64.getEncoder.encodeToString(jsonString.getBytes(StandardCharsets.UTF_8))
  }

  private def calculateChecksum(payloadString: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(payloadString.getBytes(StandardCharsets.UTF_8))
    String.format("%064x", new java.math.BigInteger(1, hash))
  }

  private def generateTimestamp(): String = {
    ZonedDateTime.now(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT)
  }
}