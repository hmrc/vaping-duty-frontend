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
import pages.returns.*
import pages.returns.adjustments.*
import play.api.Logging
import play.api.http.HttpVerbs.GET
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class ReturnsNavigator @Inject()(
  config: FrontendAppConfig
) extends Logging {

  private def withPeriod(call: Call, periodKey: String): Call =
    Call(call.method, s"${call.url}?period=$periodKey")

  private def normalRoutes(periodKey: String, adjustmentReasonMandatory: Boolean): Page => ReturnsUserAnswers => Call = {
    case DeclareDutyPage                => ua  => declareDutyPageRoutes(ua, periodKey)
    case EnterDutyAmountPage            => _   => withPeriod(controllers.returns.submit.routes.DeclareDutyCheckAnswersController.onPageLoad(), periodKey)
    case DeclareSpoiltProductsPage      => ua  => declareSpoiltProductsPageRoutes(ua, periodKey)
    case SpoiltCheckYourAnswersPage     => ua  => addSpoiltAdjustmentPageRoutes(ua, periodKey)
    case SpoiltVolumeByPeriodPage       => _   => withPeriod(controllers.returns.submit.spoilt.routes.SpoiltCheckYourAnswersController.onPageLoad(), periodKey)
    case DeclareAdjustmentPage          => ua  => declareAdjustmentQuestionPageRoutes(ua, periodKey)
    case AdjustmentListPage             => ua  => adjustmentListPageRoutes(ua, periodKey)
    case AddAnotherAdjustmentPage       => ua  => addAnotherAdjustmentPageRoutes(ua, periodKey, adjustmentReasonMandatory, NormalMode)
    case AdjustmentReasonPage           => _   => withPeriod(controllers.returns.submit.routes.TaskListController.onPageLoad(), periodKey)
    case DeclareDutySuspensePage        => ua  => declareDutySuspensePageRoutes(ua, periodKey)
    case EnterDutySuspensePage          => _   => withPeriod(controllers.returns.submit.routes.DutySuspenseCheckAnswersController.onPageLoad(), periodKey)
    case DeclarationPage                => _   => withPeriod(controllers.returns.submit.routes.ConfirmationController.onPageLoad(), periodKey)
    case _                              => _   => Call(GET, BtaLink(config))
  }

  private def checkRouteMap(periodKey: String, adjustmentReasonMandatory: Boolean = false): Page => ReturnsUserAnswers => Call = {
    case DeclareDutyPage            => ua => checkDeclareDutyPageRoutes(ua, periodKey)
    case EnterDutyAmountPage        => _  => withPeriod(controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad(), periodKey)
    case DeclareSpoiltProductsPage  => _  => controllers.returns.submit.routes.TaskListController.onPageLoad()
    case DeclareAdjustmentPage      => ua => checkDeclareAdjustmentPageRoutes(ua, periodKey)
    case AdjustmentListPage         => _  => withPeriod(controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad(CheckMode), periodKey)
    case AddAnotherAdjustmentPage   => ua => addAnotherAdjustmentPageRoutes(ua, periodKey, adjustmentReasonMandatory, CheckMode)
    case AdjustmentReasonPage       => _  => withPeriod(controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad(), periodKey)
    case DeclareDutySuspensePage    => ua => checkDeclareDutySuspensePageRoutes(ua, periodKey)
    case EnterDutySuspensePage      => _  => withPeriod(controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad(), periodKey)
    case _                          => _  => routes.JourneyRecoveryController.onPageLoad()
  }

  private def declareDutyPageRoutes(ua: ReturnsUserAnswers, periodKey: String) = {
    ua.get(DeclareDutyPage) match
      case Some(true)  => withPeriod(controllers.returns.submit.routes.EnterDutyAmountController.onPageLoad(NormalMode), periodKey)
      case Some(false) => withPeriod(controllers.returns.submit.routes.DeclareDutyCheckAnswersController.onPageLoad(), periodKey)
      case _           => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  private def checkDeclareDutyPageRoutes(ua: ReturnsUserAnswers, periodKey: String) = {
    ua.get(DeclareDutyPage) match
      case Some(true)   => withPeriod(controllers.returns.submit.routes.EnterDutyAmountController.onPageLoad(CheckMode), periodKey)
      case Some(false)  => withPeriod(controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad(), periodKey)
      case _            => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  private def declareDutySuspensePageRoutes(ua: ReturnsUserAnswers, periodKey: String) = {
    ua.get(DeclareDutySuspensePage) match
      case Some(true)  => withPeriod(controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(NormalMode), periodKey)
      case Some(false) => withPeriod(controllers.returns.submit.routes.DutySuspenseCheckAnswersController.onPageLoad(), periodKey)
      case _           => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  private def checkDeclareDutySuspensePageRoutes(ua: ReturnsUserAnswers, periodKey: String) = {
    ua.get(DeclareDutySuspensePage) match
      case Some(true)   => withPeriod(controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(CheckMode), periodKey)
      case Some(false)  => withPeriod(controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad(), periodKey)
      case _            => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  private def declareSpoiltProductsPageRoutes(ua: ReturnsUserAnswers, periodKey: String) = {
    ua.get(DeclareSpoiltProductsPage) match
      case Some(true)  => withPeriod(controllers.returns.submit.spoilt.routes.SelectSpoiltPeriodController.onPageLoad(None), periodKey)
      case Some(false) => withPeriod(controllers.returns.submit.spoilt.routes.SpoiltCheckYourAnswersController.onPageLoad(), periodKey)
      case _           => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  private def addSpoiltAdjustmentPageRoutes(ua: ReturnsUserAnswers, periodKey: String) = {
    ua.get(SpoiltCheckYourAnswersPage) match
      case Some(true)  => withPeriod(controllers.returns.submit.spoilt.routes.SelectSpoiltPeriodController.onPageLoad(None), periodKey)
      case Some(false) => withPeriod(controllers.returns.submit.routes.TaskListController.onPageLoad(), periodKey)
      case _           => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  private def declareAdjustmentQuestionPageRoutes(ua: ReturnsUserAnswers, periodKey: String) = {
    ua.get(DeclareAdjustmentPage) match
      case Some(true)  => withPeriod(controllers.returns.submit.adjustments.routes.SelectAdjustmentPeriodController.onPageLoad(NormalMode, None), periodKey)
      case Some(false) => withPeriod(controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad(NormalMode), periodKey)
      case _           => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  private def checkDeclareAdjustmentPageRoutes(ua: ReturnsUserAnswers, periodKey: String) = {
    ua.get(DeclareAdjustmentPage) match
      case Some(true)  => withPeriod(controllers.returns.submit.adjustments.routes.SelectAdjustmentPeriodController.onPageLoad(CheckMode, None), periodKey)
      case Some(false) => withPeriod(controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad(CheckMode), periodKey)
      case _           => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  private def adjustmentListPageRoutes(ua: ReturnsUserAnswers, periodKey: String) = {
    withPeriod(controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad(NormalMode), periodKey)
  }

  private def addAnotherAdjustmentPageRoutes(ua: ReturnsUserAnswers, periodKey: String, adjustmentReasonMandatory: Boolean, mode: Mode) = {
    ua.get(AddAnotherAdjustmentPage) match
      case Some(true) => withPeriod(controllers.returns.submit.adjustments.routes.SelectAdjustmentPeriodController.onPageLoad(mode, None), periodKey)
      case Some(false) =>
        mode match {
          case CheckMode =>
            val hasReason = ua.get(AdjustmentReasonPage).isDefined
            if (adjustmentReasonMandatory && !hasReason)
              withPeriod(controllers.returns.submit.routes.AdjustmentReasonController.onPageLoad(CheckMode), periodKey)
            else
              withPeriod(controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad(), periodKey)
          case NormalMode =>
            if (adjustmentReasonMandatory)
              withPeriod(controllers.returns.submit.routes.AdjustmentReasonController.onPageLoad(NormalMode), periodKey)
            else
              withPeriod(controllers.returns.submit.routes.TaskListController.onPageLoad(), periodKey)
        }
      case _ => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: ReturnsUserAnswers, adjustmentReasonMandatory: Boolean = false): Call = mode match {
    case NormalMode =>
      normalRoutes(userAnswers.periodKey, adjustmentReasonMandatory)(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(userAnswers.periodKey, adjustmentReasonMandatory)(page)(userAnswers)
  }
}