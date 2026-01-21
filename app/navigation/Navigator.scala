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
import pages.contactPreference.{EnterEmailPage, HowToBeContactedPage}
import uk.gov.hmrc.http.HttpVerbs.POST

import scala.util.Random

@Singleton
class Navigator @Inject()(config: FrontendAppConfig) {

  private val normalRoutes: Page => ContactPreferenceUserAnswers => Call = {
    case HowToBeContactedPage   => ua => howToBeContactedRoute(ua)
    case EnterEmailPage         => ua  => enterEmailPageRoute(ua)
    case _                      => _  => routes.IndexController.onPageLoad()
  }

  private val checkRouteMap: Page => ContactPreferenceUserAnswers => Call = {
    case _ => _ => routes.CheckYourAnswersController.onPageLoad()
  }

  def howToBeContactedRoute(ua: ContactPreferenceUserAnswers): Call = {
    ua.get(HowToBeContactedPage) match {
      case Some(HowToBeContacted.Email) => controllers.contactPreference.routes.EnterEmailController.onPageLoad(NormalMode)
      case Some(HowToBeContacted.Post)  => controllers.contactPreference.routes.ConfirmAddressController.onPageLoad()
      case _                            => routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def enterEmailPageRoute(ua: ContactPreferenceUserAnswers): Call = {
    // TODO Update with real check against verified emails list
    val check = ua.verifiedEmailAddresses.contains(ua.get(EnterEmailPage).getOrElse(""))
    if (check) {
      // Email entered is already verified
      controllers.contactPreference.routes.EmailConfirmationController.onPageLoad()
    } else {
      handoffToEmailVerification()
    }
  }

  private def handoffToEmailVerification() = {
    Call(POST, config.startEmailVerificationJourneyUrl)
  }

  def nextPage(page: Page, mode: Mode, userAnswers: ContactPreferenceUserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }
}
