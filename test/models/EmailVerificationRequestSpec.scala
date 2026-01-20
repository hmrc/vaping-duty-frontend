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
import models.emailverification.EmailVerificationRequest
import play.api.libs.json.Json

class EmailVerificationRequestSpec extends SpecBase {
  val emailVerificationRequest: EmailVerificationRequest = testEmailVerificationRequest

  "EmailVerificationRequest" - {
    val json =
      s"""{"credId":"cred-id","continueUrl":"/test-continue-url","origin":"testOrigin","deskproServiceName":"test-deskpro-name","accessibilityStatementUrl":"/test-accessibility-url","backUrl":"/test-back-url","email":{"address":"john.doe@example.com","enterUrl":"/test-enter-url"},"labels":{"cy":{"pageTitle":"testTitle","userFacingServiceName":"testServiceName"},"en":{"pageTitle":"testTitle2","userFacingServiceName":"testServiceName2"}},"lang":"en","useNewGovUkServiceNavigation":true}"""

    "must serialise to json" in {
      Json.toJson(emailVerificationRequest).toString() mustBe json
    }

    "must deserialise from json" in {
      Json.parse(json).as[EmailVerificationRequest] mustBe emailVerificationRequest
    }
  }
}
