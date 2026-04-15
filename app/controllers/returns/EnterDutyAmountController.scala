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

import controllers.actions.ApprovedVapingManufacturerAuthAction
import controllers.actions.returns.*
import forms.returns.EnterDutyAmountFormProvider
import models.Mode
import navigation.ReturnsNavigator
import pages.EnterDutyAmountPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.ReturnsUserAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.returns.EnterDutyAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnterDutyAmountController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: ReturnsUserAnswersService,
                                        navigator: ReturnsNavigator,
                                        identify: ApprovedVapingManufacturerAuthAction,
                                        getData: ReturnsDataRetrievalAction,
                                        requireData: ReturnsDataRequiredAction,
                                        formProvider: EnterDutyAmountFormProvider,
                                        returnsEnabledAction: ReturnsEnabledAction,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: EnterDutyAmountView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[Int] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify ancThen returnsEnabledAction andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(EnterDutyAmountPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(EnterDutyAmountPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(EnterDutyAmountPage, mode, updatedAnswers))
      )
  }
}
