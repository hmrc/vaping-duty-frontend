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

package services

import connectors.UserAnswersConnector
import models.emailverification.ErrorModel
import models.{ContactPreferenceUserAnswers, UserDetails}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UserAnswersService @Inject() (userAnswersConnector: UserAnswersConnector)(implicit ec: ExecutionContext) {
  
  def get(vpdId: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, ContactPreferenceUserAnswers]] =
    userAnswersConnector.get(vpdId)
  
  def set(userAnswers: ContactPreferenceUserAnswers)(implicit hc: HeaderCarrier): Future[Either[ErrorModel, HttpResponse]] = {
    userAnswersConnector.set(userAnswers).map { response =>
      if (response.status >= 200 && response.status < 300) {
        Right(response)
      } else {
        Left(ErrorModel(response.status, s"Unexpected error setting user answers, status: ${response.status}"))
      }
    }
  }

  def createUserAnswers(userDetails: UserDetails)
                       (implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, ContactPreferenceUserAnswers]] = {
    userAnswersConnector.createUserAnswers(userDetails)
  }

  def keepAlive(userId: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] =
    userAnswersConnector.keepAlive(userId)

  def clear(vpdId: String)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] =
    userAnswersConnector.clear(vpdId)
}
