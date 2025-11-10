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
import play.api.Logging
import play.api.http.Status.{OK, UNAUTHORIZED}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.UnauthorizedException

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class VapingDutyStubsConnectorHttp @Inject() (
                                          config: FrontendAppConfig,
                                          implicit val httpClient: HttpClientV2
                                        )(implicit ec: ExecutionContext)
  extends VapingDutyStubsConnector
    with HttpReadsInstances
    with Logging {
  override def ping()(implicit hc: HeaderCarrier): Future[Unit] =
    httpClient
      .get(url"${config.getStubsUrl()}")
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(response) if response.status == OK => ()
        case Right(response) => throw new Exception(s"Unexpected status code: ${response.status}")
        case Left(errorResponse) if errorResponse.statusCode == UNAUTHORIZED =>
          throw new UnauthorizedException(s"Not authorised: ${errorResponse.message}")
        case Left(errorResponse) => throw new Exception(s"Unexpected response: ${errorResponse.message}")
      }
}
