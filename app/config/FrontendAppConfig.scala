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

package config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FrontendAppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  private val vdHost: String = servicesConfig.baseUrl("vaping-duty")

  private val contactHost = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "vaping-duty-frontend"
  private lazy val contactPreferencesHost: String = servicesConfig.baseUrl("vaping-duty-account")

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${host + request.uri}"

  val enrolmentServiceName = configuration.get[String]("enrolment.serviceName")
  val enrolmentIdentifierKey = configuration.get[String]("enrolment.identifierKey")

  val loginUrl: String                       = configuration.get[String]("urls.login")
  val loginContinueUrl: String               = configuration.get[String]("urls.loginContinue")
  val signOutUrl: String                     = configuration.get[String]("urls.signOut")

  val organisationAcctGuidanceUrl: String    = configuration.get[String]("urls.organisationAcctGuidance")
  val applyForVpdIdGuidanceUrl: String       = configuration.get[String]("urls.applyForVpdIdGuidanceUrl")
  val continueToBta: String                  = configuration.get[String]("urls.businessTaxAccount")
  val accessibilityStatementUrl: String = configuration.get[String]("accessibility-statement.host") ++
    configuration.get[String]("accessibility-statement.url")

  private val exitSurveyBaseUrl: String      = configuration.get[String]("urls.feedback-frontend-base-url")
  val exitSurveyUrl: String                  = s"$exitSurveyBaseUrl/feedback/vaping-duty-frontend"

  val eacdEnrolmentClaimRedirectUrl: String  =
    configuration.get[String]("urls.enrolmentManagementFrontend") +
      s"/$enrolmentServiceName/request-access-tax-scheme?continue=$continueToBta"

  private val enrolmentContinuePath: String  = "/vaping-duty/enrolment/do-you-have-an-approval-id"

  val orgSignInUrl: String =
    s"$loginUrl?continue=${host + enrolmentContinuePath}&affinityGroup=${AffinityGroup.Organisation}"

  val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  def languageMap: Map[String, Lang] = Map(
    "en" -> Lang("en"),
    "cy" -> Lang("cy")
  )

  val timeout: Int   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val cacheTtl: Long = configuration.get[Int]("mongodb.timeToLiveInSeconds")

  val enrolmentServiceName = configuration.get[String]("enrolment.serviceName")
  val enrolmentIdentifierKey = configuration.get[String]("enrolment.identifierKey")

  def vdrPingUrl(): String = s"$vdHost/vaping-duty/ping"

  def ecpUserAnswersGetUrl(appaId: String): String =
    s"$contactPreferencesHost/vaping-duty-account/user-answers/$appaId"

  def ecpUserAnswersUrl(): String =
    s"$contactPreferencesHost/vaping-duty-account/user-answers"

  def ecpUserAnswersClearAllUrl(): String =
    s"$contactPreferencesHost/vaping-duty-account/test-only/user-answers/clear-all"

  def ecpGetEmailVerificationUrl(credId: String): String =
    s"$contactPreferencesHost/vaping-duty-account/get-email-verification/$credId"

  def ecpSubmitContactPreferencesUrl(appaId: String): String =
    s"$contactPreferencesHost/vaping-duty-account/submit-preferences/$appaId"

  private val startEmailVerificationContinueBaseUrl: String   =
      configuration.get[String]("microservice.services.contact-preferences-frontend.prefix")
  private val startEmailVerificationContinueUrlSuffix: String =
    configuration.get[String]("microservice.services.contact-preferences-frontend.url.checkYourAnswersPage")
  private val startEmailVerificationBackUrlSuffix: String     =
    configuration.get[String]("microservice.services.contact-preferences-frontend.url.enterEmailPage")

  val startEmailVerificationContinueUrl: String =
    s"$startEmailVerificationContinueBaseUrl$startEmailVerificationContinueUrlSuffix"
  val startEmailVerificationBackUrl: String     =
    s"$startEmailVerificationContinueBaseUrl$startEmailVerificationBackUrlSuffix"

  private val startEmailVerificationJourneyBaseUrl: String   = servicesConfig.baseUrl("email-verification")
  private val startEmailVerificationJourneyUrlSuffix: String =
    configuration.get[String]("microservice.services.email-verification.url.startEmailVerificationJourney")

  val startEmailVerificationJourneyUrl: String =
    s"$startEmailVerificationJourneyBaseUrl$startEmailVerificationJourneyUrlSuffix"

  val emailVerificationRedirectBaseUrl: String =
    configuration.get[String]("microservice.services.email-verification-frontend.prefix")

  val newServiceNavigationEnabled: Boolean = configuration.get[Boolean]("play-frontend-hmrc.forceServiceNavigation")
}
