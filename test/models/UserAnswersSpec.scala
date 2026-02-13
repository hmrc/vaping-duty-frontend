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

package models

import base.SpecBase
import play.api.libs.json.{JsPath, Json}
import queries.{Settable, Gettable}

import java.time.Instant
import scala.util.Success

class UserAnswersSpec extends SpecBase {
  val ua = userAnswersPostWithEmail.copy(validUntil = Some(Instant.now(clock).plusMillis(1)))

  case object TestPage extends Gettable[String] with Settable[String] {
    override def path: JsPath = JsPath \ toString
  }

  "UserAnswers" - {
    val json =
      s"""{"vpdId":"$vpdId","userId":"$userId","subscriptionSummary":{"paperlessPreference":false,"emailAddress":"john.doe@example.com","emailVerification":true,"bouncedEmail":false,"correspondenceAddress":"Flat 123\\n1 Example Road\\nLondon\\nAB1 2CD","countryCode":"GB"},"emailAddress":"john.doe@example.com","data":{"contactPreferenceEmail":true},"startedTime":{"$$date":{"$$numberLong":"1718118467838"}},"lastUpdated":{"$$date":{"$$numberLong":"1718118467838"}},"validUntil":{"$$date":{"$$numberLong":"1718118467839"}}}"""

    "must set a value for a given page and get the same value" in {

      val userAnswers = emptyUserAnswers

      val expectedValue = "value"

      val updatedUserAnswers = userAnswers.set(TestPage, expectedValue) match {
        case Success(value) => value
        case _              => fail()
      }

      val actualValue = updatedUserAnswers.get(TestPage) match {
        case Some(value) => value
        case _           => fail()
      }

      expectedValue mustBe actualValue
    }

    "must remove a value for a given page" in {
      val userAnswers = emptyUserAnswers
        .set(TestPage, "value")
        .success
        .value

      val updatedUserAnswers = userAnswers.remove(TestPage) match {
        case Success(updatedUA) => updatedUA
        case _                  => fail()
      }

      val actualValueOption = updatedUserAnswers.get(TestPage)
      actualValueOption mustBe None
    }

    "must serialise to json" in {
      Json.toJson(ua).toString() mustBe json
    }

    "must deserialise from json" in {
      Json.parse(json).as[UserAnswers] mustBe ua
    }
  }
}
