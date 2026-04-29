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

package services.returns

import connectors.returns.ObligationsConnector
import models.identifiers.VpdId
import models.returns.{ObligationDetails, ObligationItem, ObligationsResponse}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ObligationsService @Inject()(connector: ObligationsConnector)(using ExecutionContext) {

  def get(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, ObligationsResponse]] = {
    //connector.getObligations(vpdId)
    // Replace with connector call when ready
    Future.successful(Right(createMockObligationsResponse()))
  }

  private def createMockObligationsResponse(): ObligationsResponse = {
    val currentDate = LocalDate.now()

    ObligationsResponse(
      obligation = Seq(
        // Outstanding return - Due
        ObligationItem(
          identification = None,
          obligationDetails = ObligationDetails(
            openOrFulfilledStatus = "O",
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
            openOrFulfilledStatus = "O",
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
}


