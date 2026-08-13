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

package controllers.payments

import config.FrontendAppConfig
import connectors.payments.DirectDebitConnector
import controllers.actions.ApprovedVapingManufacturerAuthAction
import controllers.actions.returns.ReturnsEnabledAction
import models.payments.StartDirectDebitRequest
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StartDirectDebitController @Inject()(
  identify: ApprovedVapingManufacturerAuthAction,
  returnsEnabled: ReturnsEnabledAction,
  connector: DirectDebitConnector,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents
)(using ExecutionContext) extends FrontendBaseController with Logging {

  def startDirectDebit(): Action[AnyContent] =
    (identify andThen returnsEnabled).async { implicit request =>
      val backUrl = s"${config.host}${controllers.returns.submit.routes.ConfirmationController.onPageLoad().url}"
      val startRequest = StartDirectDebitRequest(returnUrl = config.continueToBta, backUrl = backUrl)

      connector.startDirectDebit(startRequest).map { response =>
        Redirect(response.nextUrl)
      }.recover {
        case e: Exception =>
          logger.warn(s"Error starting direct debit journey: ${e.getMessage}")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }
}
