/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json._
import play.api.mvc.PathBindable

final case class ChargeReference(value: String) extends AnyVal {
  override def toString: String = value
}

object ChargeReference {
  given format: Format[ChargeReference] = new Format[ChargeReference] {
    override def reads(json: JsValue): JsResult[ChargeReference] =
      json.validate[String].map(ChargeReference(_))

    override def writes(o: ChargeReference): JsValue =
      JsString(o.value)
  }

  given pathBindable(using stringBinder: PathBindable[String]): PathBindable[ChargeReference] =
    new PathBindable[ChargeReference] {
      override def bind(key: String, value: String): Either[String, ChargeReference] =
        stringBinder.bind(key, value).map(ChargeReference(_))

      override def unbind(key: String, chargeRef: ChargeReference): String =
        stringBinder.unbind(key, chargeRef.value)
    }
}