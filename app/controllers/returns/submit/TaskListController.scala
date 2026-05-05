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

import controllers.actions.*
import controllers.actions.returns.{ReturnsDataRequiredAction, ReturnsDataRetrievalAction, ReturnsEnabledAction}
import models.returns.ReturnsUserAnswers
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsObject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.submit.TaskListPageViewModel
import views.html.returns.submit.TaskListView

import java.time.Instant
import javax.inject.Inject

class TaskListController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    identify: ApprovedVapingManufacturerAuthAction,
                                    getData: ReturnsDataRetrievalAction,
                                    requireData: ReturnsDataRequiredAction,
                                    returnsEnabledAction: ReturnsEnabledAction,
                                    val controllerComponents: MessagesControllerComponents,
                                    view: TaskListView
                                     ) extends FrontendBaseController with I18nSupport {

  //controller is using empty UA until further tickets enable us to use Returns User Answers properly
  def onPageLoad: Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData) {
    implicit request =>
      val emptyreturnsUserAnswers: ReturnsUserAnswers = ReturnsUserAnswers(
        id = "",
        data = JsObject.empty,
        startedTime = Instant.now(),
        lastUpdated = Instant.now()
      )
      Ok(view(TaskListPageViewModel(request.userAnswers.getOrElse(emptyreturnsUserAnswers))))
  }
}