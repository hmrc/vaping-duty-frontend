/*
 * Copyright 2026 HM Revenue & Customs
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

package viewmodels.returns.submit

import base.SpecBase
import models.NormalMode
import models.returns.DutySuspenseVolumes
import pages.returns.{DeclareDutySuspensePage, EnterDutySuspensePage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Actions

class DutySuspenseCheckAnswersViewModelSpec extends SpecBase {

  private val volumeReceived = 1000
  private val volumeMoved = 500
  private val dutySuspenseVolumes = DutySuspenseVolumes(volumeReceived, volumeMoved)
  implicit val messages: Messages = messages(applicationBuilder(None).build())

  "DutySuspenseCheckAnswersViewModel" - {

    "when user answers YES and has entered volumes" - {

      "must return Some(viewModel)" in {
        val ua = returnsUserAnswers
          .set(DeclareDutySuspensePage, true).success.value
          .set(EnterDutySuspensePage, dutySuspenseVolumes).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        vm mustBe defined
      }

      "must have correct heading" in {
        val ua = returnsUserAnswers
          .set(DeclareDutySuspensePage, true).success.value
          .set(EnterDutySuspensePage, dutySuspenseVolumes).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        vm.get.heading mustBe messages("returns.dutySuspenseCheckAnswers.heading")
      }

      "must have card actions with change link" in {
        val ua = returnsUserAnswers
          .set(DeclareDutySuspensePage, true).success.value
          .set(EnterDutySuspensePage, dutySuspenseVolumes).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        vm.get.cardActions mustBe defined
        vm.get.cardActions.get.size mustBe 1
        
        val changeAction = vm.get.cardActions.get.head
        changeAction.href must include(controllers.returns.submit.routes.EnterDutySuspenseController.onPageLoad(NormalMode).url)
        changeAction.href must include(s"period=${periodKey.value}")
      }

      "must create summary list with three rows" in {
        val ua = returnsUserAnswers
          .set(DeclareDutySuspensePage, true).success.value
          .set(EnterDutySuspensePage, dutySuspenseVolumes).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        vm.get.summaryList.rows.size mustBe 3
      }

      "must have declare duty suspense row showing Yes with change link" in {
        val ua = returnsUserAnswers
          .set(DeclareDutySuspensePage, true).success.value
          .set(EnterDutySuspensePage, dutySuspenseVolumes).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        val declareRow = vm.get.summaryList.rows.head
        declareRow.key.content.asHtml.toString must include(messages("returns.dutySuspenseCheckAnswers.declareDutySuspense"))
        declareRow.value.content.asHtml.toString must include(messages("site.yes"))
        declareRow.actions.value.items.size mustBe 1
        
        val changeLink = declareRow.actions.value.items.head
        changeLink.href must include(controllers.returns.submit.routes.DeclareDutySuspenseController.onPageLoad(NormalMode).url)
        changeLink.href must include(s"period=${periodKey.value}")
      }

      "must have product received row with formatted volume and no change link" in {
        val ua = returnsUserAnswers
          .set(DeclareDutySuspensePage, true).success.value
          .set(EnterDutySuspensePage, dutySuspenseVolumes).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        val receivedRow = vm.get.summaryList.rows(1)
        receivedRow.key.content.asHtml.toString must include(messages("returns.dutySuspenseCheckAnswers.productReceived"))
        receivedRow.value.content.asHtml.toString must include("1000 ml")
        receivedRow.actions mustBe Some(Actions("", List()))
      }

      "must have product moved row with formatted volume and no change link" in {
        val ua = returnsUserAnswers
          .set(DeclareDutySuspensePage, true).success.value
          .set(EnterDutySuspensePage, dutySuspenseVolumes).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        val movedRow = vm.get.summaryList.rows(2)
        movedRow.key.content.asHtml.toString must include(messages("returns.dutySuspenseCheckAnswers.productMoved"))
        movedRow.value.content.asHtml.toString must include("500 ml")
        movedRow.actions mustBe Some(Actions("", List()))
      }

      "must format volumes correctly with ml suffix" in {
        val ua = returnsUserAnswers
          .set(DeclareDutySuspensePage, true).success.value
          .set(EnterDutySuspensePage, DutySuspenseVolumes(0, 12345)).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        val receivedRow = vm.get.summaryList.rows(1)
        val movedRow = vm.get.summaryList.rows(2)
        
        receivedRow.value.content.asHtml.toString must include("0 ml")
        movedRow.value.content.asHtml.toString must include("12345 ml")
      }
    }

    "when user answers NO" - {

      "must return Some(viewModel)" in {
        val ua = returnsUserAnswers.set(DeclareDutySuspensePage, false).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        vm mustBe defined
      }

      "must have correct heading for nil return" in {
        val ua = returnsUserAnswers.set(DeclareDutySuspensePage, false).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        vm.get.heading mustBe messages("returns.dutySuspenseCheckAnswers.noDutyHeading")
      }

      "must have no card actions" in {
        val ua = returnsUserAnswers.set(DeclareDutySuspensePage, false).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        vm.get.cardActions mustBe None
      }

      "must create summary list with one row" in {
        val ua = returnsUserAnswers.set(DeclareDutySuspensePage, false).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        vm.get.summaryList.rows.size mustBe 1
      }

      "must have declare duty suspense row showing No with change link" in {
        val ua = returnsUserAnswers.set(DeclareDutySuspensePage, false).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        val declareRow = vm.get.summaryList.rows.head
        declareRow.key.content.asHtml.toString must include(messages("returns.dutySuspenseCheckAnswers.declareDutySuspense"))
        declareRow.value.content.asHtml.toString must include(messages("site.no"))
        declareRow.actions.value.items.size mustBe 1
        
        val changeLink = declareRow.actions.value.items.head
        changeLink.href must include(controllers.returns.submit.routes.DeclareDutySuspenseController.onPageLoad(NormalMode).url)
        changeLink.href must include(s"period=${periodKey.value}")
      }
    }

    "when user answers YES but has not entered volumes" - {

      "must return None to trigger journey recovery" in {
        val ua = returnsUserAnswers.set(DeclareDutySuspensePage, true).success.value
        
        val vm = DutySuspenseCheckAnswersViewModel(ua, periodKey)

        vm mustBe None
      }
    }

    "when user has not answered the declare question" - {

      "must return None to trigger journey recovery" in {
        val vm = DutySuspenseCheckAnswersViewModel(returnsUserAnswers, periodKey)

        vm mustBe None
      }
    }
  }
}