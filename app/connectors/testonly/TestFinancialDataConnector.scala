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

package connectors.testonly

import config.FrontendAppConfig
import models.identifiers.VpdId
import play.api.Logging
import play.api.libs.json.JsValue
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TestFinancialDataConnector @Inject()(
  config: FrontendAppConfig,
  httpClient: HttpClientV2
)(implicit ec: ExecutionContext) extends Logging {

  def setScenario(vpdId: VpdId, scenario: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    logger.info(s"Test endpoint: Setting financial data scenario '$scenario' for VPD ID $vpdId")
    httpClient
      .post(url"${config.setFinancialDataScenarioUrl(vpdId, scenario)}")
      .execute[HttpResponse]
  }

  def clearVpdIdFinancialData(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    logger.info(s"Test endpoint: Clearing financial data for VPD ID $vpdId")
    httpClient
      .post(url"${config.clearVpdIdFinancialDataUrl(vpdId)}")
      .execute[HttpResponse]
  }

  def clearAllFinancialData()(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    logger.info("Test endpoint: Clearing all financial data")
    httpClient
      .post(url"${config.clearAllFinancialDataUrl}")
      .execute[HttpResponse]
  }

  def setCustomFinancialData(vpdId: VpdId, json: JsValue)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    logger.info(s"Test endpoint: Setting custom financial data for VPD ID $vpdId")
    httpClient
      .post(url"${config.setCustomFinancialDataUrl(vpdId)}")
      .withBody(json)
      .execute[HttpResponse]
  }
}
