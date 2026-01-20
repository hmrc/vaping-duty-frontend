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

package connectors

import config.FrontendAppConfig
import models.emailverification.{ErrorModel, PaperlessPreferenceSubmission, PaperlessPreferenceSubmittedResponse}
import play.api.Logging
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubmitPreferencesConnector @Inject() (
  config: FrontendAppConfig,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  def submitContactPreferences(contactPreferenceSubmission: PaperlessPreferenceSubmission, appaId: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorModel, PaperlessPreferenceSubmittedResponse]] = {
    httpClient
      .put(url"${config.ecpSubmitContactPreferencesUrl(appaId)}")
      .withBody(Json.toJson(contactPreferenceSubmission))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(response) if response.status == OK =>
          Try(response.json.as[PaperlessPreferenceSubmittedResponse]) match {
            case Success(successResponse) => Right(successResponse)
            case Failure(_)               =>
              logger.warn(
                "[SubmitPreferencesConnector] [submitContactPreferences] Invalid JSON format, failed to parse as PaperlessPreferenceSubmittedResponse"
              )
              Left(
                ErrorModel(
                  INTERNAL_SERVER_ERROR,
                  "Invalid JSON format. Could not parse response as PaperlessPreferenceSubmittedResponse"
                )
              )
          }
        case Left(errorResponse)                      =>
          logger.warn(
            s"[SubmitPreferencesConnector] [submitContactPreferences] Unexpected response when submitting contact preferences. Status: ${errorResponse.statusCode}"
          )
          Left(ErrorModel(errorResponse.statusCode, s"Unexpected response. Status: ${errorResponse.statusCode}"))
        case Right(response)                          =>
          logger.warn(
            s"[SubmitPreferencesConnector] [submitContactPreferences] Unexpected status code when submitting contact preferences: ${response.status}"
          )
          Left(
            ErrorModel(
              INTERNAL_SERVER_ERROR,
              s"Unexpected status code when submitting contact preferences: ${response.status}"
            )
          )
      }
  }

}
