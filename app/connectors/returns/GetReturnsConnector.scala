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
import models.returns.view.ReturnDisplayResponse
import play.api.Logging
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class GetReturnsConnector @Inject()(
                                     config: FrontendAppConfig,
                                     implicit val httpClient: HttpClientV2
                                   )(using ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  private val parsingError = "Parsing failed for VPD return get response"

  def getReturn(periodKey: String, vpdId: VpdId)
               (using HeaderCarrier): Future[ReturnDisplayResponse] =
    httpClient
      .get(url"${config.getReturnUrl(vpdId, periodKey)}")
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .recoverWith { case e: Exception =>
        logger.warn(s"Exception while getting return: ${e.getMessage}")
        Future.failed(InternalServerException("Failed to get return"))
      }
      .flatMap(getResponse)
      .flatMap(parseJson)

  private def getResponse(response: Either[UpstreamErrorResponse, HttpResponse]): Future[HttpResponse] = {
    response match {
      case Right(response) => Future.successful(response)
      case Left(error) =>
        logger.warn(s"Unexpected response from VPD return get API. Status: ${error.statusCode}")
        Future.failed(InternalServerException("Failed to get VPD return"))
    }
  }

  private def parseJson(response: HttpResponse) = {
    Try {
      response.json.as[ReturnDisplayResponse]
    } match {
      case Success(returnsData) => Future.successful(returnsData)
      case Failure(_) =>
        logger.warn(parsingError)
        Future.failed(InternalServerException(parsingError))
    }
  }
}
