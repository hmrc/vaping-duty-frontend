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
import forms.returns.EnterDutySuspenseFormProvider
import models.Mode
import navigation.ReturnsNavigator
import pages.returns.EnterDutySuspensePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.ReturnsUserAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.returns.submit.EnterDutySuspenseView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnterDutySuspenseController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: ReturnsUserAnswersService,
                                        navigator: ReturnsNavigator,
                                        identify: ApprovedVapingManufacturerAuthAction,
                                        getData: ReturnsDataRetrievalAction,
                                        requireData: ReturnsDataRequiredAction,
                                        formProvider: EnterDutySuspenseFormProvider,
                                        returnsEnabledAction: ReturnsEnabledAction,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: EnterDutySuspenseView
                                      )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>
      formProvider(request.periodKey, request.enrolmentVpdId).map { form =>
        val preparedForm = request.userAnswers.get(EnterDutySuspensePage) match {
          case None        => form
          case Some(value) => form.fill(value)
        }
        Ok(view(request.periodKey, preparedForm, mode))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>
      formProvider(request.periodKey, request.enrolmentVpdId).flatMap { form =>
        form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(request.periodKey, formWithErrors, mode))),

          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(EnterDutySuspensePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(EnterDutySuspensePage, mode, updatedAnswers))
        )
      }
  }
}
