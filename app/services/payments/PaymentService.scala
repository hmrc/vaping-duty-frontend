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

package services.payments

import connectors.payments.PaymentConnector
import models.identifiers.VpdId
import models.payments.{StartPaymentRequest, StartPaymentResponse}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentService @Inject()(
                                connector: PaymentConnector,
                                financialDataService: FinancialDataService
                              )(using ExecutionContext) {

  def startPayment(
                    vpdId: VpdId,
                    chargeReference: String,
                    returnUrl: String,
                    backUrl: String
                  )(using HeaderCarrier): Future[StartPaymentResponse] = {

      for {
        payment <- financialDataService.getOutstandingPayment(vpdId, chargeReference)
        amountInPence = (payment.amountDue * 100).toLong
      request = StartPaymentRequest(
        vapingDutyReference = vpdId.value,
        amountInPence = amountInPence,
        chargeReferenceNumber = Some(chargeReference),
        returnUrl = returnUrl,
        backUrl = backUrl
      )
      response <- connector.startPayment(request)
    } yield response
  }

  def startBtaPayment(
                       vpdId: VpdId,
                       chargeReferenceOpt: Option[String],
                       returnUrl: String,
                       backUrl: String
                     )(using HeaderCarrier): Future[StartPaymentResponse] =

    chargeReferenceOpt.fold(btaMultipleCharges(vpdId, amountInPenceForMultipleCharges(vpdId), returnUrl, backUrl))
      (chargeReference => btaPaymentWithChargeReference(vpdId, chargeReference, amountInPenceForChargeRef(vpdId, chargeReference), returnUrl, backUrl))

  private def btaMultipleCharges(vpdId: VpdId, amountInPence: Future[Long], returnUrl: String, backUrl: String)(using HeaderCarrier) = {
    startBtaPayment(vpdId, None, amountInPence, returnUrl, backUrl)
  }

  private def amountInPenceForMultipleCharges(vpdId: VpdId)(using HeaderCarrier) = {
    for {
      payments <- financialDataService.getPayments(vpdId)
      amount = payments.totalAccountBalance.filter(_ > 0).getOrElse(
        // scalafix:off DisableSyntax.throw
        throw new NoSuchElementException(s"No positive outstanding balance for VpdId: ${vpdId.value}")
      )
      amountInPence = (amount * 100).toLong
    }
    yield amountInPence
  }

  private def btaPaymentWithChargeReference(vpdId: VpdId, chargeReference: String, amountInPence: Future[Long], returnUrl: String, backUrl: String)(using HeaderCarrier) = {
    startBtaPayment(vpdId, Some(chargeReference), amountInPence, returnUrl, backUrl)
  }

  private def startBtaPayment(vpdId: VpdId,
                              chargeRef: Option[String],
                              amountInPenceFuture: Future[Long],
                              returnUrl: String,
                              backUrl: String)(using HeaderCarrier) = {
    for {
      amountInPence <- amountInPenceFuture
      request = StartPaymentRequest(
        vapingDutyReference = vpdId.value,
        amountInPence = amountInPence,
        chargeReferenceNumber = chargeRef,
        returnUrl = returnUrl,
        backUrl = backUrl
      )
      response <- connector.startBtaPayment(request)
    } yield response
  }

  private def amountInPenceForChargeRef(vpdId: VpdId, chargeReference: String)(using HeaderCarrier) = {
    for {
      payment <- financialDataService.getOutstandingPayment(vpdId, chargeReference)
      amountInPence = (payment.amountDue * 100).toLong
    } yield amountInPence
  }
}
