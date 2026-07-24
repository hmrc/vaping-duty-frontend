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

import controllers.actions.ApprovedVapingManufacturerAuthAction
import controllers.routes
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.payments.FinancialDataService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.payments.ViewPaymentsViewModel
import views.html.payments.ViewPaymentsView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ViewPaymentsController @Inject()(
  override val messagesApi: MessagesApi,
  identify: ApprovedVapingManufacturerAuthAction,
  service: FinancialDataService,
  val controllerComponents: MessagesControllerComponents,
  view: ViewPaymentsView
)(using ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad: Action[AnyContent] = identify.async { implicit request =>
    service.getPayments(request.enrolmentVpdId)
      .map { payments =>
        val viewModel = ViewPaymentsViewModel(payments)
        Ok(view(viewModel))
      }
      .recover { case e: Exception =>
        logger.warn(s"Error retrieving payments: ${e.getMessage}")
        Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
  }
}