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

package connectors.returns

import config.FrontendAppConfig
import models.identifiers.VpdId
import models.returns.ObligationsResponse
import play.api.Logging
import play.api.http.Status.*
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ObligationsConnector @Inject()(
                                      config: FrontendAppConfig,
                                      implicit val httpClient: HttpClientV2
                                    )
                                    (using ExecutionContext) extends HttpReadsInstances with Logging {

  def getObligations(vpdId: VpdId)
                    (using HeaderCarrier): Future[ObligationsResponse] =

    httpClient
      .get(url"${config.getObligationsUrl(vpdId)}")
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case OK =>
            Try {
              response.json.as[ObligationsResponse]
            } match {
              case Success(doc) =>
                Future.successful(doc)
              case Failure(error) =>
                Future.failed(InternalServerException("Parsing failed for obligations response"))
            }
          case _ =>
            Future.failed(InternalServerException("Failed to retrieve obligations"))
        }
      }
}
