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
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class ContactPreferenceInputSpec  extends AnyFreeSpec with Matchers with TestData with SpecBase {
  
  val json = """{"emailAddressInput":"email","confirmPostalAddress":"address"}"""
    
  "ContactPreferenceInput" - {
    
    "must serialise to json" in {
      val result = ContactPreferenceInput("email", "address")

      Json.toJson(result).toString mustBe json
    }

    "must serialise from json" in {
      val result = Json.parse(json).validate[ContactPreferenceInput].get

      result mustBe ContactPreferenceInput("email", "address")
    }
  }
}
