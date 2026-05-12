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

import connectors.returns.ReturnsUserAnswersConnector
import controllers.actions.*
import controllers.actions.contactPreference.DataRetrievalAction
import controllers.actions.returns.{ReturnsDataRetrievalAction, ReturnsEnabledAction}
import models.returns.ReturnsUserAnswers
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.submit.BeforeYouStartViewModel
import views.html.returns.submit.BeforeYouStartView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class BeforeYouStartController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          identify: ApprovedVapingManufacturerAuthAction,
                                          sessionRepository: ReturnsUserAnswersConnector,
                                          returnsEnabledAction: ReturnsEnabledAction,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: BeforeYouStartView,
                                          getData: ReturnsDataRetrievalAction
                                     )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData).async {
    implicit request =>
      val pk = request.getQueryString("period").getOrElse("")
      val session = request.session + ("periodKey" -> pk)

      val ua = request.userAnswers match {
        case Some(existingUa) if existingUa.periodKey.contains(pk) =>
          existingUa
        case _ =>
          ReturnsUserAnswers.getEmptyReturnsUA(request.enrolmentVpdId, pk)
      }

      sessionRepository.set(ua.copy(periodKey = Some(pk))).map(_ =>
        Ok(view(BeforeYouStartViewModel())).withSession(session)
      )

  }
}
