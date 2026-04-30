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
import models.identifiers.VpdId
import models.returns.{ReturnCreateRequest, ReturnSubmittedResponse}
import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, InternalServerException, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubmitReturnConnector @Inject()(config: FrontendAppConfig,
                                      implicit val httpClient: HttpClientV2)
                                     (implicit ec: ExecutionContext) extends HttpReadsInstances with Logging {

  def submitReturn(returnsSubmission: ReturnCreateRequest, vpdId: VpdId)
                              (implicit hc: HeaderCarrier): Future[ReturnSubmittedResponse] =
    httpClient
      .post(url"${config.submitReturnUrl(vpdId)}")
      .withBody(Json.toJson(returnsSubmission))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap(response => submitReturnsParser(response))
      .recoverWith { case _: Exception =>
        logger.warn("An exception was returned while trying to submit return")
        Future.failed(InternalServerException("Failed to submit return"))
      }

  private def submitReturnsParser(response: Either[UpstreamErrorResponse, HttpResponse]): Future[ReturnSubmittedResponse] = {
    response match {
      case Right(response) =>
          Try{
            response.json.as[ReturnSubmittedResponse]
          } match {
            case Success(submissionResponse: ReturnSubmittedResponse) =>
              Future.successful(submissionResponse)
            case Failure(_) =>
              logger.warn("Parsing failed for submission response")
              Future.failed(InternalServerException("Failed to submit return"))
          }
      case Left(error) =>
            logger.warn(s"Unexpected response from return submission API. Status: ${error.statusCode}")
            Future.failed(InternalServerException("Failed to submit return"))
    }
  }
}
