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

import com.google.inject.Inject
import models.requests.{IdentifierRequest, SignedInRequest}
import controllers.routes
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{internalId, *}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

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
      id =>
        val identifiers = for {
          userId <- id.toRight("Unable to extract internalId from GovernmentGateway auth provider.")
        } yield {
          (userId)
        }

        identifiers match {
          case Right(userId) => block(SignedInRequest(request, userId = Some(userId)))
          case Left(error) => Future.failed(AuthorisationException.fromString(error))
        }
    } recover { case _ =>
      Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }
}
