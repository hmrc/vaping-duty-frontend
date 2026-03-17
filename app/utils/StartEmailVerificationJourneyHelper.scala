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

package utils

import config.FrontendAppConfig
import models.emailverification.{EmailModel, EmailVerificationRequest, Labels, LanguageInfo}
import play.api.i18n.{Lang, Messages, MessagesApi}

import java.net.URLEncoder
import javax.inject.Inject

class StartEmailVerificationJourneyHelper @Inject()(config: FrontendAppConfig)
                                                   (implicit messagesApi: MessagesApi) {

  def createRequest(credId: String, enteredEmail: String)(implicit messages: Messages): EmailVerificationRequest = {
    
    val language: String                 = messages.lang.code
    val enterEmailAddressPageUrl: String = config.startEmailVerificationBackUrl

    val email: EmailModel = EmailModel(
      address = enteredEmail,
      enterUrl = enterEmailAddressPageUrl
    )

    val english: Lang = Lang("en")
    val welsh: Lang = Lang("cy")

    def languageInfo(language: Lang): LanguageInfo = {
      LanguageInfo(
        pageTitle = messagesApi.preferred(Seq(language))("service.name"),
        userFacingServiceName = messagesApi.preferred(Seq(language))("emailVerificationJourney.signature")
      )
    }

    val labels: Labels = Labels(
      cy = languageInfo(welsh),
      en = languageInfo(english)
    )

    EmailVerificationRequest(
      credId = credId,
      continueUrl = config.startEmailVerificationContinueUrl,
      origin = URLEncoder.encode(messages("emailVerificationJourney.signature"), "utf-8"),
      deskproServiceName = "vaping-duty-frontend",
      accessibilityStatementUrl = config.accessibilityStatementUrl,
      backUrl = enterEmailAddressPageUrl,
      email = email,
      labels = labels,
      lang = language,
      useNewGovUkServiceNavigation = config.newServiceNavigationEnabled
    )
  }

}
