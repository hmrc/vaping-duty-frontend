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

import config.FrontendAppConfig
import play.api.Logging
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserAllowListConnector @Inject()(
  config: FrontendAppConfig,
  httpClient: HttpClientV2
)(implicit ec: ExecutionContext) extends HttpReadsInstances with Logging {

  private val SERVICE_NAME = "vaping-duty-frontend"

  def check(vpdId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val url = config.userAllowListUrl(vpdId)
    
    httpClient
      .get(url"$url")
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap {
        case Right(response) =>
          parseResponse(response)
        case Left(UpstreamErrorResponse(_, NOT_FOUND, _, _)) =>
          logger.info(s"VPD ID $vpdId not found in user allow list - returning false")
          Future.successful(false)
        case Left(error) =>
          logger.warn(s"Unexpected response from user-allow-list service for VPD ID $vpdId. Status: ${error.statusCode}")
          Future.failed(UserAllowListConnector.UnexpectedResponseException(
            s"User allow list check failed with status ${error.statusCode}"
          ))
      }
      .recoverWith {
        case e: Exception if !e.isInstanceOf[UserAllowListConnector.UnexpectedResponseException] =>
          logger.warn(s"Exception while checking user allow list for VPD ID $vpdId: ${e.getMessage}")
          Future.failed(UserAllowListConnector.UnexpectedResponseException(
            s"User allow list check failed: ${e.getMessage}"
          ))
      }
  }

  private def parseResponse(response: HttpResponse): Future[Boolean] = {
    try {
      val result = response.json.as[Boolean]
      Future.successful(result)
    } catch {
      case e: Exception =>
        logger.warn(s"Failed to parse user allow list response: ${e.getMessage}")
        Future.failed(UserAllowListConnector.UnexpectedResponseException(
          "Failed to parse user allow list response"
        ))
    }
  }
}

object UserAllowListConnector {
  case class UnexpectedResponseException(message: String) extends Exception(message)
}
