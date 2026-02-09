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
import forms.HowToBeContactedFormProvider
import models.{ContactPreferenceUserAnswers, Mode, UserDetails}
import navigation.Navigator
import pages.contactPreference.HowToBeContactedPage
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.UserAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.contactPreference.HowToBeContactedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HowToBeContactedController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            sessionService: UserAnswersService,
                                            navigator: Navigator,
                                            identify: ApprovedVapingManufacturerAuthAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            formProvider: HowToBeContactedFormProvider,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: HowToBeContactedView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>
      
      request.userAnswers match {
        case Some(ua) => Future.successful(Ok(view(prepareForm(ua), mode)))
        case None =>
          sessionService.createUserAnswers(UserDetails(request.vpdId, request.userId)).map {
            case Left(error) =>
              logger.info(s"[HowToBeContactedController][onPageLoad] Creating user answers failed: ${error.message}")
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            case Right(response) => Ok(view(prepareForm(response), mode))
          }
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(HowToBeContactedPage, value))
            _              <- sessionService.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(HowToBeContactedPage, mode, updatedAnswers))
      )
  }

  private def prepareForm(ua: ContactPreferenceUserAnswers) = {
    ua.get(HowToBeContactedPage) match {
      case None => form
      case Some(value) => form.fill(value)
    }
  }
}
