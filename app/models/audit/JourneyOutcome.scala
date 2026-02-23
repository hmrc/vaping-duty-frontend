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

package models.audit

import models.audit.Actions.{AmendEmailAddress, ChangeToEmail, ChangeToPost, Unchanged}
import models.audit.PreferenceAction.*
import models.contactPreference.PaperlessPreference
import models.contactPreference.PaperlessPreference.{Email, Post}
import models.emailverification.PaperlessPreferenceSubmission
import models.requests.DataRequest
import play.api.libs.json.{Json, OFormat}

case class JourneyOutcome(
    timeStarted: String,
    credentialId: String,
    vpdId: String,
    originalContactPreference: String,
    originalContactPreferenceValue: String,
    contactPreferenceChange: String,
    contactPreferenceInput: Option[ContactPreferenceInput]
)

object JourneyOutcome {

  def buildEvent(preferenceSubmission: PaperlessPreferenceSubmission, initialPreference: PaperlessPreference, address: String)
                (implicit request: DataRequest[?]): JourneyOutcome = {

    JourneyOutcome(
      timeStarted = request.userAnswers.startedTime.toString,
      credentialId = request.credId,
      vpdId = request.enrolmentVpdId,
      originalContactPreference = initialPreference.toString,
      originalContactPreferenceValue = initialPreference match {
        case Email => request.userAnswers.subscriptionSummary.emailAddress.getOrElse("")
        case Post => address
      },
      contactPreferenceChange = assertEventType(preferenceSubmission),
      contactPreferenceInput = Some(
        ContactPreferenceInput(
          request.userAnswers.emailAddress,
          if (preferenceSubmission.paperlessPreference) {
            None
          } else {
            Some(address)
          }
        )
      )
    )
  }

  private def assertEventType(preferenceSubmission: PaperlessPreferenceSubmission)
                             (implicit request: DataRequest[?]) = {

    getAction(preferenceSubmission) match {
      case EmailToEmail => AmendEmailAddress.toString
      case EmailToPost  => ChangeToPost.toString
      case PostToEmail  => ChangeToEmail.toString
      // The Post to Post journey does not allow the user to set the address in the service, instead they need to submit
      // a request to the Compliance Team to do this and the journey directs them to do this. There is no change of
      // preferences to record on the journey, and it does not POST up a change.
      // This case is included here simply out of completeness but will never be used.
      case PostToPost   => Unchanged.toString
    }
  }

  def getAction(preferenceSubmission: PaperlessPreferenceSubmission)
               (implicit request: DataRequest[?]): PreferenceAction = {

    PreferenceAction((preferenceSubmission.paperlessPreference, request.userAnswers.subscriptionSummary.paperlessPreference))
  }

  implicit val format: OFormat[JourneyOutcome] = Json.format[JourneyOutcome]
}
