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
import controllers.actions.returns.*
import models.{CheckMode, Mode, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.submit.DutySuspenseCheckAnswersViewModel
import views.html.returns.submit.DutySuspenseCheckAnswersView

import javax.inject.Inject

class DutySuspenseCheckAnswersController @Inject()(
                                                    override val messagesApi: MessagesApi,
                                                    identify: ApprovedVapingManufacturerAuthAction,
                                                    getData: ReturnsDataRetrievalAction,
                                                    requireData: ReturnsDataRequiredAction,
                                                    returnsEnabled: ReturnsEnabledAction,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    view: DutySuspenseCheckAnswersView
                                                  ) extends FrontendBaseController with I18nSupport {

  def onPageLoad(mode: Mode = NormalMode): Action[AnyContent] = (identify andThen returnsEnabled andThen getData andThen requireData) {
    implicit request =>
      val pk = request.periodKey

      DutySuspenseCheckAnswersViewModel(request.userAnswers, pk, mode) match {
        case Some(vm) => Ok(view(pk, vm, mode))
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(mode: Mode = NormalMode): Action[AnyContent] = (identify andThen returnsEnabled andThen getData andThen requireData) {
    implicit request =>
      mode match {
        case CheckMode => Redirect(controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url + s"?period=${request.periodKey.value}")
        case NormalMode => Redirect(controllers.returns.submit.routes.TaskListController.onPageLoad().url + s"?period=${request.periodKey.value}")
      }
  }
}
