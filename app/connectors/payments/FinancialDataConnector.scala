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
import models.identifiers.VpdId
import models.payments.OutstandingPayment
import play.api.Logging
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class FinancialDataConnector @Inject()(
  config: FrontendAppConfig,
  implicit val httpClient: HttpClientV2
)(using ExecutionContext)
  extends HttpReadsInstances
  with Logging {

  private val parsingError = "Parsing failed for outstanding payments response"

  def getOutstandingPayments(vpdId: VpdId)
    (using HeaderCarrier): Future[Seq[OutstandingPayment]] =
    httpClient
      .get(url"${config.getOutstandingPaymentsUrl(vpdId)}")
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .recoverWith { case e: Exception =>
        logger.warn(s"Exception while getting outstanding payments: ${e.getMessage}")
        Future.failed(InternalServerException("Failed to get outstanding payments"))
      }
      .flatMap(getResponse)
      .flatMap(parseJson)

  private def getResponse(response: Either[UpstreamErrorResponse, HttpResponse]): Future[HttpResponse] = {
    response match {
      case Right(response) => Future.successful(response)
      case Left(error) =>
        logger.warn(s"Unexpected response from outstanding payments API. Status: ${error.statusCode}")
        Future.failed(InternalServerException("Failed to get outstanding payments"))
    }
  }

  private def parseJson(response: HttpResponse) = {
    Try {
      response.json.as[Seq[OutstandingPayment]]
    } match {
      case Success(payments) => Future.successful(payments)
      case Failure(_) =>
        logger.warn(parsingError)
        Future.failed(InternalServerException(parsingError))
    }
  }
}