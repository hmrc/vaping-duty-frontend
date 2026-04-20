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
import models.identifiers.InternalId
import models.returns.ReturnsUserAnswers
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnsUserAnswersConnector @Inject()(config: FrontendAppConfig, implicit val httpClient: HttpClientV2)
                                           (implicit ec: ExecutionContext) extends HttpReadsInstances {

  def get(internalId: InternalId)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, ReturnsUserAnswers]] =
    httpClient
      .get(url"${config.returnsUserAnswersGetUrl(internalId)}")
      .execute[Either[UpstreamErrorResponse, ReturnsUserAnswers]]

  def set(userAnswers: ReturnsUserAnswers)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    httpClient
      .put(url"${config.returnsUserAnswersUrl}")
      .setHeader("Csrf-Token" -> "nocheck")
      .withBody(Json.toJson(userAnswers))
      .execute[HttpResponse]
  }

  def keepAlive(internalId: InternalId)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] =
    httpClient
      .post(url"${config.returnsUserAnswersKeepAliveUrl(internalId)}")
      .setHeader("Csrf-Token" -> "nocheck")
      .execute[HttpResponse]
      .flatMap(parseResponse(_, "keepAlive failed"))

  def clear(internalId: InternalId)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] =
    httpClient
      .delete(url"${config.returnsUserAnswersClearUrl(internalId)}")
      .setHeader("Csrf-Token" -> "nocheck")
      .execute[HttpResponse]
      .flatMap(parseResponse(_, "clear failed"))


  private def parseResponse(response: HttpResponse, message: String) = {
    if (response.status == NO_CONTENT) {
      Future.successful(Right(()))
    } else {
      Future.successful(Left(UpstreamErrorResponse(message, response.status)))
    }
  }
}
