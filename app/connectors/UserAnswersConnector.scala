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
import models.{ContactPreferenceUserAnswers, UserDetails}
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserAnswersConnector @Inject() (config: FrontendAppConfig,implicit val httpClient: HttpClientV2)
                                     (implicit ec: ExecutionContext) extends HttpReadsInstances {

  def get(vpdId: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, ContactPreferenceUserAnswers]] =
    httpClient
      .get(url"${config.ecpUserAnswersGetUrl(vpdId)}")
      .execute[Either[UpstreamErrorResponse, ContactPreferenceUserAnswers]]

  def set(userAnswers: ContactPreferenceUserAnswers)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    httpClient
      .put(url"${config.ecpUserAnswersUrl()}")
      .setHeader("Csrf-Token" -> "nocheck")
      .withBody(Json.toJson(userAnswers))
      .execute[HttpResponse]
  }

  def createUserAnswers(userDetails: UserDetails)
                       (implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, ContactPreferenceUserAnswers]] = {
    httpClient
      .post(url"${config.ecpUserAnswersUrl()}")
      .withBody(Json.toJson(userDetails))
      .setHeader("Csrf-Token" -> "nocheck")
      .execute[Either[UpstreamErrorResponse, ContactPreferenceUserAnswers]]
  }

  def keepAlive(vpdId: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] =
    httpClient
      .post(url"${config.ecpUserAnswersKeepAliveUrl()}")
      .setHeader("Csrf-Token" -> "nocheck")
      .execute[HttpResponse]
      .flatMap { response =>
        if (response.status == NO_CONTENT) {
          Future.successful(Right(()))
        } else {
          Future.successful(Left(UpstreamErrorResponse("keepAlive failed", response.status)))
        }
      }
  
  def clear(vpdId: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] =
    httpClient
      .delete(url"${config.ecpUserAnswersClearUrl(vpdId)}")
      .setHeader("Csrf-Token" -> "nocheck")
      .execute[HttpResponse]
      .flatMap { response =>
        if (response.status == NO_CONTENT) {
          Future.successful(Right(()))
        } else {
          Future.failed(UpstreamErrorResponse("clear failed", response.status))
        }
      }

}
