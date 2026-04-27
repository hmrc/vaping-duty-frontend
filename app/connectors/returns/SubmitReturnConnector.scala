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

package connectors.returns

import config.FrontendAppConfig
import models.emailverification.{ErrorModel, PaperlessPreferenceSubmission, PaperlessPreferenceSubmittedResponse}
import models.identifiers.VpdId
import models.returns.{ReturnCreateRequest, ReturnSubmittedResponse}
import play.api.Logging
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubmitReturnConnector @Inject()(config: FrontendAppConfig,
                                      implicit val httpClient: HttpClientV2)
                                     (implicit ec: ExecutionContext) extends HttpReadsInstances with Logging {

  def submitReturn(returnsSubmission: ReturnCreateRequest, vpdId: VpdId)
                  (implicit hc: HeaderCarrier): Future[Either[ErrorModel, ReturnSubmittedResponse]] = {
    httpClient
      .post(url"${config.submitReturnUrl(vpdId)}")
      .withBody(Json.toJson(returnsSubmission))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(response) if response.status == OK =>
          Try(response.json.as[ReturnSubmittedResponse]) match {
            case Success(successResponse) => Right(successResponse)
            case Failure(_)               =>
              logger.warn("Invalid JSON format, failed to parse as ...")
              Left(ErrorModel(INTERNAL_SERVER_ERROR, "Invalid JSON format. Could not parse response as ..."))
          }
        case Left(errorResponse)                      =>
          logger.warn(s"Unexpected response when submitting return. Status: ${errorResponse.statusCode}")
          Left(ErrorModel(errorResponse.statusCode, s"Unexpected response. Status: ${errorResponse.statusCode}"))
        case Right(response)                          =>
          logger.warn(s"Unexpected status code when submitting return: ${response.status}")
          Left(
            ErrorModel(INTERNAL_SERVER_ERROR, s"Unexpected status code when submitting return: ${response.status}")
          )
      }
  }
}
