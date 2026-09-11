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

package controllers.payments

import config.FrontendAppConfig
import controllers.actions.{ApprovedVapingManufacturerAuthAction, CheckInsolvencyAction}
import controllers.actions.returns.ReturnsEnabledAction
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.payments.PaymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StartBtaPaymentController @Inject()(
                                           identify: ApprovedVapingManufacturerAuthAction,
                                           returnsEnabled: ReturnsEnabledAction,
                                           checkInsolvencyAction: CheckInsolvencyAction,
                                           paymentService: PaymentService,
                                           config: FrontendAppConfig,
                                           val controllerComponents: MessagesControllerComponents
                                         )(using ExecutionContext) extends FrontendBaseController with Logging {

  def startBtaPayment(): Action[AnyContent] =
    (identify andThen checkInsolvencyAction andThen returnsEnabled).async { implicit request =>
      paymentService.startBtaPayment(request.enrolmentVpdId, config.continueToBta, config.continueToBta).map { response =>
        Redirect(response.nextUrl)
      }.recover {
        case e: Exception =>
          logger.warn(s"Error starting BTA payment: ${e.getMessage}")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

  def startBtaPaymentForCharge(chargeReference: String): Action[AnyContent] =
    (identify andThen checkInsolvencyAction andThen returnsEnabled).async { implicit request =>
      paymentService.startPayment(request.enrolmentVpdId, chargeReference, config.continueToBta, config.continueToBta).map { response =>
        Redirect(response.nextUrl)
      }.recover {
        case e: Exception =>
          logger.warn(s"Error starting BTA payment for charge reference $chargeReference: ${e.getMessage}")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }
}
