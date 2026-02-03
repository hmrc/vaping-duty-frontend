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
import play.api.libs.json.{JsResultException, Json}
import models.emailverification.JsonHelpers._

class JsonHelpersSpec extends SpecBase {
  "JsonHelpers must" - {
    Seq((false, """"0""""), (true, """"1"""")).foreach { case (v, code) =>
      s"deserialise the code $code to boolean $v" in {

        Json.parse(code).as[Boolean] mustBe v
      }

      s"serialise boolean $v to the code $code" in {

        Json.toJson(v).toString mustBe code
      }
    }

    "return an error if a read value is an invalid string" in {

      a[JsResultException] mustBe thrownBy(Json.parse(""""2"""").as[Boolean])
    }

    "return an error if a read value is an invalid type" in {

      a[JsResultException] mustBe thrownBy(Json.parse("""1""").as[Boolean])
    }
  }
}
