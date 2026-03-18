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

package models.contactPreference

import base.SpecBase
import play.api.libs.json.{JsSuccess, Json}

class SubscriptionContactPreferencesSpec extends SpecBase {

  "SubscriptionContactPreferences" - {

    val json = """{"paperlessPreference":"1","emailAddress":"john.doe@example.com"}"""
    val underTest = SubscriptionContactPreferences(true, Some(emailAddress))

    "must serialise to JSON" in {
      Json.toJson(underTest).toString mustBe json
    }

    "must serialise from JSON" in {
      val result = Json.parse(json).validate[SubscriptionContactPreferences]
      result mustBe JsSuccess(underTest)
    }

    "must serialise from JSON with no email" in {
      val result = Json.parse("""{"paperlessPreference":"1"}""").validate[SubscriptionContactPreferences]
      result mustBe JsSuccess(underTest.copy(emailAddress = None))
    }
  }
}
