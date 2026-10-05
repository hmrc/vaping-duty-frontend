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
import models.identifiers.{PeriodKey, VpdId}
import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, InternalServerException, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubmitReturnConnector @Inject()(config: FrontendAppConfig,
                                      implicit val httpClient: HttpClientV2)
                                     (implicit ec: ExecutionContext) extends HttpReadsInstances with Logging {

  def submitReturn(returnsSubmission: ReturnCreateRequest,
                   vpdId: VpdId)
                  (implicit hc: HeaderCarrier): Future[ReturnSubmittedResponse] = {
    httpClient
      .post(url"${config.submitReturnUrl(vpdId, PeriodKey(returnsSubmission.periodKey))}")
      .withBody(Json.toJson(returnsSubmission))
      .injectNrsHeaders
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap(response => submitReturnsParser(response))
      .recoverWith { case _: Exception =>
        logger.warn("An exception was returned while trying to submit return")
        Future.failed(InternalServerException("Failed to submit return"))
      }
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

extension (baseRequest: RequestBuilder)
  // Prepare the headers elements needed for NRS submission as HttpClientV2 removes/writes over some of the needed data
  def injectNrsHeaders(using hc: HeaderCarrier): RequestBuilder = {
    // Headers that should NOT be forwarded as they're set by the HTTP client or would corrupt the request
    val headersToExclude = Set(
      "content-type",
      "content-length",
      "host",
      "connection",
      "timeout-access",
      "raw-request-uri",
      "tls-session-info",
      "path"
    )

    // Filter out HTTP-level headers, keep only application/business headers for NRS
    val safeHeaders = hc.otherHeaders.filterNot { case (name, _) =>
      headersToExclude.contains(name.toLowerCase)
    }

    // Chain the safe headers
    safeHeaders.foldLeft(baseRequest) { case (req, (name, value)) =>
      req.setHeader(name -> value)
    }
  }
