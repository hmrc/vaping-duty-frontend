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

package navigation

import config.FrontendAppConfig

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import controllers.routes
import pages.*
import models.*
import uk.gov.hmrc.http.HttpVerbs.POST

@Singleton
class Navigator @Inject()(config: FrontendAppConfig) {

  private val normalRoutes: Page => UserAnswers => Call = {
    case HowToBeContactedPage   => ua => howToBeContactedRoute(ua)
    case EnterEmailPage         => _  => handoffToEmailVerification()
    case _                      => _  => routes.IndexController.onPageLoad()
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case _ => _ => routes.CheckYourAnswersController.onPageLoad()
  }

  def howToBeContactedRoute(ua: UserAnswers): Call = {
    ua.get(HowToBeContactedPage) match {
      case Some(HowToBeContacted.Email) => routes.EnterEmailController.onPageLoad(NormalMode)
      case Some(HowToBeContacted.Post)  => routes.ConfirmAddressController.onPageLoad()
      case _                            => routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def handoffToEmailVerification() = {
    // TODO add actual url
    Call(POST, config.loginUrl)
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }
}
