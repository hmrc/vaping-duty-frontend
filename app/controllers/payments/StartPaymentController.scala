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
import controllers.actions.ApprovedVapingManufacturerAuthAction
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.payments.PaymentService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class StartPaymentController @Inject()(
  identify: ApprovedVapingManufacturerAuthAction,
  paymentService: PaymentService,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents
)(using ExecutionContext) extends FrontendBaseController with Logging {

  def startPayment(chargeReference: String): Action[AnyContent] =
    identify.async { implicit request =>
      val returnUrl = s"${config.host}${controllers.payments.routes.ViewPaymentsController.onPageLoad().url}"
      val backUrl = s"${config.host}${controllers.payments.routes.ViewPaymentsController.onPageLoad().url}"

      paymentService.startPayment(
        request.enrolmentVpdId,
        chargeReference,
        returnUrl,
        backUrl
      ).map { response =>
        Redirect(response.nextUrl)
      }.recover {
        case e: Exception =>
          logger.warn(s"Error starting payment for charge reference $chargeReference: ${e.getMessage}")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }
}
