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

package controllers.actions

import connectors.SubscriptionConnector
import models.InsolvencyStatus
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait CheckInsolvencyAction extends ActionRefiner[IdentifierRequest, IdentifierRequest]

class CheckInsolvencyActionImpl @Inject()(
                                           subscriptionConnector: SubscriptionConnector
                                         )(implicit val executionContext: ExecutionContext)
  extends CheckInsolvencyAction with Logging {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, IdentifierRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    subscriptionConnector.getSubscriptionContactPreferences(request.enrolmentVpdId).map {
      case Right(subscription) =>
        subscription.insolvencyStatus match {
          case Some(InsolvencyStatus.Insolvent) =>
            logger.info(s"User with VpdId ${request.enrolmentVpdId.value} is insolvent, redirecting")
            Left(Redirect(controllers.routes.InsolventController.onPageLoad()))
          case _ =>
            Right(request)
        }
      case Left(error) =>
        logger.warn(s"Failed to fetch subscription data for VpdId ${request.enrolmentVpdId.value}: ${error.statusCode} - ${error.message}")
        Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
