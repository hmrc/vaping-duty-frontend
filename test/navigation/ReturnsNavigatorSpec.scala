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
import models.returns.ReturnsUserAnswers
import pages.*
import pages.returns.{DeclareDutyPage, EnterDutyAmountPage}
import play.api.libs.json.Json
import play.api.mvc.Call

import java.time.Instant

class ReturnsNavigatorSpec extends SpecBase {

  val navigator = new ReturnsNavigator(mockAppConfig)

  "ReturnsNavigator" - {

    "in Normal mode" - {

      "must go from DeclareDuty to EnterAmount when there IS duty to declare" in {
        val ua = returnsUserAnswers.set(DeclareDutyPage, true).success.value
        navigator.nextPage(DeclareDutyPage, NormalMode, ua) mustBe controllers.returns.routes.EnterDutyAmountController.onPageLoad(NormalMode)
      }

      "must go from DeclareDuty to TaskList when there IS NO duty to declare" in {
        val ua = returnsUserAnswers
          .set(EnterDutyAmountPage, 1).success.value
          .set(DeclareDutyPage, false).success.value

        ua.get(EnterDutyAmountPage) mustBe None
        navigator.nextPage(DeclareDutyPage, NormalMode, ua) mustBe controllers.returns.routes.TaskListController.onPageLoad()
      }

      "must go from DeclareDuty to JourneyRecovery when there is no value present" in {
        val ua = ReturnsUserAnswers("id", Json.obj(), Instant.now(), Instant.now())
        navigator.nextPage(DeclareDutyPage, NormalMode, ua) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from EnterAmount to TaskList " in {
        val ua = returnsUserAnswers.set(EnterDutyAmountPage, 1).success.value

        navigator.nextPage(EnterDutyAmountPage, NormalMode, ua) mustBe controllers.returns.routes.TaskListController.onPageLoad()
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

        navigator.nextPage(EnterDutyAmountPage, CheckMode, returnsUserAnswers) mustBe controllers.returns.routes.CheckYourAnswersController.onPageLoad()
      }
    }
  }
}
