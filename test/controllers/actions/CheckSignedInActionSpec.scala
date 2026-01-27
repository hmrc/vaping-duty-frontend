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
import play.api.mvc.{BodyParsers, Result, Results}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import scala.concurrent.ExecutionContext.Implicits.global
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
      when(
        mockAuthConnector.authorise(
          eqTo(
            AuthProviders(GovernmentGateway)
          ),
          eqTo(Retrievals.internalId)
        )(any(), any())
      )
        .thenReturn(Future.successful(Some("id")))

      val result: Future[Result] = checkSignedInAction.invokeBlock(FakeRequest(), block = (_: SignedInRequest[_]) => Future.successful(Results.Ok(testContent)))

      status(result)          mustBe OK
      contentAsString(result) mustBe testContent
    }

    "send users to UnauthorisedController when not logged in to an account" in {
      List(
        BearerTokenExpired(),
        MissingBearerToken(),
        InvalidBearerToken(),
        SessionRecordNotFound()
      ).foreach { exception =>
        when(
          mockAuthConnector.authorise(
            eqTo(
              AuthProviders(GovernmentGateway)
            ),
            eqTo(EmptyRetrieval)
          )(any(), any())
        ).thenReturn(Future.failed(exception))

        val result: Future[Result] = checkSignedInAction.invokeBlock(
          SignedInRequest(FakeRequest(), userId = "id"),
          block = (_: SignedInRequest[_]) =>
            Future.failed(
              SessionRecordNotFound()
            )
          )

        status(result)                 mustBe SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.UnauthorisedController.onPageLoad().url
      }
    }
  }
}
