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

import models.ContactPreferenceUserAnswers
import models.contactPreference.PaperlessPreference.{Email, Post}
import models.contactPreference.{HowToBeContacted, PaperlessPreference}
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

case class HowToBeContactedViewModel(content: Html, radioItems: Seq[RadioItem])

object HowToBeContactedViewModel {
  def apply(ua: ContactPreferenceUserAnswers)(implicit messages: Messages): HowToBeContactedViewModel = {
    howToBeContactedViewModel(ua)
  }

  private def howToBeContactedViewModel(ua: ContactPreferenceUserAnswers)(implicit messages: Messages) = {

    val emailPreference: String     = ua.subscriptionSummary.emailAddress.getOrElse("")
    val emailSuffix: String         = messages("contactPreference.howToBeContacted.email")
    val emailPreferenceKey: String  = "setToEmail"
    val postalPreference: String    = ua.subscriptionSummary.correspondenceAddress.replaceAll("\n", ", ")
    val postalSuffix: String        = messages("contactPreference.howToBeContacted.post")
    val postalPreferenceKey: String = "setToPost"

    PaperlessPreference(ua.subscriptionSummary.paperlessPreference) match {
      case Email =>
        buildViewModel(
          content     = makeContentString(suffix = emailSuffix, currentPreference = emailPreference),
          radioItems  = HowToBeContacted.options(emailPreferenceKey)
        )
      case Post =>
        buildViewModel(
          content     = makeContentString(suffix = postalSuffix, currentPreference = postalPreference),
          radioItems  = HowToBeContacted.options(postalPreferenceKey)
        )
    }
  }

  private def buildViewModel(content: String, radioItems: Seq[RadioItem]) = {
    HowToBeContactedViewModel(content = Html(content), radioItems = radioItems)
  }

  private def makeContentString(suffix: String, currentPreference: String)
                               (implicit messages: Messages): String = {
    val currentMessage: String = messages("contactPreference.howToBeContacted.currently")

    s"$currentMessage $suffix<br/><strong>$currentPreference</strong>"
  }
}
