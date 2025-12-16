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
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.enrolment.UserHasApprovalIdView

import javax.inject.Inject

class UserHasApprovalIdController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             isAuthenticated: OptEnrolmentAuthAction,
                                             hasNoEnrolment: NoEnrolmentAction,
                                             formProvider: UserHasApprovalIdFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: UserHasApprovalIdView,
                                             config: FrontendAppConfig
                                 )() extends FrontendBaseController with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (isAuthenticated andThen hasNoEnrolment) {
    implicit request =>
      Ok(view(form))
  }

  def onSubmit(): Action[AnyContent] = (isAuthenticated andThen hasNoEnrolment) {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          BadRequest(view(formWithErrors)),

        userHasVpdEnrolmentId =>
          if (userHasVpdEnrolmentId) {
            Redirect(config.eacdEnrolmentClaimRedirectUrl)
          }
          else {
            Redirect(
              controllers.enrolment.routes.UserDoesNotHaveApprovalIdController.onPageLoad().url
            )
          }
      )
  }
}
