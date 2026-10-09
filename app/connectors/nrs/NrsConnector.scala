/*
 * Copyright 2026 HM Revenue & Customs
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

package connectors.nrs

import config.FrontendAppConfig
import models.nrs.NrsSubmissionWorkItem
import play.api.Logging
import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsConnector @Inject()(
                              config: FrontendAppConfig,
                              httpClient: HttpClientV2
                            )(implicit ec: ExecutionContext) extends Logging {

  def submitNrsWorkItem(workItem: NrsSubmissionWorkItem)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = config.nrsSubmissionUrl

    httpClient
      .post(url"$url")
      .withBody(Json.toJson(workItem))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case ACCEPTED =>
            logger.info("NRS work item successfully submitted")
            Future.successful(())
          case status =>
            logger.warn(s"NRS submission returned unexpected status: $status")
            Future.failed(UpstreamErrorResponse(s"NRS submission failed with status $status", status))
        }
      }
  }
}
