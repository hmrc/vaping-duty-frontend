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

import javax.inject.Inject
import scala.concurrent.Future

import config.FrontendAppConfig
import controllers.actions._
import models.BtaLink
import models.contactPreference.HowToBeContacted
import models.requests.DataRequest
import pages.contactPreference.HowToBeContactedPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.UserAnswersService
import views.html.contactPreference.{EmailConfirmationView, PostalConfirmationView}

import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

class ConfirmationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: ApprovedVapingManufacturerAuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       postalConfirmationView: PostalConfirmationView,
                                       emailConfirmationView: EmailConfirmationView,
                                       userAnswersService: UserAnswersService,
                                       config: FrontendAppConfig
                                     ) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val btaUrl = BtaLink(config)
      val email = request.userAnswers.emailAddress.getOrElse("")
      
      request.userAnswers.get(HowToBeContactedPage) match {
        case Some(value) => value match {
          case HowToBeContacted.Email =>
            clearUserSession(userAnswersService)
            Ok(emailConfirmationView(email, btaUrl))
          case HowToBeContacted.Post  =>
            clearUserSession(userAnswersService)
            Ok(postalConfirmationView(btaUrl))
        }
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  private def clearUserSession(userAnswersService: UserAnswersService)
                              (implicit hc: HeaderCarrier, request: DataRequest[?]): Future[Either[UpstreamErrorResponse, Unit]] = {
    userAnswersService.clear(request.userId)
  }
}
