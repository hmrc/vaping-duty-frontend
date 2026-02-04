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

import cats.data.EitherT
import config.FrontendAppConfig
import models.emailverification.{EmailVerificationRequest, ErrorModel, GetVerificationStatusResponse, RedirectUri, VerificationDetails}
import play.api.Logging
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class EmailVerificationConnector @Inject()(
  config: FrontendAppConfig,
  implicit val httpClient: HttpClientV2
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

  def getEmailVerification(verificationDetails: VerificationDetails)
                          (implicit hc: HeaderCarrier):
  EitherT[Future, ErrorModel, GetVerificationStatusResponse] = EitherT {
    
    httpClient
      .get(url"${config.ecpGetEmailVerificationUrl(verificationDetails.credId)}")
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .flatMap {
        case Right(response)     =>
          Try(response.json.as[GetVerificationStatusResponse]) match {
            case Success(successResponse) => Future.successful(Right(successResponse))
            case Failure(_)               =>
              logger.warn(
                s"[EmailVerificationConnector] [getEmailVerification] Invalid JSON format, failed to parse as GetVerificationStatusResponse"
              )
              Future.successful(
                Left(
                  ErrorModel(
                    INTERNAL_SERVER_ERROR,
                    "Invalid JSON format. Could not parse response as GetVerificationStatusResponse"
                  )
                )
              )
          }
        case Left(errorResponse) =>
          logger.warn(
            s"[EmailVerificationConnector] [getEmailVerification] Unexpected response when retrieving email verification details. Status: ${errorResponse.statusCode}, Message: ${errorResponse.message}"
          )
          Future.successful(
            Left(ErrorModel(errorResponse.statusCode, s"Unexpected response. Status: ${errorResponse.statusCode}"))
          )
      }
  }

  def startEmailVerification(request: EmailVerificationRequest)
                            (implicit hc: HeaderCarrier): Future[Either[ErrorModel, RedirectUri]] = {

    val startEmailVerificationJourneyUrl = config.startEmailVerificationJourneyUrl
    httpClient
      .post(url"$startEmailVerificationJourneyUrl")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case CREATED =>
            Try(response.json.as[RedirectUri]) match {
              case Success(successResponse) =>
                logger.info(
                  "[EmailVerificationConnector] [startEmailVerification] Email verification url retrieved successfully"
                )
                Right(successResponse)
              case Failure(_)               =>
                logger.warn(
                  "[EmailVerificationConnector] [startEmailVerification] Invalid JSON format, failed to parse response as a RedirectUrl"
                )
                Left(
                  ErrorModel(INTERNAL_SERVER_ERROR, "Invalid JSON format, failed to parse response as a RedirectUrl")
                )
            }
          case _       =>
            // Not logging response body in case it contains the email address itself (PII)
            logger.warn(
              s"[EmailVerificationConnector] [startEmailVerification] Unexpected response from email verification service. Http status: ${response.status}"
            )
            Left(
              ErrorModel(
                response.status,
                s"Unexpected response from email verification service. Http status: ${response.status}"
              )
            )
        }
      }

  }
}
