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

import models.identifiers.Identifiers
import models.obligations.ObligationDetails
import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import play.api.libs.json.*

object SubmitReturnAuditEvent {

  def buildExplicitAuditEvent(submission: ReturnCreateRequest,
                              result: ReturnSubmittedResponse,
                              identifiers: Identifiers,
                              obligations: Seq[ObligationDetails]): JsObject = {
    buildExplicitAuditEvent(Json.toJson(submission), result, identifiers, obligations)
  }

  def buildExplicitAuditEvent(submission: JsValue,
                              response: ReturnSubmittedResponse,
                              identifiers: Identifiers,
                              obligations: Seq[ObligationDetails]): JsObject = {
    new JsObject(Map(
      "submission"       -> buildSubmission(submission, obligations),
      "prePopulatedData" -> buildPrePopulatedData(identifiers),
      "response"         -> buildResponse(response)
    ))
  }

  def buildSubmission(etmpSubmission: JsValue,
                      obligations: Seq[ObligationDetails]): JsValue = {
    val submissionWithRenamedKeys = renameKeys(etmpSubmission)

    makePeriodKeysHumanReadable(
      submissionWithRenamedKeys,
      Set("returnPeriod", "returnPeriodAffected"),
      buildObligationsToHumanReadableMap(obligations))
  }

  private def renameKeys(etmpSubmission: JsValue) = {
    jsRenameKeys(etmpSubmission, Map(
      // Replace periodKey jargon with clearer returnPeriod
      "periodKey" -> "returnPeriod",

      // Be clear that volumes are recorded in Litres
      "amountProducedLiquid" -> "amountProducedLiquidLitres",
      "amountUnderDeclared"  -> "amountUnderDeclaredLitres",
      "amountOverDeclared"   -> "amountOverDeclaredLitres",
      "amountSpoilt"         -> "amountSpoiltLitres",
      "volumeMovedFromDutySuspense" -> "volumeMovedFromDutySuspenseLitres",
      "volumeMovedToDutySuspense"   -> "volumeMovedToDutySuspenseLitres",

      // Expand abbreviations
      "underDeclFilled"    -> "underDeclarationFilled",
      "reasonForUnderDecl" -> "reasonForUnderDeclaration",
      "overDeclFilled"     -> "overDeclarationFilled",
      "reasonForOverDecl"  -> "reasonForOverDeclaration",
      "vapingProdManufactured" -> "vapingProductsManufactured"
    ))
  }

  def buildObligationsToHumanReadableMap(obligations: Seq[ObligationDetails]): Map[String, String] =
    obligations.map(ob => ob.periodKey -> s"${ob.iCFromDate} to ${ob.iCToDate}").toMap

  def buildPrePopulatedData(identifiers: Identifiers): JsValue = {
    JsObject(Map(
      "approvalId"   -> JsString(identifiers.enrolmentVpdId.toString),
      "credentialId" -> JsString(identifiers.credId.toString),
      "groupId"      -> JsString(identifiers.groupId.toString)
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

  def makePeriodKeysHumanReadable(jsValue: JsValue,
                                  fieldsToChange: Set[String],
                                  periodKeyToHumanReadable: Map[String, String]): JsValue = {
    jsValue match {
      case jsObj: JsObject =>
        JsObject(
          jsObj.fields.map((key, value) =>
            key -> (
              if (fieldsToChange.contains(key)) {
                val periodKey = value.as[JsString].value
                periodKeyToHumanReadable.get(periodKey)
                  .map(JsString(_))
                  .getOrElse(value)
              }
              else
                makePeriodKeysHumanReadable(value, fieldsToChange, periodKeyToHumanReadable)
              )
          )
        )

      case array: JsArray =>
        JsArray(array.value.map((jsv: JsValue) =>
          makePeriodKeysHumanReadable(
            jsv,
            fieldsToChange,
            periodKeyToHumanReadable)))

      case x => x
    }
  }
}
