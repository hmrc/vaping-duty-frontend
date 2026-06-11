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
import models.identifiers.{CredentialId, GroupId, InternalId, PeriodKey, VpdId}
import models.obligations.{ObligationDetails, ObligationItem, ObligationStatus, ObligationsResponse}
import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import models.returns.view.*
import models.returns.{DeclarationDetails, ReturnsUserAnswers, TotalDutyDue, VapingProductsProduced}
import pages.returns.EnterDutyAmountPage
import play.api.libs.json.{JsObject, Json}

import java.time.{Clock, Instant, LocalDate, LocalDateTime, ZoneId, ZoneOffset}

trait TestData {
  val vpdId: VpdId = VpdId(id = "VPPAID01")
  val vpdRef: Option[String] = Some("VPDREF123")
  val btaLink = "http://localhost:9020/business-account?useServiceNavigation"
  val groupId: GroupId = GroupId(id = "groupid")
  val periodKey = PeriodKey("26AF")
  val ukTimeZoneStringId = "Europe/London"
  val clock: Clock = Clock.fixed(LocalDateTime.parse("2026-02-12T11:13:06").toInstant(ZoneOffset.UTC), ZoneId.of(ukTimeZoneStringId))
  val testDutyRate = BigDecimal("3.15")

  val sampleRegularReturn: models.returns.RegularReturn = models.returns.RegularReturn(
    taxType = "351",
    dutyRate = testDutyRate,
    amountProducedLiquid = BigDecimal("1000"),
    dutyDue = BigDecimal("3150")
  )

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
    vpdId = vpdId.value,
    periodKey = periodKey.value,
    data = JsObject.empty,
    startedTime = Instant.now(clock),
    lastUpdated = Instant.now(clock)
  )

  val testSummaryList: uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList =
    uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList(rows = Seq.empty)

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
    paymentDueDate = Option(LocalDate.now()),
    declaration = testDeclarationDetails
  )

  val totalInMl = returnsUserAnswers.get(EnterDutyAmountPage).fold(BigDecimal(0))(value => BigDecimal(value))

  val zeroValue = BigDecimal(0)

  val totalDue = totalInMl - zeroValue

  val testDeclarationDetails: DeclarationDetails = DeclarationDetails(
    fullName = "Test User",
    capacityInWhichSigned = "Director",
    signeesEmailAddress = "test@example.com"
  )

  val testSubmitReturnRequest = ReturnCreateRequest(
    periodKey.toString,
    VapingProductsProduced(Seq.empty, Seq.empty),
    TotalDutyDue(totalInMl, zeroValue, zeroValue, zeroValue, zeroValue, totalDue),
    testDeclarationDetails
  )

  def createMockObligationsResponse(): ObligationsResponse = {
    val currentDate = LocalDate.now()

    ObligationsResponse(
      obligation = Seq(
        // Outstanding return - Due
        ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = ObligationStatus.O.toString,
            iCFromDate = LocalDate.of(2027, 12, 1),
            iCToDate = LocalDate.of(2027, 12, 31),
            iCDateReceived = None,
            iCDueDate = currentDate.plusDays(10),
            periodKey = "27AL"
          )
        ),
        // Outstanding return - Overdue
        ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = ObligationStatus.O.toString,
            iCFromDate = LocalDate.of(2027, 11, 1),
            iCToDate = LocalDate.of(2027, 11, 30),
            iCDateReceived = None,
            iCDueDate = currentDate.minusDays(5),
            periodKey = "27AK"
          )
        ),
        // Completed return
        ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "F",
            iCFromDate = LocalDate.of(2027, 10, 1),
            iCToDate = LocalDate.of(2027, 10, 31),
            iCDateReceived = Some(LocalDate.of(2027, 11, 15)),
            iCDueDate = LocalDate.of(2027, 11, 30),
            periodKey = "27AJ"
          )
        )
      )
    )
  }

  def createReturnDisplayResponse(): ReturnDisplayResponse = {
    ReturnDisplayResponse(
      success = ReturnDisplaySuccess(
        processingDate = Instant.now(clock),
        idDetails = Some(
          IdDetails(
            vpdReference = vpdRef.get,
            submissionId = Some("SUB123456789")
          )
        ),
        chargeDetails = Some(
          ChargeDetails(
            periodKey = periodKey.value,
            chargeReference = Some("XVC123456789012"),
            periodFrom = LocalDate.of(2026, 6, 1),
            periodTo = LocalDate.of(2026, 6, 30),
            receiptDate = Instant.now(clock)
          )
        ),
        vapingProductsProduced = Some(
          VapingProductsProduced(
            nilReturn = Seq.empty,
            regularReturn = Seq(sampleRegularReturn)
          )
        ),
        overDeclaration = Some(
          OverDeclaration(
            overDeclFilled = "true",
            reasonForOverDecl = Some("Correction of previous return"),
            overDeclarationProducts = Some(
              Seq(
                OverDeclarationProduct(
                  returnPeriodAffected = "26AE",
                  taxType = "311",
                  dutyRate = BigDecimal("0.50"),
                  amountOverDeclaration = BigDecimal("1000.00"),
                  dutyDue = BigDecimal("500.00")
                )
              )
            )
          )
        ),
        underDeclaration = Some(
          UnderDeclaration(
            underDeclFilled = "true",
            reasonForUnderDec = Some("Additional products found"),
            underDeclarationProducts = Some(
              Seq(
                UnderDeclarationProduct(
                  returnPeriodAffected = "26AD",
                  taxType = "312",
                  dutyRate = BigDecimal("0.75"),
                  amountUnderDeclaration = BigDecimal("500.00"),
                  dutyDue = BigDecimal("375.00")
                )
              )
            )
          )
        ),
        spoiltProduct = Some(
          SpoiltProduct(
            spoiltProductFilled = "true",
            spoiltProducts = Some(
              Seq(
                SpoiltProductItem(
                  returnPeriodAffected = "26AE",
                  taxType = "311",
                  dutyRate = BigDecimal("0.50"),
                  amountSpoilt = BigDecimal("200.00"),
                  dutyDue = BigDecimal("100.00")
                )
              )
            )
          )
        ),
        totalDutyDue = Some(
          TotalDutyDue(
            totalDutyDueVapingProducts = BigDecimal("1000.00"),
            totalDutyOverDeclaration = BigDecimal("500.00"),
            totalDutyUnderDeclaration = BigDecimal("375.00"),
            totalDutySpoiltProduct = BigDecimal("100.00"),
            adjustmentAmount = BigDecimal("50.00"),
            totalDutyDue = BigDecimal("1825.00")
          )
        ),
        totalDutyDueByTaxType = Some(
          TotalDutyDue(
            totalDutyDueVapingProducts = BigDecimal("1000.00"),
            totalDutyOverDeclaration = BigDecimal("500.00"),
            totalDutyUnderDeclaration = BigDecimal("375.00"),
            totalDutySpoiltProduct = BigDecimal("100.00"),
            adjustmentAmount = BigDecimal("50.00"),
            totalDutyDue = BigDecimal("1825.00")
          )
        ),
        otherOptions = Some(
          OtherOptions(
            otherOptions = "true",
            vapingProdManufactured = Some("true"),
            otherVapingProduct = Some("false"),
            destroyed = Some(BigDecimal("50.00")),
            imported = Some(BigDecimal("100.00")),
            exported = Some(BigDecimal("75.00")),
            amtRecieved = Some(BigDecimal("25.00"))
          )
        ),
        declaration = testDeclarationDetails
      )
    )
  }
}
