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

import base.SpecBase
import controllers.routes
import models.*
import models.returns.{DutySuspenseVolumes, ReturnsUserAnswers, SpoiltVolumeByPeriod}
import pages.*
import pages.returns.{AddSpoiltAdjustmentPage, DeclareDutyPage, DeclareDutySuspensePage, DeclareSpoiltProductsPage, EnterDutyAmountPage, EnterDutySuspensePage, SpoiltVolumeByPeriodPage}
import play.api.libs.json.Json
import play.api.mvc.Call

import java.time.Instant

class ReturnsNavigatorSpec extends SpecBase {

  val navigator = new ReturnsNavigator(mockAppConfig)

  "ReturnsNavigator" - {

    "in Normal mode" - {

      "must go from DeclareDuty to EnterAmount when there IS duty to declare" in {
        val ua = returnsUserAnswers.set(DeclareDutyPage, true).success.value
        navigator.nextPage(DeclareDutyPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.EnterDutyAmountController.onPageLoad(NormalMode).url}?period=$periodKey"
      }

      "must go from DeclareDuty to TaskList when there IS NO duty to declare" in {
        val ua = returnsUserAnswers
          .set(EnterDutyAmountPage, 1).success.value
          .set(DeclareDutyPage, false).success.value

        ua.get(EnterDutyAmountPage) mustBe None
        navigator.nextPage(DeclareDutyPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareDuty to JourneyRecovery when there is no value present" in {
        val ua = ReturnsUserAnswers("id", periodKey.value, Json.obj(), Instant.now(), Instant.now())
        navigator.nextPage(DeclareDutyPage, NormalMode, ua) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from EnterDutyAmount to TaskList " in {
        val ua = returnsUserAnswers.set(EnterDutyAmountPage, 1).success.value

        navigator.nextPage(EnterDutyAmountPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareDutySuspense to EnterDutySuspense when there IS suspended duty to declare" in {
        val ua = returnsUserAnswers.set(DeclareDutySuspensePage, true).success.value
        navigator.nextPage(DeclareDutySuspensePage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(NormalMode).url}?period=$periodKey"
      }

      "must go from DeclareDutySuspense to TaskList when there IS NO suspended duty to declare" in {
        val ua = returnsUserAnswers
          .set(DeclareDutySuspensePage, false).success.value

        ua.get(EnterDutySuspensePage) mustBe None
        navigator.nextPage(DeclareDutySuspensePage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareDutySuspense to JourneyRecovery when there is no value present" in {
        val ua = ReturnsUserAnswers("id", periodKey.value, Json.obj(), Instant.now(), Instant.now())
        navigator.nextPage(DeclareDutySuspensePage, NormalMode, ua) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from EnterDutySuspense to TaskList" in {
        val ua = returnsUserAnswers.set(EnterDutyAmountPage, 1).success.value

        navigator.nextPage(EnterDutySuspensePage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareSpoiltProductsPage to SelectSpoiltPeriodPage when there are spoilt products to declare" in {
        val ua = returnsUserAnswers.set(DeclareSpoiltProductsPage, true).success.value

        navigator.nextPage(DeclareSpoiltProductsPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.spoilt.routes.SelectSpoiltPeriodController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareSpoiltProductsPage to TaskList when there are NO spoilt products to declare" in {
        val ua = returnsUserAnswers.set(DeclareSpoiltProductsPage, false).success.value

        navigator.nextPage(DeclareSpoiltProductsPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=$periodKey"
      }

      "must go from SpoiltVolumeByPeriodPage to AddSpoiltAdjustmentPage when entering spoilt products" in {
        val ua = returnsUserAnswers.set(SpoiltVolumeByPeriodPage, List(SpoiltVolumeByPeriod(1, periodKey))).success.value

        navigator.nextPage(SpoiltVolumeByPeriodPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.spoilt.routes.AddSpoiltAdjustmentController.onPageLoad(NormalMode).url}?period=$periodKey"
      }

      "must go from AddSpoiltAdjustmentPage to SelectSpoiltPeriod view when user has more spoilt adjustments to make" in {
        val ua = returnsUserAnswers.set(AddSpoiltAdjustmentPage, true).success.value

        navigator.nextPage(AddSpoiltAdjustmentPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.spoilt.routes.SelectSpoiltPeriodController.onPageLoad().url}?period=$periodKey"
      }

      "must go from AddSpoiltAdjustmentPage to TaskList view when no more spoilt adjustments to make" in {
        val ua = returnsUserAnswers.set(AddSpoiltAdjustmentPage, false).success.value

        navigator.nextPage(AddSpoiltAdjustmentPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=$periodKey"
      }


      "must go from a page that doesn't exist in the route map to Index" in {
        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, returnsUserAnswers).url mustBe BtaLink(mockAppConfig)
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, returnsUserAnswers) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from EnterDutyAmountPage to CheckYourAnswers" in {

        navigator.nextPage(EnterDutyAmountPage, CheckMode, returnsUserAnswers).url mustBe s"${controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareDutyPage to CheckYourAnswers when selecting 'No'" in {
        val ua = returnsUserAnswers.set(DeclareDutyPage, false).success.value
        navigator.nextPage(DeclareDutyPage, CheckMode, ua).url mustBe s"${controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareDutyPage to EnterDutyAmount when selecting 'Yes'" in {
        val ua = returnsUserAnswers.set(DeclareDutyPage, true).success.value
        navigator.nextPage(DeclareDutyPage, CheckMode, ua).url mustBe s"${controllers.returns.submit.routes.EnterDutyAmountController.onPageLoad(CheckMode).url}?period=$periodKey"
      }

      "must go from EnterDutySuspensePage to CheckYourAnswers" in {

        navigator.nextPage(EnterDutySuspensePage, CheckMode, returnsUserAnswers).url mustBe s"${controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareDutySuspensePage to EnterDutySuspensePage when selecting 'No" in {
        val ua = returnsUserAnswers.set(DeclareDutySuspensePage, false).success.value
        navigator.nextPage(DeclareDutySuspensePage, CheckMode, ua).url mustBe s"${controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareDutySuspensePage to EnterDutySuspensePage when selecting 'Yes" in {
        val ua = returnsUserAnswers.set(DeclareDutySuspensePage, true).success.value
        navigator.nextPage(DeclareDutySuspensePage, CheckMode, ua).url mustBe s"${controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(CheckMode).url}?period=$periodKey"
      }
    }
  }
}
