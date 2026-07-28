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

package controllers.testonly

import connectors.testonly.TestFinancialDataConnector
import controllers.actions.ApprovedVapingManufacturerAuthAction
import models.identifiers.VpdId
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TestFinancialDataController @Inject()(
                                             val controllerComponents: MessagesControllerComponents,
                                             authAction: ApprovedVapingManufacturerAuthAction,
                                             connector: TestFinancialDataConnector
                                           )(implicit ec: ExecutionContext) extends FrontendBaseController {

  def setScenario(vpdId: String, scenario: String): Action[AnyContent] =
    authAction.async { implicit request =>
      connector.setScenario(VpdId(vpdId), scenario).map { response =>
        Status(response.status)(response.body)
      }
    }

  def clearVpdIdFinancialData(vpdId: String): Action[AnyContent] =
    authAction.async { implicit request =>
      connector.clearVpdIdFinancialData(VpdId(vpdId)).map { response =>
        Status(response.status)(response.body)
      }
    }

  def clearAllFinancialData(): Action[AnyContent] =
    authAction.async { implicit request =>
      connector.clearAllFinancialData().map { response =>
        Status(response.status)(response.body)
      }
    }

  def setCustomFinancialData(vpdId: String): Action[JsValue] =
    authAction(parse.json).async { implicit request =>
      connector.setCustomFinancialData(VpdId(vpdId), request.body).map { response =>
        Status(response.status)(response.body)
      }
    }
}
