package services.returns

import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import play.api.libs.json.{JsObject, Json}

object SubmitReturnAuditEvent {

  def buildExplicitAuditEvent(submission: ReturnCreateRequest,
                              result: ReturnSubmittedResponse): JsObject = {
    Json.toJson(submission).as[JsObject]
  }

}
