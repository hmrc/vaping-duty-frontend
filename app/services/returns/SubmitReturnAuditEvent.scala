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

package services.returns

import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue, Json}

object SubmitReturnAuditEvent {

  def buildExplicitAuditEvent(submission: ReturnCreateRequest,
                              result: ReturnSubmittedResponse): JsObject = {
    buildExplicitAuditEvent(Json.toJson(submission), result)
  }

  def buildExplicitAuditEvent(submission: JsValue,
                              response: ReturnSubmittedResponse): JsObject = {
    new JsObject(Map(
      "submission" -> buildSubmission(submission),
      "response" -> buildResponse(response)
    ))
  }

  def buildSubmission(etmpSubmission: JsValue): JsValue = {
    jsRenameKeys(etmpSubmission, Map(
      "periodKey" -> "returnPeriod",
      "amountProducedLiquid" -> "amountProducedLiquidLitres",
      "amountUnderDeclared" -> "amountUnderDeclaredLitres",
      "amountOverDeclared" -> "amountOverDeclaredLitres",
      "amountSpoilt" -> "amountSpoiltLitres",
      "volumeMovedFromDutySuspense" -> "volumeMovedFromDutySuspenseLitres",
      "volumeMovedToDutySuspense" -> "volumeMovedToDutySuspenseLitres"
    ))
  }

  def buildResponse(etmpResponse: ReturnSubmittedResponse): JsValue = {
    JsObject(Seq(
      etmpResponse.submissionId   .map(submissionId    => "submissionId"    -> JsString(submissionId)),
      etmpResponse.chargeReference.map(chargeReference => "chargeReference" -> JsString(chargeReference)),
      etmpResponse.paymentDueDate .map(paymentDueDate  => "paymentDueDate"  -> JsString(paymentDueDate.toString))
    ).flatten)
  }

  def jsRenameKeys(jsValue: JsValue, nameChanges: Map[String, String]): JsValue =
    jsValue match {
      case jsObj: JsObject =>
        JsObject(
          jsObj.fields.map((key, value) =>
            nameChanges.getOrElse(key, key) -> jsRenameKeys(value, nameChanges))
        )

      case array: JsArray =>
        JsArray(array.value.map((jsv: JsValue) => jsRenameKeys(jsv, nameChanges)))

      case x => x
    }

}
