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

package viewmodels.contactPreference

import base.{SpecBase, UnitSpec}
import data.TestData
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text


class HowToBeContactedViewModelSpec extends UnitSpec with SpecBase with TestData {
 implicit val messages: Messages = messages(applicationBuilder(None).build())
 
  "HowToBeContactedViewModel" - {

    "content field should return a string containing the users currently subscribed email" in {

      val vm = HowToBeContactedViewModel(userAnswers)

      val result = vm.content.toString

      result contains userAnswers.subscriptionSummary.emailAddress.get mustBe true
    }

    "content field should return a string containing the users currently subscribed postal address" in {

      val vm = HowToBeContactedViewModel(userAnswersPostNoEmail)

      val result = vm.content.toString

      result contains userAnswersPostNoEmail.subscriptionSummary.correspondenceAddress.replaceAll("\n", ", ") mustBe true
    }

    "email radio item should contain the correct message value when set to email" in {

      val vm = HowToBeContactedViewModel(userAnswers)

      val result = vm.radioItems

      result.head.content mustBe Text(messages("contactPreference.howToBeContacted.setToEmail.email"))

    }

    "post radio item should contain the correct message value when set to email" in {

      val vm = HowToBeContactedViewModel(userAnswers)

      val result = vm.radioItems

      result.tail.head.content mustBe Text(messages("contactPreference.howToBeContacted.setToEmail.post"))

    }

    "email radio item should contain the correct message value when set to post" in {

      val vm = HowToBeContactedViewModel(userAnswersPostNoEmail)

      val result = vm.radioItems

      result.head.content mustBe Text(messages("contactPreference.howToBeContacted.setToPost.email"))
    }

    "post radio item should contain the correct message value when set to post" in {

      val vm = HowToBeContactedViewModel(userAnswersPostNoEmail)

      val result = vm.radioItems

      result.tail.head.content mustBe Text(messages("contactPreference.howToBeContacted.setToPost.post"))
    }
  }
}
