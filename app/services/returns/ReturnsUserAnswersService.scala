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

import connectors.contactPreference.PreferenceUserAnswersConnector
import connectors.returns.ReturnsUserAnswersConnector
import models.contactPreference.{PreferenceUserAnswers, UserDetails}
import models.emailverification.ErrorModel
import models.identifiers.{InternalId, VpdId}
import models.returns.ReturnsUserAnswers
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReturnsUserAnswersService @Inject()(userAnswersConnector: ReturnsUserAnswersConnector)(implicit ec: ExecutionContext) {
  
  def get(vpdId: VpdId)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, ReturnsUserAnswers]] =
    userAnswersConnector.get(vpdId)
  
  def set(userAnswers: ReturnsUserAnswers)(implicit hc: HeaderCarrier): Future[Either[ErrorModel, HttpResponse]] = {
    userAnswersConnector.set(userAnswers).map { response =>
      if (response.status >= 200 && response.status < 300) {
        Right(response)
      } else {
        Left(ErrorModel(response.status, s"Unexpected error setting user answers, status: ${response.status}"))
      }
    }
  }

  def keepAlive(internalId: InternalId)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] =
    userAnswersConnector.keepAlive(internalId)

  def clear(internalId: InternalId)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] =
    userAnswersConnector.clear(internalId)
}
