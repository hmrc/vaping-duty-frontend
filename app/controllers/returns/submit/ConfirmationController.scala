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

import config.FrontendAppConfig
import connectors.SubscriptionConnector
import controllers.actions.ApprovedVapingManufacturerAuthAction
import controllers.actions.returns.*
import models.BtaLink
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.submit.ConfirmationViewModel
import views.html.returns.submit.ConfirmationEmailView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConfirmationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: ApprovedVapingManufacturerAuthAction,
                                       getData: ReturnsDataRetrievalAction,
                                       requireData: ReturnsDataRequiredAction,
                                       returnsEnabled: ReturnsEnabledAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: ConfirmationEmailView,
                                       subscriptionConnector: SubscriptionConnector,
                                       config: FrontendAppConfig
                                     )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(vpdRef: Option[String]): Action[AnyContent] = (identify andThen returnsEnabled andThen getData andThen requireData).async {
    implicit request =>
      subscriptionConnector.getSubscriptionContactPreferences(request.enrolmentVpdId).map {
        case Left(_) => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Right(value) =>
          val email = value.emailAddress.getOrElse("")
          val btaUrl = BtaLink(config)
          val vm = ConfirmationViewModel(request.userAnswers, email, vpdRef.fold("")(ref => ref), btaUrl)

          Ok(view(vm))
      }
  }
}
