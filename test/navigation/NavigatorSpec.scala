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
import config.FrontendAppConfig
import controllers.routes
import pages.*
import models.*
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.contactPreference.{EnterEmailPage, HowToBeContactedPage}
import play.api.mvc.Call
import uk.gov.hmrc.http.HttpVerbs.POST

class NavigatorSpec extends SpecBase {

  override val mockAppConfig = mock[FrontendAppConfig]
  val navigator = new Navigator(mockAppConfig)

  "Navigator" - {

    "in Normal mode" - {

      "must go from a page that doesn't exist in the route map to Index" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id")) mustBe routes.IndexController.onPageLoad()
      }

      "must go from HowToBeContacted page to EnterEmail page" in {

        val ua = UserAnswers("id").set(HowToBeContactedPage, HowToBeContacted.Email).success.value
        navigator.nextPage(HowToBeContactedPage, NormalMode, ua) mustBe controllers.contactPreference.routes.EnterEmailController.onPageLoad(NormalMode)
      }

      "must go from HowToBeContacted page to Post page" in {

        val ua = UserAnswers("id").set(HowToBeContactedPage, HowToBeContacted.Post).success.value
        navigator.nextPage(HowToBeContactedPage, NormalMode, ua) mustBe controllers.contactPreference.routes.ConfirmAddressController.onPageLoad()
      }

      "must go from EnterEmail page to Email Verification Service when address is unverified" in {
        //TODO Must expand UA to include verified email list
        val ua = UserAnswers("id").set(HowToBeContactedPage, HowToBeContacted.Email).success.value
        navigator.nextPage(EnterEmailPage, NormalMode, ua) mustBe Call(POST, mockAppConfig.loginUrl)
      }
    }

    "in Check mode" - {

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        navigator.nextPage(UnknownPage, CheckMode, UserAnswers("id")) mustBe routes.CheckYourAnswersController.onPageLoad()
      }
    }
  }
}
