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

package controllers.auth

import config.FrontendAppConfig
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject


class AuthController @Inject()(
                                val controllerComponents: MessagesControllerComponents,
                                config: FrontendAppConfig
                              ) extends FrontendBaseController with I18nSupport {

  /**
   * When generated the below methods had the functionality to clear the user answers stored in this repository when a user signs out.
   * As we have moved our user answers to the backend some authentication was required to clear.  Our timeout lines up with the auth session
   * of 15 minutes which could cause the use of these methods when timed out to return as unauthorised, causing the wrong page to be displayed.
   * The TTL currently used will clear the answers at 15 minutes, so an explicit call to clear them is not needed.
   */

  def signOut(): Action[AnyContent] = Action {
    Redirect(config.signOutUrl, Map("continue" -> Seq(config.exitSurveyUrl)))
  }

  def signOutNoSurvey(): Action[AnyContent] = Action {
    Redirect(routes.SignedOutController.onPageLoad().url)
  }
}
