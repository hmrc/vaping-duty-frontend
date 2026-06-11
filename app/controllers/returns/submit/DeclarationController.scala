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
import forms.returns.DeclarationFormProvider
import models.NormalMode
import models.requests.returns.ReturnsDataRequest
import models.returns.{DeclarationDetails, ReturnsUserAnswers}
import navigation.ReturnsNavigator
import pages.returns.DeclarationPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.returns.{ReturnsUserAnswersService, SubmitReturnService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.returns.submit.DeclarationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: ReturnsUserAnswersService,
                                       identify: ApprovedVapingManufacturerAuthAction,
                                       getData: ReturnsDataRetrievalAction,
                                       requireData: ReturnsDataRequiredAction,
                                       returnsEnabled: ReturnsEnabledAction,
                                       formProvider: DeclarationFormProvider,
                                       submitReturnService: SubmitReturnService,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: DeclarationView,
                                       navigator: ReturnsNavigator,
                                       userAnswersService: ReturnsUserAnswersService
                                     )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[DeclarationDetails] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen returnsEnabled andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(DeclarationPage) match {
        case None => form

        case Some(value) => form.fill(value)
      }

      Ok(view(request.periodKey, preparedForm))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen returnsEnabled andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(view(request.periodKey, formWithErrors))),

          value =>
            Future.fromTry(request.userAnswers.set(DeclarationPage, value))
        .flatMap { updatedAnswers =>
          sessionRepository.set(updatedAnswers).flatMap { _ =>
            submitAndContinue(updatedAnswers)
          }
        }
    )
  }

  private def submitAndContinue(updatedAnswers: ReturnsUserAnswers)(using request: ReturnsDataRequest[?]): Future[Result] = {
    submitReturnService.submit(updatedAnswers)
      .flatMap(_ => userAnswersService.clear(request.enrolmentVpdId, request.periodKey))
      .map(_ => Redirect(navigator.nextPage(DeclarationPage, NormalMode, updatedAnswers)))
      .recover { case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()) }
  }
}