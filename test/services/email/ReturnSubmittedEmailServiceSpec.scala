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

package services.email

import base.SpecBase
import connectors.email.EmailConnector
import models.email.*
import models.obligations.ObligationDetails
import models.returns.*
import models.returns.submit.{ReturnCreateRequest, ReturnSubmittedResponse}
import models.returns.view.{OtherOptions, OverDeclaration, SpoiltProduct, UnderDeclaration}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{ACCEPTED, BAD_REQUEST}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.time.{Instant, LocalDate}
import scala.concurrent.Future

class ReturnSubmittedEmailServiceSpec extends SpecBase {

  private val declaration = DeclarationDetails(
    fullName = "John Smith",
    capacityInWhichSigned = "Director",
    signeesEmailAddress = "john.smith@example.com"
  )

  private val nilTotalDutyDue = TotalDutyDue(0.0, 0.0, 0.0, 0.0, 0.0)

  private def submission(totalDue: BigDecimal) = ReturnCreateRequest(
    "24KA",
    VapingProductsProduced("0", Seq()),
    Some(UnderDeclaration("0", None, Some(Seq()))),
    Some(OverDeclaration("0", None, Some(Seq()))),
    Some(SpoiltProduct("0", Some(Seq()))),
    nilTotalDutyDue.copy(totalDue = totalDue),
    Some(OtherOptions("0", None, None)),
    declaration
  )

  private val obligation = ObligationDetails(
    openOrFulfilledStatus = "O",
    iCFromDate = LocalDate.of(2026, 10, 1),
    iCToDate = LocalDate.of(2026, 10, 31),
    iCDateReceived = None,
    iCDueDate = LocalDate.of(2026, 11, 7),
    periodKey = "24KA"
  )

  private def response(amount: BigDecimal, chargeReference: Option[String] = Some("VPD38270541977")) = ReturnSubmittedResponse(
    processingDate = Instant.parse("2026-11-03T10:15:00Z"),
    vpdReferenceNumber = "GBWK1234567WK",
    submissionId = Some("123456789012"),
    chargeReference = chargeReference,
    amount = amount,
    paymentDueDate = Some(LocalDate.of(2026, 11, 15))
  )

  private given HeaderCarrier = HeaderCarrier()

  "sendReturnSubmittedEmail" - {

    "must send a DutyDueEmail when the amount is positive" in new SetUp {
      stubSuccessfulPost()

      service.sendReturnSubmittedEmail(submission(220.00), response(220.00), obligation).futureValue

      val sent = captureEmail()
      sent mustBe a[DutyDueEmail]
      val email = sent.asInstanceOf[DutyDueEmail]
      email.to mustBe List("john.smith@example.com")
      email.parameters mustBe DutyDueEmailParameters(
        recipientName = "John Smith",
        returnPeriod = "October 2026",
        submissionDate = "3 November 2026",
        chargeReference = "VPD38270541977",
        amountDue = "£220",
        paymentDueDate = "15 November 2026"
      )
    }

    "must send a NilReturnEmail when the amount is zero" in new SetUp {
      stubSuccessfulPost()

      service.sendReturnSubmittedEmail(submission(0), response(0), obligation).futureValue

      val sent = captureEmail()
      sent mustBe a[NilReturnEmail]
      sent.asInstanceOf[NilReturnEmail].parameters mustBe NilReturnEmailParameters(
        recipientName = "John Smith",
        returnPeriod = "October 2026",
        submissionDate = "3 November 2026"
      )
    }

    "must send a CreditDueEmail with the absolute amount when the amount is negative" in new SetUp {
      stubSuccessfulPost()

      service.sendReturnSubmittedEmail(submission(-50.00), response(-50.00), obligation).futureValue

      val sent = captureEmail()
      sent mustBe a[CreditDueEmail]
      sent.asInstanceOf[CreditDueEmail].parameters mustBe CreditDueEmailParameters(
        recipientName = "John Smith",
        returnPeriod = "October 2026",
        submissionDate = "3 November 2026",
        creditAmount = "£50"
      )
    }

    "must resolve successfully even when the connector returns a non-2xx response" in new SetUp {
      when(mockEmailConnector.postEmail(any())(using any()))
        .thenReturn(Future.successful(HttpResponse(status = BAD_REQUEST, body = "")))

      noException must be thrownBy service.sendReturnSubmittedEmail(submission(220.00), response(220.00), obligation).futureValue
    }

    "must resolve successfully even when the connector call fails" in new SetUp {
      when(mockEmailConnector.postEmail(any())(using any()))
        .thenReturn(Future.failed(new RuntimeException("connection refused")))

      noException must be thrownBy service.sendReturnSubmittedEmail(submission(220.00), response(220.00), obligation).futureValue
    }
  }

  class SetUp {
    val mockEmailConnector: EmailConnector = mock[EmailConnector]
    val service = new ReturnSubmittedEmailService(mockEmailConnector)

    def stubSuccessfulPost(): Unit =
      when(mockEmailConnector.postEmail(any())(using any()))
        .thenReturn(Future.successful(HttpResponse(status = ACCEPTED, body = "")))

    def captureEmail(): Email = {
      val captor = ArgumentCaptor.forClass(classOf[Email])
      verify(mockEmailConnector).postEmail(captor.capture())(using any())
      captor.getValue
    }
  }
}
