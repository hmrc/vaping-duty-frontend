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
import scala.concurrent.Future

class PaymentService @Inject()(
  connector: PaymentConnector
) {

  def startPayment(
    vpdId: VpdId,
    chargeReference: String,
    amountInPence: Long,
    returnUrl: String,
    backUrl: String
  )(using HeaderCarrier): Future[StartPaymentResponse] = {

    val request = StartPaymentRequest(
      vapingDutyReference = vpdId.value,
      amountInPence = amountInPence,
      chargeReferenceNumber = chargeReference,
      returnUrl = returnUrl,
      backUrl = backUrl
    )

    connector.startPayment(request)
  }
}