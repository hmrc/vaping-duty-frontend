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

package models.audit

import base.SpecBase
import data.TestData
import models.UserAnswers
import models.audit.Actions.*
import models.audit.PreferenceAction.PostToPost
import models.contactPreference.PaperlessPreference.*
import models.requests.DataRequest
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.test.FakeRequest

import scala.language.implicitConversions

class JourneyOutcomeSpec extends AnyFreeSpec with Matchers with TestData with SpecBase {

  def request(ua: UserAnswers): DataRequest[AnyContent] = {
    DataRequest[AnyContent](FakeRequest(), vpdId, userId, credId, ua)
  }

  "JourneyOutcome" - {

    val json = """{"timeStarted":"2024-06-11T15:07:47.838Z","credentialId":"cred-id","vpdId":"VPPAID01","originalContactPreference":"Email","originalContactPreferenceValue":"john.doe@example.com","contactPreferenceChange":"AmendEmailAddress","contactPreferenceInput":{"emailAddressInput":"john.doe@example.com"}}"""
    val journeyOutcomeModel = JourneyOutcome.buildEvent(
      contactPreferenceSubmissionNewEmail,
      Email,
      userAnswersEmailUpdate.subscriptionSummary.correspondenceAddress
    )(request(userAnswersEmailUpdate.copy(emailAddress = Some(emailAddress))))

    "must serialise to json" in {

      Json.toJson(journeyOutcomeModel).toString mustBe json
    }

    "must serialise from json" in {
      val result = Json.parse(json).validate[JourneyOutcome].get

      result mustBe journeyOutcomeModel
    }
  }

  "JourneyOutcome.buildEvent" - {

    "must create AmendEmailAddress event correctly" in {
      val result = JourneyOutcome.buildEvent(
        contactPreferenceSubmissionNewEmail,
        Email,
        userAnswersEmailUpdate.subscriptionSummary.correspondenceAddress
      )(request(userAnswersEmailUpdate))

      result.contactPreferenceChange mustBe AmendEmailAddress.toString
    }

    "must create ChangeToPost event correctly" in {
      val result = JourneyOutcome.buildEvent(
        contactPreferenceSubmissionPost,
        Post,
        userAnswers.subscriptionSummary.correspondenceAddress
      )(request(userAnswers))

      result.contactPreferenceChange mustBe ChangeToPost.toString
    }

    "must create ChangeToEmail event correctly" in {
      val result = JourneyOutcome.buildEvent(
        contactPreferenceSubmissionEmail,
        Email,
        userAnswersPostNoEmail.subscriptionSummary.correspondenceAddress
      )(request(userAnswersPostNoEmail))

      result.contactPreferenceChange mustBe ChangeToEmail.toString
    }

    "must create PostToPost event correctly" in {
      val result = JourneyOutcome.buildEvent(
        contactPreferenceSubmissionPost,
        Post,
        userAnswersPostWithEmail.subscriptionSummary.correspondenceAddress
      )(request(userAnswersPostWithEmail))

      result.contactPreferenceChange mustBe PostToPost.toString
    }
  }

  "JourneyOutcome.getAction" - {

    "must return EmailToEmail" in {
      val result = JourneyOutcome.getAction(contactPreferenceSubmissionNewEmail)(request(userAnswersEmailUpdate))

      result mustBe PreferenceAction.EmailToEmail
    }

    "must return EmailToPost" in {
      val result = JourneyOutcome.getAction(contactPreferenceSubmissionPost)(request(userAnswers))

      result mustBe PreferenceAction.EmailToPost
    }

    "must return PostToEmail" in {
      val result = JourneyOutcome.getAction(contactPreferenceSubmissionEmail)(request(userAnswersPostNoEmail))

      result mustBe PreferenceAction.PostToEmail
    }

    "must return PostToPost" in {
      val result = JourneyOutcome.getAction(contactPreferenceSubmissionPost)(request(userAnswersPostWithEmail))

      result mustBe PreferenceAction.PostToPost
    }
  }
}
