package services.returns

import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import play.api.libs.json.{JsObject, JsValue, Json}

object SubmitReturnAuditEvent {

  def buildExplicitAuditEvent(submission: ReturnCreateRequest,
                              result: ReturnSubmittedResponse): JsObject = {
    buildExplicitAuditEvent(Json.toJson(submission), Json.toJson(result))
  }

  def buildExplicitAuditEvent(submission: JsValue,
                              result: JsValue): JsObject = {
    new JsObject(Map(
      "submission" -> submission,
      "response" -> result
    ))
  }

}
