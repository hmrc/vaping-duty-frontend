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

package connectors

import com.google.inject.Inject
import config.Service
import connectors.UserAllowListConnector.UnexpectedResponseException
import models.identifiers.VpdId
import models.requests.CheckRequest
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.{Configuration, Logging}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

class UserAllowListConnector @Inject()(
                                        configuration: Configuration,
                                        httpClient: HttpClientV2
                                      )(implicit ec: ExecutionContext) extends Logging {

  private val userAllowListService: Service = configuration.get[Service]("microservice.services.user-allow-list")
  private val internalAuthToken: String = configuration.get[String]("internal-auth.token")

  def check(feature: String, vpdId: VpdId)(implicit hc: HeaderCarrier): Future[Boolean] =
    httpClient
      .post(url"$userAllowListService/user-allow-list/vaping-duty-frontend/$feature/check")
      .setHeader("Authorization" -> internalAuthToken)
      .withBody(Json.toJson(CheckRequest(vpdId.value)))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(true)
          case NOT_FOUND =>
            logger.info(s"VPD ID $vpdId not found in user allow list - returning false")
            Future.successful(false)
          case status =>
            logger.info(s"Unexpected response from user-allow-list service for VPD ID $vpdId. Status: $status")
            Future.failed(UnexpectedResponseException(status))
        }
      }
}

object UserAllowListConnector {

  final case class UnexpectedResponseException(status: Int) extends Exception with NoStackTrace {
    override def getMessage: String = s"Unexpected status: $status"
  }
}
