/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.enrolment

import config.FrontendAppConfig
import controllers.actions.*
import forms.enrolment.UserHasApprovalIdFormProvider
import models.enrolment.EnrolmentUserAnswers
import models.requests.EnrolmentOptionalDataRequest
import pages.enrolment.UserHasApprovalIdPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.EnrolmentUserAnswersRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.enrolment.UserHasApprovalIdView

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserHasApprovalIdController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             isAuthenticated: EnrolmentClaimAuthAction,
                                             hasNoEnrolment: NoEnrolmentAction,
                                             formProvider: UserHasApprovalIdFormProvider,
                                             getData: EnrolmentDataRetrievalAction,
                                             enrolmentRepository: EnrolmentUserAnswersRepository,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: UserHasApprovalIdView,
                                             config: FrontendAppConfig
                                            )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (isAuthenticated andThen hasNoEnrolment andThen getData) {
    implicit request =>

      val preparedForm = request.userAnswers.flatMap(_.get(UserHasApprovalIdPage))
        .fold(form)(form.fill)

      Ok(view(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = (isAuthenticated andThen hasNoEnrolment andThen getData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors))),

        userHasVpdEnrolmentId =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.getOrElse(emptyAnswers(request))
              .set(UserHasApprovalIdPage, userHasVpdEnrolmentId))
            _              <- enrolmentRepository.set(updatedAnswers)
          } yield {
            if (userHasVpdEnrolmentId) {
              Redirect(config.eacdEnrolmentClaimRedirectUrl)
            } else {
              Redirect(controllers.enrolment.routes.UserDoesNotHaveApprovalIdController.onPageLoad().url)
            }
          }
      )
  }

  private def emptyAnswers(request: EnrolmentOptionalDataRequest[?]) = {
    EnrolmentUserAnswers(request.userId, Json.obj(), Instant.now(), Instant.now())
  }
}
