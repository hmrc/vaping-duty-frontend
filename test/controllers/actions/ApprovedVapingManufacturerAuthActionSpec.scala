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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.whenReady
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.mvc.{Action, AnyContent, BodyParsers, Results}
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

  "Auth Action" - {

    val appConfig = mock[FrontendAppConfig]
    when(appConfig.enrolmentServiceName).thenReturn("HMRC-VPD-ORG")
    when(appConfig.enrolmentIdentifierKey).thenReturn("VPPAID")
    when(appConfig.loginUrl).thenReturn("login-url")
    when(appConfig.loginContinueUrl).thenReturn("login-continue-url")

    val bodyParsers = mock[BodyParsers.Default]

    "when authenticated and authorised" - {
      "executes the block passed " in {

        val successfulAuthConnector = mock[AuthConnector]
        when(
          successfulAuthConnector.authorise(any[Predicate],
                                            ArgumentMatchers.eq(internalId and groupIdentifier and allEnrolments)
                                           )(any[HeaderCarrier], any[ExecutionContext])).
            thenReturn(Future.successful[Option[String] ~ Option[String] ~ Enrolments](
              Some("test-internal-id") and Some("test-group-id") and Enrolments(Set(
                  Enrolment(
                    key = "HMRC-VPD-ORG",
                    identifiers = Seq(EnrolmentIdentifier(key = "VPPAID", value = "TestVpdId")),
                    state = "TestState")))))

        val authAction = new ApprovedVapingManufacturerAuthActionImpl(successfulAuthConnector, appConfig, bodyParsers)
        val controller = new Harness(authAction)
        val result = controller.onPageLoad()(FakeRequest())

        status(result) mustBe OK
      }

      "Allows UnauthorisedException from a Connector called from the executed block to pass through and be handled by the framework" in {

        val successfulAuthConnector = mock[AuthConnector]
        when(
          successfulAuthConnector.authorise(any[Predicate],
                                            ArgumentMatchers.eq(internalId and groupIdentifier and allEnrolments)
                                           )(any[HeaderCarrier], any[ExecutionContext])).
            thenReturn(Future.successful[Option[String] ~ Option[String] ~ Enrolments](
              Some("test-internal-id") and Some("test-group-id") and Enrolments(Set(
                  Enrolment(
                    key = "HMRC-VPD-ORG",
                    identifiers = Seq(EnrolmentIdentifier(key = "VPPAID", value = "TestVpdId")),
                    state = "TestState")))))

        val authAction = new ApprovedVapingManufacturerAuthActionImpl(successfulAuthConnector, appConfig, bodyParsers)
        val controller = new ExceptionThrowingHarness(authAction)

        whenReady(controller.onPageLoad()(FakeRequest()).failed) { ex =>
          ex shouldBe a[UnauthorizedException]
        }
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
        redirectLocation(result) mustBe Some(routes.UnauthorisedController.onPageLoad().url)
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
