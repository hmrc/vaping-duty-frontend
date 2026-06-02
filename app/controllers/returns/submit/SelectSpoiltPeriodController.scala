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

package controllers.returns.submit

import controllers.actions.ApprovedVapingManufacturerAuthAction
import controllers.actions.contactPreference.{DataRequiredAction, DataRetrievalAction}
import controllers.actions.returns.ReturnsEnabledAction
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.ObligationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.submit.SelectSpoiltPeriodViewModel
import views.html.returns.submit.SelectSpoiltPeriodView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SelectSpoiltPeriodController @Inject()(
  override val messagesApi: MessagesApi,
  identify: ApprovedVapingManufacturerAuthAction,
  returnsEnabledAction: ReturnsEnabledAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  obligationService: ObligationService,
  val controllerComponents: MessagesControllerComponents,
  view: SelectSpoiltPeriodView
)(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(year: Option[Int]): Action[AnyContent] =
    (identify andThen returnsEnabledAction andThen getData andThen requireData).async { implicit request =>

      obligationService.getObligations(request.enrolmentVpdId).map { obligationsResponse =>
        val viewModel = SelectSpoiltPeriodViewModel(obligationsResponse, year)
        Ok(view(viewModel))
      }.recover {
        case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }
}