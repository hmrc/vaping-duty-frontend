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

package viewmodels.contactPreference

import models.UserAnswers
import models.contactPreference.PaperlessPreference.{Email, Post}
import models.contactPreference.{HowToBeContacted, PaperlessPreference}
import play.api.i18n.Messages
import play.twirl.api.Html

import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

case class HowToBeContactedViewModel(content: Html, radioItems: Seq[RadioItem])

object HowToBeContactedViewModel {
  def apply(ua: UserAnswers)(implicit messages: Messages): HowToBeContactedViewModel = {
    howToBeContactedViewModel(ua)
  }

  private def howToBeContactedViewModel(ua: UserAnswers)(implicit messages: Messages) = {

    PaperlessPreference(ua.subscriptionSummary.paperlessPreference) match {
      case Email =>

        val emailPreference: String     = ua.subscriptionSummary.emailAddress.getOrElse("")
        val emailSuffix: String         = messages("contactPreference.howToBeContacted.email")
        val emailPreferenceKey: String  = "setToEmail"
        val currentlyEmail: String      = messages("contactPreference.howToBeContacted.currently.email")

        buildViewModel(
          content     = makeContentString(message              = currentlyEmail,
                                          emailOrPost          = emailSuffix,
                                          emailOrPostalAddress = emailPreference
                                          ),
          radioItems  = HowToBeContacted.options(emailPreferenceKey)
        )
      case Post =>

        val postalPreference: String    = ua.subscriptionSummary.correspondenceAddress.replaceAll("\n", ", ")
        val postalSuffix: String        = messages("contactPreference.howToBeContacted.post")
        val postalPreferenceKey: String = "setToPost"
        val currentlyPost: String       = messages("contactPreference.howToBeContacted.currently.post")

        buildViewModel(
          content     = makeContentString(message              = currentlyPost,
                                          emailOrPost          = postalSuffix,
                                          emailOrPostalAddress = postalPreference
                                          ),
          radioItems  = HowToBeContacted.options(postalPreferenceKey)
        )
    }
  }

  private def buildViewModel(content: String, radioItems: Seq[RadioItem]) = {
    HowToBeContactedViewModel(content = Html(content), radioItems = radioItems)
  }

  private def makeContentString(message: String, emailOrPost: String, emailOrPostalAddress: String): String = {
    s"$message $emailOrPost<br/><strong>$emailOrPostalAddress</strong>"
  }
}
