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

package connectors.email

import config.FrontendAppConfig
import models.email.Email
import play.api.Logging
import play.api.http.Status.{ACCEPTED, BAD_REQUEST}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class EmailConnector @Inject()(config: FrontendAppConfig, httpClient: HttpClientV2)(using ExecutionContext)
  extends Logging {

  def postEmail(email: Email, emailType: String)(using HeaderCarrier): Future[Unit] =
    httpClient
      .post(url"${config.sendEmailUrl}")
      .withBody(Json.toJson(email))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case ACCEPTED =>
            logger.info(s" HMRC email service: sent $emailType confirmation email")
          case BAD_REQUEST =>
            logger.warn(s"Non critical - Error from HMRC email service: status=400 sending $emailType confirmation email")
          case status =>
            logger.warn(s"Non critical - Unexpected response from HMRC email service: status=$status sending $emailType confirmation email")
        }
      }
      .recover { case NonFatal(e) =>
        logger.warn(s"Unable to send $emailType confirmation email: ${e.getClass.getSimpleName}")
      }
}
