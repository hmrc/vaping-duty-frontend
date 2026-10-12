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

import play.api.libs.json.{Json, OFormat}

final case class NrsMetadata(
  businessId: String,
  notableEvent: String,
  payloadContentType: String,
  payloadSha256Checksum: String,
  userSubmissionTimestamp: String,
  identityData: IdentityData,
  userAuthToken: String,
  headerData: Map[String, String],
  searchKeys: Map[String, String]
)

object NrsMetadata {
  private val BUSINESS_ID = "vpd"
  private val NOTABLE_EVENT = "vpd-return-submitted"
  private val SEARCH_KEY_ZVPD = "zvpd"
  private val SEARCH_KEY_PERIOD = "periodKey"
  private val PAYLOAD_CONTENT_TYPE = "application/json"
  
  val notableEventSubmitReturn: String = NOTABLE_EVENT

  def create(
    payLoad: String,
    sha256Hash: String,
    identityData: IdentityData,
    submissionTimeStamp: String,
    userAuthToken: String,
    userHeaderData: Map[String, String],
    vpdId: String,
    periodKey: String
  ): NrsMetadata =
    NrsMetadata(
      businessId = BUSINESS_ID,
      notableEvent = NOTABLE_EVENT,
      payloadContentType = PAYLOAD_CONTENT_TYPE,
      payloadSha256Checksum = sha256Hash,
      userSubmissionTimestamp = submissionTimeStamp,
      identityData = identityData,
      userAuthToken = userAuthToken,
      headerData = userHeaderData,
      searchKeys = Map(
        SEARCH_KEY_ZVPD -> vpdId,
        SEARCH_KEY_PERIOD -> periodKey
      )
    )

  implicit val format: OFormat[NrsMetadata] = Json.format[NrsMetadata]
}