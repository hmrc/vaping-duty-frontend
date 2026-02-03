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

import base.SpecBase
import models.requests.SignedInRequest
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{BodyParsers, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals

import scala.concurrent.Future

class CheckSignedInActionSpec extends SpecBase with MockitoSugar {
  val app = applicationBuilder(userAnswers = None)
  val testContent                            = "Test"
  val defaultBodyParser: BodyParsers.Default = app.injector().instanceOf[BodyParsers.Default]
  val mockAuthConnector: AuthConnector       = mock[AuthConnector]

  val checkSignedInAction = new CheckSignedInActionImpl(mockAuthConnector, defaultBodyParser)

  val signedInKey = "signedIn"

  "invokeBlock" - {

    "execute the block and return signed in if signed in to Government Gateway" in {
      stubAuthResponse(Future.successful(Some("internalId123")))

      val result = checkSignedInAction.invokeBlock(
        FakeRequest(), block = request => Future.successful(Results.Ok(request.internalId))
      )

      status(result)          mustBe OK
      contentAsString(result) mustBe "internalId123"
    }

    "send users to UnauthorisedController when not logged in to an account" in {
      List(
        BearerTokenExpired(),
        MissingBearerToken(),
        InvalidBearerToken(),
        SessionRecordNotFound()
      ).foreach { exception =>
        stubAuthResponse(Future.failed(exception))

        val result = checkSignedInAction.invokeBlock(
          SignedInRequest(FakeRequest(), internalId = "id"), block = _ => Future.failed(SessionRecordNotFound())
        )

        status(result)                 mustBe SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.UnauthorisedController.onPageLoad().url
      }
    }
  }

  private def stubAuthResponse(authResponse: Future[Option[String]]) = {
    when(
      mockAuthConnector.authorise(
        eqTo(AuthProviders(GovernmentGateway)),
        eqTo(Retrievals.internalId)
      )(any(), any())
    ).thenReturn(authResponse)
  }
}
