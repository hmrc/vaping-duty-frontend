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

package uk.gov.hmrc.vapingdutyfinance.models

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}

enum MainTransactionType(val code: String, val description: String):
  case PaymentOnAccount extends MainTransactionType("0060", "Payment on Account")
  case Return extends MainTransactionType("4060", "Return")
  case LatePaymentInterest extends MainTransactionType("4061", "Late Payment Interest")

object MainTransactionType {
  given format: Format[MainTransactionType] = new Format[MainTransactionType] {
    override def reads(json: JsValue): JsResult[MainTransactionType] =
      json.validate[String].flatMap {
        case "PaymentOnAccount"    => JsSuccess(MainTransactionType.PaymentOnAccount)
        case "Return"              => JsSuccess(MainTransactionType.Return)
        case "LatePaymentInterest" => JsSuccess(MainTransactionType.LatePaymentInterest)
        case other                 => JsError(s"Unknown MainTransactionType: $other")
      }

    override def writes(value: MainTransactionType): JsValue =
      JsString(value.toString)
  }
}
