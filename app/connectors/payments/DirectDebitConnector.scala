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

package connectors.payments

import config.FrontendAppConfig
import models.payments.{StartDirectDebitRequest, StartDirectDebitResponse}
import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class DirectDebitConnector @Inject()(
  config: FrontendAppConfig,
  implicit val httpClient: HttpClientV2
)(using ExecutionContext)
  extends HttpReadsInstances
  with Logging {

  private val parsingError = "Parsing failed for start direct debit response"

  def startDirectDebit(request: StartDirectDebitRequest)
                      (using HeaderCarrier): Future[StartDirectDebitResponse] =
    httpClient
      .post(url"${config.startDirectDebitUrl}")
      .withBody(Json.toJson(request))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .recoverWith { case e: Exception =>
        logger.warn(s"Exception while starting direct debit journey: ${e.getMessage}")
        Future.failed(InternalServerException("Failed to start direct debit journey"))
      }
      .flatMap(getResponse)
      .flatMap(parseJson)

  private def getResponse(response: Either[UpstreamErrorResponse, HttpResponse]): Future[HttpResponse] = {
    response match {
      case Right(response) => Future.successful(response)
      case Left(error) =>
        logger.warn(s"Unexpected response from start direct debit API. Status: ${error.statusCode}")
        Future.failed(InternalServerException("Failed to start direct debit journey"))
    }
  }

  private def parseJson(response: HttpResponse) = {
    Try {
      response.json.as[StartDirectDebitResponse]
    } match {
      case Success(directDebitResponse) => Future.successful(directDebitResponse)
      case Failure(_) =>
        logger.warn(parsingError)
        Future.failed(InternalServerException(parsingError))
    }
  }
}
