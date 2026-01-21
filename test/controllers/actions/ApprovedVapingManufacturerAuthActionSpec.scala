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
import models.requests.IdentifierRequest
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty, BodyParsers, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, groupIdentifier, internalId}
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

 class ApprovedVapingManufacturerAuthActionSpec extends SpecBase {

  class Harness(authAction: ApprovedVapingManufacturerAuthAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => Results.Ok }
  }

  class ExceptionThrowingHarness(authAction: ApprovedVapingManufacturerAuthAction) {
    def onPageLoad(): Action[AnyContent] = authAction { _ => throw new uk.gov.hmrc.http.UnauthorizedException("test exception") }
  }

   private val authConnector = mock[AuthConnector]

   private def stubAuthResponse(authResponse: Option[String] ~ Option[String] ~ Enrolments) = {
     when(
       authConnector.authorise(any[Predicate],
         ArgumentMatchers.eq(internalId and groupIdentifier and allEnrolments)
       )(any[HeaderCarrier], any[ExecutionContext])).
       thenReturn(Future.successful(authResponse))
   }

  "Auth Action" - {

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

        val authAction = new ApprovedVapingManufacturerAuthActionImpl(authConnector, appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe OK
      }

      "must pass expected retrievals to block" in {

        stubAuthResponse(
          Some(INTERNAL_ID) and Some(GROUP_IDENTIFIER) and VPD_ORG_VALID_ENROLMENT
        )

        val authAction = new ApprovedVapingManufacturerAuthActionImpl(authConnector, appConfig, bodyParsers)

        val request = FakeRequest()
        val block = mock[IdentifierRequest[AnyContentAsEmpty.type] => Future[Result]]

        when(block.apply(IdentifierRequest(request, "test-value", GROUP_IDENTIFIER, INTERNAL_ID))).
          thenReturn(Future.successful(Results.Ok))

        val result = authAction.invokeBlock(request, block)
        await(result)

        verify(block).apply(IdentifierRequest(request, "test-value", GROUP_IDENTIFIER, INTERNAL_ID))

      }


      "Allows UnauthorisedException from a Connector called from the executed block to pass through and be handled by the framework" in {

        stubAuthResponse(Some(INTERNAL_ID) and Some(GROUP_IDENTIFIER) and VPD_ORG_VALID_ENROLMENT)

        val authAction = new ApprovedVapingManufacturerAuthActionImpl(authConnector, appConfig, bodyParsers)
        val controller = new ExceptionThrowingHarness(authAction)

        whenReady(controller.onPageLoad()(FakeRequest()).failed) { ex =>
          ex shouldBe a[UnauthorizedException]
        }
      }

      "must give SEE_OTHER when missing auth data received " in {

        stubAuthResponse(None and Some(GROUP_IDENTIFIER) and VPD_ORG_VALID_ENROLMENT)

        val authAction = new ApprovedVapingManufacturerAuthActionImpl(authConnector, appConfig, bodyParsers)

        val result = authAction.invokeBlock(
          request = FakeRequest(),
          block = (_: IdentifierRequest[_]) => Future.successful(Results.Ok("Okay"))
        )

        status(result) shouldBe SEE_OTHER
      }

      "must give SEE_OTHER when incorrect enrolment service name present " in {

        stubAuthResponse(Some(INTERNAL_ID) and Some(GROUP_IDENTIFIER) and Enrolments(Set(
          Enrolment(
            key = "INCORRECT_ENROLMENT_SERVICE_NAME-ONLY",
            identifiers = Seq(EnrolmentIdentifier(key = VPD_ORG_IDENT_KEY, value = "TestId")),
            state = ENROLMENT_STATE
          )
        )))

        val authAction = new ApprovedVapingManufacturerAuthActionImpl(authConnector, appConfig, bodyParsers)

        val result = authAction.invokeBlock(
          request = FakeRequest(),
          block = (_: IdentifierRequest[_]) => Future.successful(Results.Ok("Okay"))
        )

        status(result) shouldBe SEE_OTHER
      }

      "must give SEE_OTHER when incorrect enrolment identifier key received " in {

        stubAuthResponse(Some(INTERNAL_ID) and Some(GROUP_IDENTIFIER) and Enrolments(Set(
          Enrolment(
            key = HMRC_VPD_ORG_ENROLMENT_NAME,
            identifiers = Seq(EnrolmentIdentifier(key = "IncorrectEnrolmentIdent", value = "TestId")),
            state = ENROLMENT_STATE
          )
        )))

        val authAction = new ApprovedVapingManufacturerAuthActionImpl(authConnector, appConfig, bodyParsers)

        val result = authAction.invokeBlock(
          request = FakeRequest(),
          block = (_: IdentifierRequest[_]) => Future.successful(Results.Ok("Okay"))
        )

        status(result) shouldBe SEE_OTHER
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

      "must redirect the user to the organisation sign in guidance page" in {

        val result = failingController(new UnsupportedAffinityGroup).onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.enrolment.routes.OrganisationSignInController.onPageLoad().url)
      }
    }

    "the user has an unsupported credential role" - {

      "must redirect the user to the unauthorised page" in {

        val result = failingController(new UnsupportedCredentialRole).onPageLoad()(FakeRequest())

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
      }
    }

    def failingController(authorisationException: AuthorisationException) = {
      new Harness(
        new ApprovedVapingManufacturerAuthActionImpl(
          new FakeFailingAuthConnector(authorisationException), appConfig, bodyParsers))
    }

  }

 }

class FakeFailingAuthConnector(exceptionToReturn: Throwable) extends AuthConnector {
  override def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
    Future.failed(exceptionToReturn)
}
