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

package models.nrs

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption

final case class NrsSubmissionWorkItem(payload: NrsPayload) {
  def vpdId: String = payload.metadata.searchKeys("approvalId")
  def periodKey: String = payload.metadata.searchKeys("periodKey")
}

object NrsSubmissionWorkItem {
  // Standard format for non-repository use
  implicit val format: OFormat[NrsSubmissionWorkItem] = Json.format[NrsSubmissionWorkItem]

  // MongoDB format with encryption for repository storage
  def mongoFormat(implicit crypto: Encrypter with Decrypter): OFormat[NrsSubmissionWorkItem] = {
    implicit val sensitiveFormat: Format[SensitiveString] = sensitiveStringFormat
    
    // Custom format that encrypts only the payload.payload field, not the metadata
    implicit val encryptedPayloadFormat: OFormat[NrsPayload] = {
      (
        (__ \ "payload").format[SensitiveString]
          .inmap[String](
            s => s.decryptedValue,
            str => SensitiveString(str)
          ) and
        (__ \ "metadata").format[NrsMetadata]
      )(NrsPayload.apply, p => (p.payload, p.metadata))
    }
    
    // Format for the work item using the encrypted payload format
    (__ \ "payload").format[NrsPayload](encryptedPayloadFormat)
      .inmap[NrsSubmissionWorkItem](
        NrsSubmissionWorkItem.apply,
        _.payload
      )
  }

  private def sensitiveStringFormat(implicit crypto: Encrypter with Decrypter): Format[SensitiveString] =
    JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)
}
