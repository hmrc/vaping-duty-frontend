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

package data

import models.{InternalId, SubscriptionSummary, UserAnswers, UserDetails, VpdId}
import models.emailverification.*
import play.api.libs.json.{JsObject, Json}

import java.time.{Clock, Instant, ZoneId}

trait TestData {
  val vpdId = VpdId(id = "VPPAID01")
  val groupId: String = "groupid"
  val ukTimeZoneStringId = "Europe/London"
  val epochTime = 1718118467838L
  val clock: Clock = Clock.fixed(Instant.ofEpochMilli(epochTime), ZoneId.of(ukTimeZoneStringId))

  val userId: InternalId = InternalId(id = "user-id")
  val credId: String = "cred-id"

  val userDetails: UserDetails = UserDetails(vpdId.toString, userId.toString)

  val emailAddress  = "john.doe@example.com"
  val emailAddress2 = "jonjones@example.com"
  val emailAddress3 = "robsmith@example.com"
  val emailAddress4 = "timmytimmy@example.com"

  val verifiedEmailAddresses: Set[String] = Set(emailAddress2, emailAddress3)

  val correspondenceAddress = "Flat 123\n1 Example Road\nLondon\nAB1 2CD"
  val countryCode = "GB"

  val subscriptionSummaryEmail: SubscriptionSummary = SubscriptionSummary(
    paperlessPreference = true,
    emailAddress = Some(emailAddress),
    emailVerification = Some(true),
    bouncedEmail = Some(false),
    correspondenceAddress = correspondenceAddress,
    countryCode = Some(countryCode)
  )

  val subscriptionSummaryPostWithEmail: SubscriptionSummary = subscriptionSummaryEmail.copy(paperlessPreference = false)

  val subscriptionSummaryPostNoEmail: SubscriptionSummary = SubscriptionSummary(
    paperlessPreference = false,
    emailAddress = None,
    emailVerification = None,
    bouncedEmail = None,
    correspondenceAddress = correspondenceAddress,
    countryCode = Some(countryCode)
  )

  val userAnswers: UserAnswers = UserAnswers(
    vpdId = vpdId.toString,
    userId = userId.toString,
    subscriptionSummary = subscriptionSummaryEmail,
    emailAddress = None,
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(false))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val userAnswersEmailUpdate: UserAnswers = UserAnswers(
    vpdId = vpdId.toString,
    userId = userId.toString,
    subscriptionSummary = subscriptionSummaryEmail,
    emailAddress = None,
    data = JsObject(Seq()),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val userAnswersPostWithEmail: UserAnswers = UserAnswers(
    vpdId = vpdId.toString,
    userId = userId.toString,
    subscriptionSummary = subscriptionSummaryPostWithEmail,
    emailAddress = Some(emailAddress),
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(true))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val userAnswersPostWithUnverifiedEmail: UserAnswers = userAnswersPostWithEmail.copy(
    subscriptionSummary = subscriptionSummaryPostWithEmail.copy(emailVerification = Some(false))
  )

  val userAnswersPostWithBouncedEmail: UserAnswers = userAnswersPostWithEmail.copy(
    subscriptionSummary = subscriptionSummaryPostWithEmail.copy(bouncedEmail = Some(true))
  )

  val userAnswersPostWithBouncedEmailUpdate: UserAnswers = userAnswersPostWithEmail.copy(
    subscriptionSummary = subscriptionSummaryPostWithEmail.copy(bouncedEmail = Some(true))
  )

  val userAnswersPostNoEmail: UserAnswers = UserAnswers(
    vpdId = vpdId.toString,
    userId = userId.toString,
    subscriptionSummary = subscriptionSummaryPostNoEmail,
    emailAddress = Some(emailAddress),
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(true))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val emptyUserAnswers: UserAnswers = UserAnswers(
    vpdId = vpdId.toString,
    userId = userId.toString,
    subscriptionSummary = subscriptionSummaryEmail,
    emailAddress = None,
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val testVerificationDetails1: GetVerificationStatusResponseEmailAddressDetails =
    GetVerificationStatusResponseEmailAddressDetails(emailAddress = emailAddress, verified = false, locked = false)
  val testVerificationDetails2: GetVerificationStatusResponseEmailAddressDetails =
    GetVerificationStatusResponseEmailAddressDetails(emailAddress = emailAddress2, verified = true, locked = true)
  val testVerificationDetails3: GetVerificationStatusResponseEmailAddressDetails =
    GetVerificationStatusResponseEmailAddressDetails(emailAddress = emailAddress4, verified = true, locked = false)

  val testGetVerificationStatusResponse: GetVerificationStatusResponse = GetVerificationStatusResponse(
    List(testVerificationDetails1, testVerificationDetails2, testVerificationDetails3)
  )

  val testVerificationDetails: VerificationDetails = VerificationDetails(credId)

  val testEmailVerificationRequest: EmailVerificationRequest = EmailVerificationRequest(
    credId = credId,
    continueUrl = "/test-continue-url",
    origin = "testOrigin",
    deskproServiceName = "test-deskpro-name",
    accessibilityStatementUrl = "/test-accessibility-url",
    backUrl = "/test-back-url",
    email = EmailModel(address = emailAddress, enterUrl = "/test-enter-url"),
    labels = Labels(LanguageInfo("testTitle", "testServiceName"), LanguageInfo("testTitle2", "testServiceName2")),
    lang = "en",
    useNewGovUkServiceNavigation = true
  )

  val testJsonRedirectUriString: String =
    """
          {"redirectUri": "/test-uri"}
        """

  val testRedirectUri: RedirectUri = RedirectUri("/test-uri")

  val contactPreferenceSubmissionPost = PaperlessPreferenceSubmission(
    paperlessPreference = false,
    emailAddress = None,
    emailVerification = None,
    bouncedEmail = None
  )

  val contactPreferenceSubmissionEmail = PaperlessPreferenceSubmission(
    paperlessPreference = true,
    emailAddress = Some(emailAddress),
    emailVerification = Some(true),
    bouncedEmail = Some(false)
  )

  val contactPreferenceSubmissionNewEmail = PaperlessPreferenceSubmission(
    paperlessPreference = true,
    emailAddress = Some(emailAddress2),
    emailVerification = Some(true),
    bouncedEmail = Some(false)
  )

  val testSubmissionResponse = PaperlessPreferenceSubmittedResponse(Instant.now(clock), "910000000000")
  }
