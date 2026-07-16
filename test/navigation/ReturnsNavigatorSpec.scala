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
import pages.returns.{SpoiltCheckYourAnswersPage, DeclareDutyPage, DeclareDutySuspensePage, DeclareSpoiltProductsPage, EnterDutyAmountPage, EnterDutySuspensePage, SpoiltVolumeByPeriodPage}
import pages.returns.adjustments.{AddAnotherAdjustmentPage, AdjustmentListPage, AdjustmentReasonPage, DeclareAdjustmentPage}
import play.api.libs.json.Json
import play.api.mvc.Call

import java.time.{Instant, Month}

class ReturnsNavigatorSpec extends SpecBase {

  val navigator = new ReturnsNavigator(mockAppConfig)

  "ReturnsNavigator" - {

    "in Normal mode" - {

      "must go from DeclareDuty to EnterAmount when there IS duty to declare" in {
        val ua = returnsUserAnswers.set(DeclareDutyPage, true).success.value
        navigator.nextPage(DeclareDutyPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.EnterDutyAmountController.onPageLoad(NormalMode).url}?period=$periodKey"
      }

      "must go from DeclareDuty to DeclareDutyCheckAnswers (mini CYA) when there IS NO duty to declare" in {
        val ua = returnsUserAnswers
          .set(EnterDutyAmountPage, BigDecimal(1)).success.value
          .set(DeclareDutyPage, false).success.value

        ua.get(EnterDutyAmountPage) mustBe None
        navigator.nextPage(DeclareDutyPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.DeclareDutyCheckAnswersController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareDuty to JourneyRecovery when there is no value present" in {
        val ua = ReturnsUserAnswers("id", periodKey.value, Some(Month.JUNE), Some("2027"), Json.obj(), Instant.now(), Instant.now())
        navigator.nextPage(DeclareDutyPage, NormalMode, ua) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from EnterDutyAmount to DeclareDutyCheckAnswers (mini CYA)" in {
        val ua = returnsUserAnswers.set(EnterDutyAmountPage, BigDecimal(1)).success.value

        navigator.nextPage(EnterDutyAmountPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.DeclareDutyCheckAnswersController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareDutySuspense to EnterDutySuspense when there IS suspended duty to declare" in {
        val ua = returnsUserAnswers.set(DeclareDutySuspensePage, true).success.value
        navigator.nextPage(DeclareDutySuspensePage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(NormalMode).url}?period=$periodKey"
      }

      "must go from DeclareDutySuspense to DutySuspenseCheckAnswers (mini CYA) when there IS NO suspended duty to declare" in {
        val ua = returnsUserAnswers
          .set(DeclareDutySuspensePage, false).success.value

        ua.get(EnterDutySuspensePage) mustBe None
        navigator.nextPage(DeclareDutySuspensePage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.DutySuspenseCheckAnswersController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareDutySuspense to JourneyRecovery when there is no value present" in {
        val ua = ReturnsUserAnswers("id", periodKey.value, Some(Month.JUNE), Some("2027"), Json.obj(), Instant.now(), Instant.now())
        navigator.nextPage(DeclareDutySuspensePage, NormalMode, ua) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from EnterDutySuspense to DutySuspenseCheckAnswers (mini CYA)" in {
        val ua = returnsUserAnswers.set(EnterDutySuspensePage, DutySuspenseVolumes(1, 1)).success.value

        navigator.nextPage(EnterDutySuspensePage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.DutySuspenseCheckAnswersController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareSpoiltProductsPage to SelectSpoiltPeriodPage when there are spoilt products to declare" in {
        val ua = returnsUserAnswers.set(DeclareSpoiltProductsPage, true).success.value

        navigator.nextPage(DeclareSpoiltProductsPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.spoilt.routes.SelectSpoiltPeriodController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareSpoiltProductsPage to SpoiltCheckYourAnswers when there are NO spoilt products to declare" in {
        val ua = returnsUserAnswers.set(DeclareSpoiltProductsPage, false).success.value

        navigator.nextPage(DeclareSpoiltProductsPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.spoilt.routes.SpoiltCheckYourAnswersController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareSpoiltProductsPage to JourneyRecovery when there is no value present" in {
        val ua = ReturnsUserAnswers("id", periodKey.value, Some(Month.JUNE), Some("2027"), Json.obj(), Instant.now(), Instant.now())
        navigator.nextPage(DeclareSpoiltProductsPage, NormalMode, ua) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from SpoiltVolumeByPeriodPage to SpoiltCheckYourAnswersPage when entering spoilt products" in {
        val ua = returnsUserAnswers.set(SpoiltVolumeByPeriodPage, List(SpoiltVolumeByPeriod(1, periodKey))).success.value

        navigator.nextPage(SpoiltVolumeByPeriodPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.spoilt.routes.SpoiltCheckYourAnswersController.onPageLoad().url}?period=$periodKey"
      }

      "must go from AddSpoiltAdjustmentPage to SelectSpoiltPeriod view when user has more spoilt adjustments to make" in {
        val ua = returnsUserAnswers.set(SpoiltCheckYourAnswersPage, true).success.value

        navigator.nextPage(SpoiltCheckYourAnswersPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.spoilt.routes.SelectSpoiltPeriodController.onPageLoad().url}?period=$periodKey"
      }

      "must go from AddSpoiltAdjustmentPage to TaskList view when no more spoilt adjustments to make" in {
        val ua = returnsUserAnswers.set(SpoiltCheckYourAnswersPage, false).success.value

        navigator.nextPage(SpoiltCheckYourAnswersPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=$periodKey"
      }

      "must go from AddSpoiltAdjustmentPage to JourneyRecovery when there is no value present" in {
        val ua = ReturnsUserAnswers("id", periodKey.value, Some(Month.JUNE), Some("2027"), Json.obj(), Instant.now(), Instant.now())
        navigator.nextPage(SpoiltCheckYourAnswersPage, NormalMode, ua) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from DeclareAdjustmentPage to SelectAdjustmentPeriod when there are adjustments to declare" in {
        val ua = returnsUserAnswers.set(DeclareAdjustmentPage, true).success.value

        navigator.nextPage(DeclareAdjustmentPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.adjustments.routes.SelectAdjustmentPeriodController.onPageLoad(None).url}?period=$periodKey"
      }

      "must go from DeclareAdjustmentPage to AdjustmentCheckYourAnswers when there are NO adjustments to declare" in {
        val ua = returnsUserAnswers.set(DeclareAdjustmentPage, false).success.value

        navigator.nextPage(DeclareAdjustmentPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad().url}?period=$periodKey"
      }

      "must go from DeclareAdjustmentPage to JourneyRecovery when there is no value present" in {
        val ua = ReturnsUserAnswers("id", periodKey.value, Some(Month.JUNE), Some("2027"), Json.obj(), Instant.now(), Instant.now())
        navigator.nextPage(DeclareAdjustmentPage, NormalMode, ua) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from AdjustmentListPage to AdjustmentCheckYourAnswers (mini CYA)" in {
        val ua = returnsUserAnswers.set(AdjustmentListPage, adjustmentList).success.value

        navigator.nextPage(AdjustmentListPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad().url}?period=$periodKey"
      }

      "must go from AddAnotherAdjustmentPage to SelectAdjustmentPeriod when user has more adjustments to make" in {
        val ua = returnsUserAnswers.set(AddAnotherAdjustmentPage, true).success.value

        navigator.nextPage(AddAnotherAdjustmentPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.adjustments.routes.SelectAdjustmentPeriodController.onPageLoad(None).url}?period=$periodKey"
      }

      "must go from AddAnotherAdjustmentPage to TaskList when no more adjustments to make" in {
        val ua = returnsUserAnswers.set(AddAnotherAdjustmentPage, false).success.value

        navigator.nextPage(AddAnotherAdjustmentPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=$periodKey"
      }

      "must go from AddAnotherAdjustmentPage to AdjustmentReason when adjustmentReasonMandatory is true" in {
        val ua = returnsUserAnswers.set(AddAnotherAdjustmentPage, false).success.value

        navigator.nextPage(AddAnotherAdjustmentPage, NormalMode, ua, adjustmentReasonMandatory = true)
          .url mustBe s"${controllers.returns.submit.routes.AdjustmentReasonController.onPageLoad(NormalMode).url}?period=$periodKey"
      }

      "must go from AddAnotherAdjustmentPage to TaskList when adjustmentReasonMandatory is false" in {
        val ua = returnsUserAnswers.set(AddAnotherAdjustmentPage, false).success.value

        navigator.nextPage(AddAnotherAdjustmentPage, NormalMode, ua, adjustmentReasonMandatory = false)
          .url mustBe s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=$periodKey"
      }

      "must go from AdjustmentReasonPage to TaskList" in {
        val ua = returnsUserAnswers.set(AdjustmentReasonPage, "a reason").success.value

        navigator.nextPage(AdjustmentReasonPage, NormalMode, ua).url mustBe s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=$periodKey"
      }

      "must go from AddAnotherAdjustmentPage to JourneyRecovery when there is no value present" in {
        val ua = ReturnsUserAnswers("id", periodKey.value, Some(Month.JUNE), Some("2027"), Json.obj(), Instant.now(), Instant.now())
        navigator.nextPage(AddAnotherAdjustmentPage, NormalMode, ua) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
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

      "must go from DeclareDutyPage to JourneyRecovery when there is no value present" in {
        val ua = ReturnsUserAnswers("id", periodKey.value, Some(Month.JUNE), Some("2027"), Json.obj(), Instant.now(), Instant.now())
        navigator.nextPage(DeclareDutyPage, CheckMode, ua) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
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

      "must go from DeclareDutySuspensePage to JourneyRecovery when there is no value present" in {
        val ua = ReturnsUserAnswers("id", periodKey.value, Some(Month.JUNE), Some("2027"), Json.obj(), Instant.now(), Instant.now())
        navigator.nextPage(DeclareDutySuspensePage, CheckMode, ua) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from DeclareSpoiltProductsPage to TaskList" in {
        navigator.nextPage(DeclareSpoiltProductsPage, CheckMode, returnsUserAnswers) mustBe controllers.returns.submit.routes.TaskListController.onPageLoad()
      }

      "must go from DeclareAdjustmentPage to AdjustmentCheckYourAnswers (mini CYA)" in {
        navigator.nextPage(DeclareAdjustmentPage, CheckMode, returnsUserAnswers).url mustBe s"${controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad().url}?period=$periodKey"
      }

      "must go from AdjustmentReasonPage to CheckYourAnswers" in {
        val ua = returnsUserAnswers.set(AdjustmentReasonPage, "test reason").success.value
        navigator.nextPage(AdjustmentReasonPage, CheckMode, ua).url mustBe s"${controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url}?period=$periodKey"
      }
    }
  }
}
