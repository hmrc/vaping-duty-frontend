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

import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import queries.{ContactPreferenceSettable, Gettable, Settable}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.{Failure, Success, Try}

final case class ContactPreferenceUserAnswers(
  vpdId: String,
  userId: String,
  subscriptionSummary: SubscriptionSummary,
  emailAddress: Option[String],
  verifiedEmailAddresses: Set[String],
  data: JsObject = Json.obj(),
  startedTime: Instant,
  lastUpdated: Instant,
  validUntil: Option[Instant] = None
) {

  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)

  def set[A](page: ContactPreferenceSettable[A], value: A)(implicit writes: Writes[A]): Try[ContactPreferenceUserAnswers] = {

    val updatedData = data.setObject(page.path, Json.toJson(value)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors)       =>
        Failure(JsResultException(errors))
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      page.cleanup(Some(value), updatedAnswers)
    }
  }

  def remove[A](page: ContactPreferenceSettable[A]): Try[ContactPreferenceUserAnswers] = {

    val updatedData = data.removeObject(page.path) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(_)            =>
        Success(data)
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      page.cleanup(None, updatedAnswers)
    }
  }
}

object ContactPreferenceUserAnswers {

  implicit val format: OFormat[ContactPreferenceUserAnswers] = (
    (__ \ "vpdId").format[String] and
      (__ \ "userId").format[String] and
      (__ \ "subscriptionSummary").format[SubscriptionSummary] and
      (__ \ "emailAddress").formatNullable[String] and
      (__ \ "verifiedEmailAddresses").format[Set[String]] and
      (__ \ "data").formatWithDefault[JsObject](Json.obj()) and
      (__ \ "startedTime").format(MongoJavatimeFormats.instantFormat) and
      (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat) and
      (__ \ "validUntil").formatNullable(MongoJavatimeFormats.instantFormat)
  )(ContactPreferenceUserAnswers.apply, o => Tuple.fromProductTyped(o))

}

case class SubscriptionSummary(
  paperlessReference: Boolean,
  emailAddress: Option[String],
  emailVerification: Option[Boolean],
  bouncedEmail: Option[Boolean],
  correspondenceAddress: String,
  countryCode: Option[String]
)

object SubscriptionSummary {
  implicit val subscriptionSummaryFormat: OFormat[SubscriptionSummary] =
    Json.format[SubscriptionSummary]
}
