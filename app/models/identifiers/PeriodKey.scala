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

package models.identifiers

import play.api.libs.json.{Json, OFormat, Reads, Writes}
import play.api.mvc.PathBindable

case class PeriodKey(value: String) {
  override def toString: String = value
}

object PeriodKey {
  private val periodKeyPattern = "^\\d{2}[A-Z]{2}$".r

  given PathBindable[PeriodKey] = new PathBindable[PeriodKey] {

    override def bind(key: String, value: String): Either[String, PeriodKey] = {
      validate(value)
    }

    override def unbind(key: String, periodKey: PeriodKey): String = periodKey.toString
  }

  private def validate(value: String) =
    if (periodKeyPattern.matches(value)) {
      Right(PeriodKey(value))
    } else {
      Left(s"Invalid PeriodKey format: $value. Expected format: YYPP (e.g., 24AA for Jan 2024)")
    }

  given OFormat[PeriodKey] = Json.format[PeriodKey]
}