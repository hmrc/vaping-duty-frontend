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

import controllers.routes
import models.*
import models.contactPreference.PaperlessPreference.{Email, Post}
import models.contactPreference.{HowToBeContacted, PaperlessPreference}
import pages.*
import pages.contactPreference.{EnterEmailPage, HowToBeContactedPage}
import play.api.Logging
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() extends Logging {

  private val normalRoutes: Page => UserAnswers => Call = {
    case HowToBeContactedPage   => ua   => howToBeContactedRoute(ua)
    case EnterEmailPage         => _    => controllers.contactPreference.routes.SubmitPreviouslyVerifiedEmailController.onPageLoad()
    case _                      => _    => routes.IndexController.onPageLoad()
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case _ => _ => routes.CheckYourAnswersController.onPageLoad()
  }

  def howToBeContactedRoute(ua: UserAnswers): Call = {
    ua.get(HowToBeContactedPage) match {
      case Some(HowToBeContacted.Email) => controllers.contactPreference.routes.EnterEmailController.onPageLoad(NormalMode)
      case Some(HowToBeContacted.Post)  => postalRoute(ua)
      case _                            => routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def postalRoute(ua: UserAnswers): Call = {
    PaperlessPreference(ua.subscriptionSummary.paperlessPreference) match {
      case Email  => controllers.contactPreference.routes.ConfirmAddressController.onPageLoad()
      case Post   => controllers.contactPreference.routes.ContinuePostalPreferenceController.onPageLoad()
    }
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }
}
