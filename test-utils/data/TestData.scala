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

import models.contactPreference.{PreferenceUserAnswers, SubscriptionSummary, UserDetails}
import models.emailverification.*
import models.identifiers.{CredentialId, GroupId, InternalId, VpdId}
import models.returns.{ReturnCreateRequest, ReturnSubmittedResponse, ReturnsUserAnswers, TotalDutyDue, VapingProductsProduced}
import pages.returns.EnterDutyAmountPage
import play.api.libs.json.{JsObject, Json}

import java.time.{Clock, Instant, LocalDate, ZoneId}

trait TestData {
  val vpdId: VpdId = VpdId(id = "VPPAID01")
  val vpdRef: Option[String] = Some("VPDREF123")
  val btaLink = "http://localhost:9020/business-account"
  val groupId: GroupId = GroupId(id = "groupid")
  val ukTimeZoneStringId = "Europe/London"
  val epochTime = 1718118467838L
  val clock: Clock = Clock.fixed(Instant.ofEpochMilli(epochTime), ZoneId.of(ukTimeZoneStringId))

  val internalId: InternalId = InternalId(id = "user-id")
  val credId: CredentialId = CredentialId(id = "cred-id")

  val userDetails: UserDetails = UserDetails(vpdId.value, internalId.value)

  val emailAddress = "john.doe@example.com"
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

  val userAnswers: PreferenceUserAnswers = PreferenceUserAnswers(
    vpdId = vpdId.toString,
    internalId = internalId.toString,
    subscriptionSummary = subscriptionSummaryEmail,
    emailAddress = None,
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(false))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val userAnswersEmailUpdate: PreferenceUserAnswers = PreferenceUserAnswers(
    vpdId = vpdId.toString,
    internalId = internalId.toString,
    subscriptionSummary = subscriptionSummaryEmail,
    emailAddress = None,
    data = JsObject(Seq()),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val userAnswersPostWithEmail: PreferenceUserAnswers = PreferenceUserAnswers(
    vpdId = vpdId.toString,
    internalId = internalId.toString,
    subscriptionSummary = subscriptionSummaryPostWithEmail,
    emailAddress = Some(emailAddress),
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(true))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val userAnswersPostWithUnverifiedEmail: PreferenceUserAnswers = userAnswersPostWithEmail.copy(
    subscriptionSummary = subscriptionSummaryPostWithEmail.copy(emailVerification = Some(false))
  )

  val userAnswersPostWithBouncedEmail: PreferenceUserAnswers = userAnswersPostWithEmail.copy(
    subscriptionSummary = subscriptionSummaryPostWithEmail.copy(bouncedEmail = Some(true))
  )

  val userAnswersPostWithBouncedEmailUpdate: PreferenceUserAnswers = userAnswersPostWithEmail.copy(
    subscriptionSummary = subscriptionSummaryPostWithEmail.copy(bouncedEmail = Some(true))
  )

  val userAnswersPostNoEmail: PreferenceUserAnswers = PreferenceUserAnswers(
    vpdId = vpdId.toString,
    internalId = internalId.toString,
    subscriptionSummary = subscriptionSummaryPostNoEmail,
    emailAddress = Some(emailAddress),
    data = JsObject(Seq("contactPreferenceEmail" -> Json.toJson(true))),
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val emptyUserAnswers: PreferenceUserAnswers = PreferenceUserAnswers(
    vpdId = vpdId.toString,
    internalId = internalId.toString,
    subscriptionSummary = subscriptionSummaryEmail,
    emailAddress = None,
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val returnsUserAnswers: ReturnsUserAnswers = ReturnsUserAnswers(
    id = "",
    data = JsObject.empty,
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
    credId = credId.toString,
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

  val testReturnSubmissionResponse = ReturnSubmittedResponse(
    processingDate = Instant.now(),
    vpdReferenceNumber = "vpdReferenceNumber",
    submissionID = Option("submissionID"),
    chargeReference = Option("chargeReference"),
    amount = BigDecimal(0),
    paymentDueDate = Option(LocalDate.now())
  )

  val totalInMl = returnsUserAnswers.get(EnterDutyAmountPage).fold(BigDecimal(0))(value => BigDecimal(value))

  // Temp value
  val zeroValue = BigDecimal(0)

  // Will need to either get or pass the period key here
  val periodKey = "26AF"

  // Will need to enhance this much more
  val totalDue = totalInMl - zeroValue

  val testSubmitReturnRequest = ReturnCreateRequest(
    periodKey,
    VapingProductsProduced(Seq.empty, Seq.empty),
    TotalDutyDue(totalInMl, zeroValue, zeroValue, zeroValue, zeroValue, totalDue)
  )
}
