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
import models.returns.{ObligationDetails, ObligationItem, ObligationsResponse}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.ReturnsOverviewViewModel
import views.html.returns.ReturnsOverviewView

import java.time.LocalDate
import javax.inject.Inject

class ReturnsOverviewController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           identify: ApprovedVapingManufacturerAuthAction,
                                           returnsEnabledAction: ReturnsEnabledAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: ReturnsOverviewView
                                         ) extends FrontendBaseController with I18nSupport {

  def show: Action[AnyContent] = (identify andThen returnsEnabledAction) {
    implicit request =>
      
      // TODO: Replace with actual connector call when available
      val mockObligations = createMockObligationsResponse()
      
      Ok(view(ReturnsOverviewViewModel(mockObligations)))
  }

  // Mock data for demonstration - will be replaced with connector call
  private def createMockObligationsResponse(): ObligationsResponse = {
    val currentDate = LocalDate.now()
    
    ObligationsResponse(
      obligation = Seq(
        // Outstanding return - Due
        ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "O",
            iCFromDate = LocalDate.of(2027, 12, 1),
            iCToDate = LocalDate.of(2027, 12, 31),
            iCDateReceived = None,
            iCDueDate = currentDate.plusDays(10),
            periodKey = "27AL"
          )
        ),
        // Outstanding return - Overdue
        ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "O",
            iCFromDate = LocalDate.of(2027, 11, 1),
            iCToDate = LocalDate.of(2027, 11, 30),
            iCDateReceived = None,
            iCDueDate = currentDate.minusDays(5),
            periodKey = "27AK"
          )
        ),
        // Completed return
        ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "F",
            iCFromDate = LocalDate.of(2027, 10, 1),
            iCToDate = LocalDate.of(2027, 10, 31),
            iCDateReceived = Some(LocalDate.of(2027, 11, 15)),
            iCDueDate = LocalDate.of(2027, 11, 30),
            periodKey = "27AJ"
          )
        )
      )
    )
  }
}