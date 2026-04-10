/*
 * Copyright 2024 HM Revenue & Customs
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
import models.contactPreference.SubscriptionContactPreferences
import models.identifiers.VpdId
import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import play.api.http.Status.*
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubscriptionConnector @Inject() (
                                        config: FrontendAppConfig,
                                        implicit val system: ActorSystem,
                                        implicit val httpClient: HttpClientV2
                                      )
                                      (implicit ec: ExecutionContext) extends HttpReadsInstances with Logging {

  def getSubscriptionContactPreferences(vpdId: VpdId)
                                       (implicit hc: HeaderCarrier): Future[Either[ErrorResponse, SubscriptionContactPreferences]] =

    httpClient
      .get(url"${config.getSubscriptionUrl(vpdId)}")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            Try {
              response.json.as[SubscriptionContactPreferences]
            } match {
              case Success(doc)   =>
                Future.successful(Right(doc))
              case Failure(error) =>
                Future.successful(Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unable to parse subscription summary success")))
            }
          case _  =>
            Future.successful(Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Unexpected response")))
        }
      }
}
