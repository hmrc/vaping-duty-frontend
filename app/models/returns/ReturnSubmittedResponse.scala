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

package models.returns

import play.api.libs.json.{Json, OFormat}

import java.time.{Instant, LocalDate}

case class ReturnSubmittedResponse(
  processingDate: Instant,
  vpdReferenceNumber: String,
  submissionID: Option[String],
  chargeReference: Option[String],
  amount: BigDecimal,
  paymentDueDate: Option[LocalDate]
)

object ReturnSubmittedResponse {
  given OFormat[ReturnSubmittedResponse] = Json.format[ReturnSubmittedResponse]
}