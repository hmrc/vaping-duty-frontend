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

import connectors.payments.FinancialDataConnector
import models.identifiers.VpdId
import models.payments.{OutstandingPayment, PaymentsResponse}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataService @Inject()(
  connector: FinancialDataConnector
)(using ExecutionContext) {

  def getPayments(vpdId: VpdId)(using HeaderCarrier): Future[PaymentsResponse] =
    connector.getPayments(vpdId)

  def getOutstandingPayment(vpdId: VpdId, chargeReference: String)(using HeaderCarrier): Future[OutstandingPayment] =
    getPayments(vpdId).map { payments =>
      payments.outstanding
        .find(_.chargeReference == chargeReference)
        .getOrElse(
          // scalafix:off DisableSyntax.throw
          throw new NoSuchElementException(s"No outstanding payment found for charge reference: $chargeReference")
        )
    }
}
