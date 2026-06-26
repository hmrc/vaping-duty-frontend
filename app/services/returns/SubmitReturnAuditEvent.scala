package services.returns

import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}

object SubmitReturnAuditEvent {

  def buildExplicitAuditEvent(submission: ReturnCreateRequest,
                              result: ReturnSubmittedResponse): JsObject = {
    buildExplicitAuditEvent(Json.toJson(submission), Json.toJson(result))
  }

  def buildExplicitAuditEvent(submission: JsValue,
                              response: JsValue): JsObject = {
    new JsObject(Map(
      "submission" -> buildSubmission(submission),
      "response" -> buildResponse(response)
    ))
  }

  def buildSubmission(etmpSubmission: JsValue): JsValue = {
    jsRenameKeys(etmpSubmission, Map(
      "periodKey" -> "returnPeriod",
      "amountProducedLiquid" -> "amountProducedLiquidLitres"
    ))
  }

  def buildResponse(etmpSubmission: JsValue): JsValue = {
    etmpSubmission
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
