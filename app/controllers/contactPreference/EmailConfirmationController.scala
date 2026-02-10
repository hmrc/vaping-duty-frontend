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

import config.FrontendAppConfig
import connectors.SubmitPreferencesConnector
import controllers.actions.*
import models.BtaLink
import models.emailverification.{PaperlessPreferenceSubmission, VerificationDetails}
import models.requests.DataRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.EmailVerificationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.contactPreference.EmailConfirmationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailConfirmationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: ApprovedVapingManufacturerAuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: EmailConfirmationView,
                                       config: FrontendAppConfig
                                     ) extends FrontendBaseController with I18nSupport with BtaLink {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val email = request.userAnswers.emailAddress.getOrElse("")

      Ok(view(email, btaLink))
  }

  override def btaLink: String = config.continueToBta
}
