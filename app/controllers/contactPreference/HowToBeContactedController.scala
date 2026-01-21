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

import connectors.UserAnswersConnector
import controllers.actions.*
import forms.HowToBeContactedFormProvider
import models.{ContactPreferenceUserAnswers, Mode, SubscriptionSummary, UserAnswers}
import navigation.Navigator
import pages.contactPreference.HowToBeContactedPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsObject
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.contactPreference.HowToBeContactedView

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HowToBeContactedController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: UserAnswersConnector,
                                       navigator: Navigator,
                                       identify: ApprovedVapingManufacturerAuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: HowToBeContactedFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: HowToBeContactedView
                                     )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) {
    implicit request =>

      val preparedForm = request.userAnswers.getOrElse(ContactPreferenceUserAnswers("vpdId", request.userId, SubscriptionSummary(true, None, None, None, correspondenceAddress = "", None), None, Set.empty, JsObject.empty, Instant.now(), Instant.now())).get(HowToBeContactedPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.getOrElse(ContactPreferenceUserAnswers("vpdId", request.userId, SubscriptionSummary(true, None, None, None, correspondenceAddress = "", None), None, Set.empty, JsObject.empty, Instant.now(), Instant.now())).set(HowToBeContactedPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(HowToBeContactedPage, mode, updatedAnswers))
      )
  }
}
