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

import controllers.actions.returns.*
import controllers.actions.ApprovedVapingManufacturerAuthAction
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import connectors.returns.{GetReturnsConnector, ReturnsUserAnswersConnector}
import models.returns.ReturnsUserAnswers
import viewmodels.returns.view.ViewIndividualReturnViewModel
import views.html.returns.view.ViewIndividualReturnView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ViewIndividualReturnController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: ApprovedVapingManufacturerAuthAction,
                                       connector: GetReturnsConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: ViewIndividualReturnView,
                                       returnsEnabled: ReturnsEnabledAction,
                                       returnsRepository: ReturnsUserAnswersConnector,
                                       getData: ReturnsDataRetrievalAction
                                     )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(periodKey: String): Action[AnyContent] = (identify andThen returnsEnabled andThen getData).async {
    implicit request =>
      connector.getReturn(periodKey, vpdId = request.enrolmentVpdId)
        .map { returnData =>
          val ua = request.userAnswers.fold(ReturnsUserAnswers.getEmptyReturnsUA(request.internalId))(ua => ua)
          returnsRepository.set(ua.copy(periodKey = Some(periodKey)))
          Ok(view(ViewIndividualReturnViewModel(returnData)))
        }
  }
}
