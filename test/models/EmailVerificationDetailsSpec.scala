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
import models.emailverification.EmailVerificationDetails
import play.api.libs.json.Json

class EmailVerificationDetailsSpec extends SpecBase {
  val emailVerificationDetails: EmailVerificationDetails =
    EmailVerificationDetails(emailAddress, isVerified = true, isLocked = false)

  "EmailVerificationDetails" - {
    val json = s"""{"emailAddress":"$emailAddress","isVerified":true,"isLocked":false}"""

    "must serialise to json" in {
      Json.toJson(emailVerificationDetails).toString() mustBe json
    }

    "must deserialise from json" in {
      Json.parse(json).as[EmailVerificationDetails] mustBe emailVerificationDetails
    }
  }
}
