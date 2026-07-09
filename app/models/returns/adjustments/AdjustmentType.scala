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

package models.returns.adjustments

import models.{Enumerable, WithName}
import play.api.libs.json.{Format, Json, Reads, Writes}

sealed trait AdjustmentType extends WithName

object AdjustmentType extends Enumerable.Implicits {

  case object UnderDeclared extends WithName("underDeclared") with AdjustmentType

  case object OverDeclared extends WithName("overDeclared") with AdjustmentType

  val values: Seq[AdjustmentType] = Seq(
    UnderDeclared,
    OverDeclared
  )

  implicit val enumerable: Enumerable[AdjustmentType] =
    Enumerable(values.map(v => v.toString -> v): _*)

  implicit val reads: Reads[AdjustmentType] = Reads.StringReads.flatMap {
    case "underDeclared" => Reads.pure(UnderDeclared)
    case "overDeclared"  => Reads.pure(OverDeclared)
    case other           => Reads.failed(s"Unknown adjustment type: $other")
  }

  implicit val writes: Writes[AdjustmentType] = Writes { adjustmentType =>
    Json.toJson(adjustmentType.toString)
  }

  implicit val format: Format[AdjustmentType] = Format(reads, writes)
}