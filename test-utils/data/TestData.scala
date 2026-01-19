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

import models.{ContactPreferenceUserAnswers, UserDetails, SubscriptionSummary}
import models.emailverification._
import play.api.libs.json.{JsObject, Json}

import java.time.{Clock, Instant, ZoneId}

trait TestData {
  val vppaId = "VPPAID01"
  val groupId: String = "groupid"
  val ukTimeZoneStringId = "Europe/London"
  val clock: Clock = Clock.fixed(Instant.ofEpochMilli(1718118467838L), ZoneId.of(ukTimeZoneStringId))

  val userId: String = "user-id"
  val credId: String = "cred-id"

  val userDetails: UserDetails = UserDetails(vppaId, userId)

  val emailAddress = "john.doe@example.com"
  val emailAddress2 = "jonjones@example.com"
  val emailAddress3 = "robsmith@example.com"
  val emailAddress4 = "timmytimmy@example.com"

  val verifiedEmailAddresses: Set[String] = Set(emailAddress2, emailAddress3)

  val correspondenceAddress = "Flat 123\n1 Example Road\nLondon\nAB1 2CD"
  val countryCode = "GB"

  val subscriptionSummaryEmail: SubscriptionSummary = SubscriptionSummary(
    paperlessReference = true,
    emailAddress = Some(emailAddress),
    emailVerification = Some(true),
    bouncedEmail = Some(false),
    correspondenceAddress = correspondenceAddress,
    countryCode = Some(countryCode)
  )

  val subscriptionSummaryPostWithEmail: SubscriptionSummary = subscriptionSummaryEmail.copy(paperlessReference = false)

  val subscriptionSummaryPostNoEmail: SubscriptionSummary = SubscriptionSummary(
    paperlessReference = false,
    emailAddress = None,
    emailVerification = None,
    bouncedEmail = None,
    correspondenceAddress = correspondenceAddress,
    countryCode = Some(countryCode)
  )

  val userAnswers: ContactPreferenceUserAnswers = ContactPreferenceUserAnswers(
    appaId = vppaId,
    userId = userId,
    subscriptionSummary = subscriptionSummaryEmail,
    emailAddress = None,
    verifiedEmailAddresses = verifiedEmailAddresses,
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(false))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val userAnswersEmailUpdate: ContactPreferenceUserAnswers = ContactPreferenceUserAnswers(
    appaId = vppaId,
    userId = userId,
    subscriptionSummary = subscriptionSummaryEmail,
    emailAddress = None,
    verifiedEmailAddresses = Set(emailAddress2),
    data = JsObject(Seq()),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val userAnswersPostWithEmail: ContactPreferenceUserAnswers = ContactPreferenceUserAnswers(
    appaId = vppaId,
    userId = userId,
    subscriptionSummary = subscriptionSummaryPostWithEmail,
    emailAddress = Some(emailAddress),
    verifiedEmailAddresses = Set(emailAddress),
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(true))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val userAnswersPostWithUnverifiedEmail: ContactPreferenceUserAnswers = userAnswersPostWithEmail.copy(
    subscriptionSummary = subscriptionSummaryPostWithEmail.copy(emailVerification = Some(false))
  )

  val userAnswersPostWithBouncedEmail: ContactPreferenceUserAnswers = userAnswersPostWithEmail.copy(
    subscriptionSummary = subscriptionSummaryPostWithEmail.copy(bouncedEmail = Some(true))
  )

  val userAnswersPostWithBouncedEmailUpdate: ContactPreferenceUserAnswers = userAnswersPostWithEmail.copy(
    subscriptionSummary = subscriptionSummaryPostWithEmail.copy(bouncedEmail = Some(true))
  )

  val userAnswersPostNoEmail: ContactPreferenceUserAnswers = ContactPreferenceUserAnswers(
    appaId = vppaId,
    userId = userId,
    subscriptionSummary = subscriptionSummaryPostNoEmail,
    emailAddress = Some(emailAddress),
    verifiedEmailAddresses = verifiedEmailAddresses,
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(true))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val emptyUserAnswers: ContactPreferenceUserAnswers = ContactPreferenceUserAnswers(
    appaId = vppaId,
    userId = userId,
    subscriptionSummary = subscriptionSummaryEmail,
    emailAddress = None,
    verifiedEmailAddresses = Set.empty[String],
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
