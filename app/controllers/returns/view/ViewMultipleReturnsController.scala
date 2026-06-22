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

package controllers.returns.view

import controllers.actions.*
import controllers.actions.contactPreference.DataRetrievalAction
import controllers.actions.returns.ReturnsEnabledAction
import models.obligations.ObligationStatus
import models.requests.contactPreference.OptionalDataRequest
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.returns.ObligationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.view.ViewMultipleReturnsViewModel
import views.html.returns.view.ViewMultipleReturnsView

import java.time.{Clock, LocalDate}
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ViewMultipleReturnsController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               clock: Clock,
                                               identify: ApprovedVapingManufacturerAuthAction,
                                               returnsEnabledAction: ReturnsEnabledAction,
                                               obligationService: ObligationService,
                                               val controllerComponents: MessagesControllerComponents,
                                               getData: DataRetrievalAction,
                                               view: ViewMultipleReturnsView
                                             )(using ExecutionContext, Clock) extends FrontendBaseController with I18nSupport {

  def onPageLoad(year: Option[Int] = None): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData).async {
    implicit request =>

      renderView(request, year)
        .recover(_ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
  }

  private def renderView(request: OptionalDataRequest[?], year: Option[Int])
                        (using Messages, HeaderCarrier) = {
    
    given Request[?] = request.request
    
    obligationService.getObligations(request.enrolmentVpdId).map { obligationResponse =>
      val completedYears = obligationResponse.obligation
        .filter(_.obligationDetails.openOrFulfilledStatus == ObligationStatus.F.toString)
        .map(_.obligationDetails.iCFromDate.getYear)
        .distinct
        .sorted
        .reverse

      val currentYear = year.getOrElse(
        completedYears.headOption.getOrElse(LocalDate.now(clock).getYear)
      )

      Ok(view(ViewMultipleReturnsViewModel(obligationResponse, currentYear)))
    }
  }
}
