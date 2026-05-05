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
import controllers.routes
import models.*
import models.returns.ReturnsUserAnswers
import pages.*
import pages.returns.{DeclareDutyPage, EnterDutyAmountPage}
import play.api.Logging
import play.api.http.HttpVerbs.GET
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class ReturnsNavigator @Inject()(
  config: FrontendAppConfig
) extends Logging {

  private val normalRoutes: Page => ReturnsUserAnswers => Call = {
    case DeclareDutyPage        => ua   => declareDutyPageRoutes(ua)
    case EnterDutyAmountPage    => _    => controllers.returns.submit.routes.TaskListController.onPageLoad()
    case _                      => _    => Call(GET, BtaLink(config))
  }

  private val checkRouteMap: Page => ReturnsUserAnswers => Call = {
    case EnterDutyAmountPage    => _    => controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad()
    case _ => _ => routes.JourneyRecoveryController.onPageLoad()
  }

  private def declareDutyPageRoutes(ua: ReturnsUserAnswers) = {
    ua.get(DeclareDutyPage) match
      case Some(true)   => controllers.returns.submit.routes.EnterDutyAmountController.onPageLoad(NormalMode)
      case Some(false)  => controllers.returns.submit.routes.TaskListController.onPageLoad()
      case _            => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: ReturnsUserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }
}
