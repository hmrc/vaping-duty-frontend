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

package controllers.contactPreference

import controllers.actions.*
import controllers.actions.contactPreference.{DataRequiredAction, DataRetrievalAction}
import forms.contactPreference.EnterEmailFormProvider
import models.contactPreference.EnterEmailOutcome
import models.contactPreference.EnterEmailOutcome.{VerificationStarted, Verified}
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.contactPreference.EnterEmailPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.contactPreference.EnterEmailService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.contactPreference.EnterEmailView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnterEmailController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      identify: ApprovedVapingManufacturerAuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: EnterEmailFormProvider,
                                      enterEmailService: EnterEmailService,
                                      navigator: Navigator,
                                      val controllerComponents: MessagesControllerComponents,
                                      view: EnterEmailView
                                    )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.emailAddress match {
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

        email =>
          enterEmailService.submitEmail(email, request.userAnswers, request.credId).map {
            case Right(Verified(updatedAnswers)) => Redirect(navigator.nextPage(EnterEmailPage, NormalMode, updatedAnswers))
            case Right(EnterEmailOutcome.Locked) => Redirect(controllers.contactPreference.routes.LockedEmailController.onPageLoad())
            case Right(VerificationStarted(url)) => Redirect(url)
            case Left(_)                         => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      )
  }
}
