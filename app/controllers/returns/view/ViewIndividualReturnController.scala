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

import connectors.returns.GetReturnsConnector
import controllers.actions.ApprovedVapingManufacturerAuthAction
import controllers.actions.returns.*
import models.identifiers.PeriodKey
import models.returns.view.ReturnDisplayResponse
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.ObligationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.view.ViewIndividualReturnViewModel
import views.html.returns.view.ViewIndividualReturnView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ViewIndividualReturnController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: ApprovedVapingManufacturerAuthAction,
                                       connector: GetReturnsConnector,
                                       obligationService: ObligationService,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: ViewIndividualReturnView,
                                       returnsEnabled: ReturnsEnabledAction
                                     )(using ExecutionContext) extends FrontendBaseController with I18nSupport with Logging{

  def onPageLoad(periodKey: PeriodKey): Action[AnyContent] = (identify andThen returnsEnabled).async {
    implicit request =>
      for {
        returnData <- connector.getReturn(periodKey, request.enrolmentVpdId)
        obligations <- obligationService.getObligations(request.enrolmentVpdId)
      } yield {
        val dutyRate = extractDutyRate(returnData)
        Ok(view(ViewIndividualReturnViewModel(returnData, dutyRate, obligations)))
      }
  }

  private def extractDutyRate(returnData: ReturnDisplayResponse): Option[BigDecimal] =
    returnData.success.vapingProductsProduced
      .flatMap(_.returns.headOption)
      .map(_.dutyRate)
}
