/*
 * Copyright 2023 HM Revenue & Customs
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

import scala.concurrent.{ExecutionContext, Future}

import com.google.inject.Inject
import controllers.routes
import models.requests.SignedInRequest
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc._

import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.internalId
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

/**
 * This auth action will verify the basic requirement that the User
 * is signed in to GovernmentGateway and does not carry out any kind
 * of enrolment checks or verification. Use `IdentifyWith[out]EnrolmentAction`
 * for pages which require the user to be appropriately authenticated
 */

trait CheckSignedInAction
  extends ActionBuilder[SignedInRequest, AnyContent]
    with ActionFunction[Request, SignedInRequest]

class CheckSignedInActionImpl @Inject() (
                                          override val authConnector: AuthConnector,
                                          val parser: BodyParsers.Default
                                        )(implicit val executionContext: ExecutionContext)
  extends CheckSignedInAction
    with AuthorisedFunctions
    with Logging {

  private def predicate: Predicate =
    AuthProviders(GovernmentGateway)

  override def invokeBlock[A](request: Request[A], block: SignedInRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    
    authorised(predicate).retrieve(internalId) {
      optInternalId => optInternalId.map(internalId => SignedInRequest(request, internalId)).
                                     map(signedInRequest => block(signedInRequest)).
                                     getOrElse(Future.failed(AuthorisationException.fromString("Unable to retrieve internalId.")))
    } recoverWith { case e: AuthorisationException =>
      logger.warn(s"Got AuthorisationException: ${e.reason}")
      Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
    }
  }
}
