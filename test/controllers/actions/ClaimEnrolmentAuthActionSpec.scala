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

import base.SpecBase
import config.FrontendAppConfig
import controllers.routes
import models.requests.NoEnrolmentIdentifierRequest
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.CredentialStrength.strong
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, groupIdentifier, internalId}
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

 class EnrolmentClaimAuthActionSpec extends SpecBase {

  class Harness(authAction: EnrolmentClaimAuthAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  class ExceptionThrowingHarness(authAction: EnrolmentClaimAuthAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => throw new uk.gov.hmrc.http.UnauthorizedException("test exception") }
  }

   private val authConnector = mock[AuthConnector]

   private def predicate: Predicate =
     AuthProviders(GovernmentGateway) and
       CredentialStrength(strong) and
       Organisation and
       User and
       ConfidenceLevel.L50


   private def stubAuthResponse(authResponse: Option[String] ~ Option[String] ~ Enrolments) = {
     when(
       authConnector.authorise(ArgumentMatchers.eq(predicate),
         ArgumentMatchers.eq(internalId and groupIdentifier and allEnrolments)
       )(any[HeaderCarrier], any[ExecutionContext])).
       thenReturn(Future.successful(authResponse))
   }


  "Optional Enrolment Auth Action" - {

    val appConfig                    = mock[FrontendAppConfig]

    when(appConfig.enrolmentServiceName).thenReturn("HMRC-VPD-ORG")
    when(appConfig.enrolmentIdentifierKey).thenReturn("ZVPD")
    when(appConfig.loginUrl).thenReturn("login-url")
    when(appConfig.loginContinueUrl).thenReturn("login-continue-url")

    val HMRC_VPD_ORG_ENROLMENT_NAME  = appConfig.enrolmentServiceName
    val VPD_ORG_IDENT_KEY            = appConfig.enrolmentIdentifierKey

    val INTERNAL_ID                  = "test-internal-id"
    val GROUP_IDENTIFIER             = "test-group-id"
    val ENROLMENT_STATE              = "test-state"

    val bodyParsers                  = mock[BodyParsers.Default]

    val VPD_ORG_VALID_ENROLMENT      = Enrolments(Set(Enrolment(
      key = HMRC_VPD_ORG_ENROLMENT_NAME,
      identifiers = Seq(EnrolmentIdentifier(key = VPD_ORG_IDENT_KEY, value = "test-value")),
      state = ENROLMENT_STATE
    )))

    "when authenticated and authorised" - {

      "executes the block passed " in {

        stubAuthResponse(Some(INTERNAL_ID) and Some(GROUP_IDENTIFIER) and VPD_ORG_VALID_ENROLMENT)

        val authAction = new EnrolmentClaimAuthActionImpl(authConnector, appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe OK
      }

      "must pass expected retrievals to block" in {

        stubAuthResponse(
          Some(INTERNAL_ID) and Some(GROUP_IDENTIFIER) and VPD_ORG_VALID_ENROLMENT
        )

        val authAction = new EnrolmentClaimAuthActionImpl(authConnector, appConfig, bodyParsers)

        val request = FakeRequest()
        val block = mock[NoEnrolmentIdentifierRequest[AnyContentAsEmpty.type] => Future[Result]]

        when(block.apply(NoEnrolmentIdentifierRequest(request, Some("test-value"), GROUP_IDENTIFIER, INTERNAL_ID))).
          thenReturn(Future.successful(Results.Ok))

        val result = authAction.invokeBlock(request, block)
        await(result)

        verify(block).apply(NoEnrolmentIdentifierRequest(request, Some("test-value"), GROUP_IDENTIFIER, INTERNAL_ID))

      }


      "Allows UnauthorisedException from a Connector called from the executed block to pass through and be handled by the framework" in {

        stubAuthResponse(Some(INTERNAL_ID) and Some(GROUP_IDENTIFIER) and VPD_ORG_VALID_ENROLMENT)

        val authAction = new EnrolmentClaimAuthActionImpl(authConnector, appConfig, bodyParsers)
        val controller = new ExceptionThrowingHarness(authAction)

        whenReady(controller.onPageLoad()(FakeRequest()).failed) { ex =>
          ex shouldBe a[UnauthorizedException]
        }
      }

      "must give SEE_OTHER when missing auth data received " in {

        stubAuthResponse(None and Some(GROUP_IDENTIFIER) and VPD_ORG_VALID_ENROLMENT)

        val authAction = new EnrolmentClaimAuthActionImpl(authConnector, appConfig, bodyParsers)

        val result = authAction.invokeBlock(
          request = FakeRequest(),
          block = (_: NoEnrolmentIdentifierRequest[_]) => Future.successful(Results.Ok("Okay"))
        )

        status(result) shouldBe SEE_OTHER
      }

      "must allow users to authenticate with an enrolment only for another service" in {

        stubAuthResponse(Some(INTERNAL_ID) and Some(GROUP_IDENTIFIER) and Enrolments(Set(
          Enrolment(
            key = HMRC_VPD_ORG_ENROLMENT_NAME,
            identifiers = Seq(EnrolmentIdentifier(key = "IncorrectEnrolmentIdent", value = "TestId")),
            state = ENROLMENT_STATE
          )
        )))

        val authAction = new EnrolmentClaimAuthActionImpl(authConnector, appConfig, bodyParsers)

        val result = authAction.invokeBlock(
          request = FakeRequest(),
          block = (_: NoEnrolmentIdentifierRequest[_]) => Future.successful(Results.Ok("Okay"))
        )

        status(result) shouldBe OK
      }

      "must allow users to authenticate without any enrolment" in {

        stubAuthResponse(Some(INTERNAL_ID) and Some(GROUP_IDENTIFIER) and Enrolments(Set()))

        val authAction = new EnrolmentClaimAuthActionImpl(authConnector, appConfig, bodyParsers)

        val result = authAction.invokeBlock(
          request = FakeRequest(),
          block = (_: NoEnrolmentIdentifierRequest[_]) => Future.successful(Results.Ok("Okay"))
        )

        status(result) shouldBe OK
      }

    }

    "when the user hasn't logged in" - {

      "must redirect the user to log in " in {

        val result = failingController(new MissingBearerToken).onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value must startWith(appConfig.loginUrl)
      }
    }

    "the user's session has expired" - {

      "must redirect the user to log in " in {

        val result = failingController(new BearerTokenExpired).onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value must startWith(appConfig.loginUrl)
      }
    }

    "the user doesn't have sufficient enrolments" - {

      "must redirect the user to the unauthorised page" in {

        val result = failingController(new InsufficientEnrolments).onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
      }
    }

    "the user doesn't have sufficient confidence level" - {

      "must redirect the user to the unauthorised page" in {

        val result = failingController(new InsufficientConfidenceLevel).onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
      }
    }

    "the user used an unaccepted auth provider" - {

      "must redirect the user to the unauthorised page" in {

        val result = failingController(new UnsupportedAuthProvider).onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe routes.UnauthorisedController.onPageLoad().url
      }
    }

    "the user has an unsupported affinity group" - {

      "must redirect the user to the unauthorised page" in {

        val result = failingController(new UnsupportedAffinityGroup).onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.enrolment.routes.OrganisationSignInController.onPageLoad().url)
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {

        val result = failingController(new UnsupportedCredentialRole).onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.enrolment.routes.OrganisationSignInController.onPageLoad().url)
      }
    }

    def failingController(authorisationException: AuthorisationException) = {
      new Harness(
        new EnrolmentClaimAuthActionImpl(
          new FakeFailingNoEnrolmentAuthConnector(authorisationException), appConfig, bodyParsers))
    }

  }

 }

class FakeFailingNoEnrolmentAuthConnector(exceptionToReturn: Throwable) extends AuthConnector {
  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}
