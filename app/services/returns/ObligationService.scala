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

package services.returns

import com.google.inject.{Inject, Singleton}
import connectors.returns.ObligationsConnector
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.{ObligationDetails, ObligationItem}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ObligationService @Inject()(obligationsConnector: ObligationsConnector)
                                 (using ExecutionContext) {

  private val REFERENCE_TYPE_ZVPD = "ZVPD"

  def getObligations(vpdId: VpdId)(using HeaderCarrier): Future[Seq[ObligationDetails]] =
    obligationsConnector.getObligations(vpdId).map { response =>
      filterObligationsByVpdId(response.obligation, vpdId)
    }

  def getObligationByPeriodKey(vpdId: VpdId, periodKey: PeriodKey)
                              (using HeaderCarrier): Future[Option[ObligationDetails]] =
    obligationsConnector.getObligations(vpdId).map { response =>
      filterObligationsByVpdId(response.obligation, vpdId)
        .find(_.periodKey == periodKey.toString)
    }

  private def filterObligationsByVpdId(
                                        obligations: Seq[ObligationItem],
                                        vpdId: VpdId
                                      ): Seq[ObligationDetails] =
    obligations
      .filter(_.identification.exists(id =>
        id.referenceType == REFERENCE_TYPE_ZVPD &&
          id.referenceNumber == vpdId.value
      ))
      .flatMap(_.obligationDetails)
}
