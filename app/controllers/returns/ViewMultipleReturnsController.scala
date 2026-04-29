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

package controllers.returns

import controllers.actions.*
import controllers.actions.returns.ReturnsEnabledAction
import models.returns.ObligationsResponse
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.ObligationsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.ViewMultipleReturnsViewModel
import views.html.returns.ViewMultipleReturnsView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ViewMultipleReturnsController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               identify: ApprovedVapingManufacturerAuthAction,
                                               returnsEnabledAction: ReturnsEnabledAction,
                                               obligationsService: ObligationsService,
                                               val controllerComponents: MessagesControllerComponents,
                                               view: ViewMultipleReturnsView
                                             )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen returnsEnabledAction).async {
    implicit request =>

      obligationsService.get(request.enrolmentVpdId).map {
        case Left(_) => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Right(obligationResponse) => Ok(view(ViewMultipleReturnsViewModel(obligationResponse)))
      }

  }
}