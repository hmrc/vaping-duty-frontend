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
import config.FrontendAppConfig
import connectors.returns.ObligationsConnector
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.{ObligationDetails, ObligationItem}
import models.returns.ReturnsUserAnswers
import queries.returns.ObligationsQuery
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ObligationService @Inject()(
                                   obligationsConnector: ObligationsConnector,
                                   appConfig: FrontendAppConfig
                                 )(using ExecutionContext) {

  def getObligations(vpdId: VpdId)(using HeaderCarrier): Future[Seq[ObligationDetails]] =
    obligationsConnector.getObligations(vpdId).map { response =>
      filterObligationsByVpdId(response.obligation, vpdId)
    }

  def getCachedObligations(ua: ReturnsUserAnswers, vpdId: VpdId)
                          (using HeaderCarrier): Future[Seq[ObligationDetails]] =
    ua.get(ObligationsQuery) match {
      case Some(obligations) => Future.successful(obligations)
      case None              => getObligations(vpdId)
    }

  def withCachedObligations(ua: ReturnsUserAnswers, obligations: Seq[ObligationDetails]): ReturnsUserAnswers =
    ua.set(ObligationsQuery, obligations).getOrElse(ua)

  def getObligationByPeriodKey(vpdId: VpdId, periodKey: PeriodKey)
                              (using HeaderCarrier): Future[Option[ObligationDetails]] =
    getObligations(vpdId).map(_.find(_.periodKey == periodKey.toString))

  private def filterObligationsByVpdId(
                                        obligations: Seq[ObligationItem],
                                        vpdId: VpdId
                                      ): Seq[ObligationDetails] =
    obligations
      .filter(_.identification.exists(id =>
        id.referenceType == appConfig.enrolmentIdentifierKey &&
          id.referenceNumber == vpdId.value
      ))
      .flatMap(_.obligationDetails)
}
