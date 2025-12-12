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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq => eqTo
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import play.api.mvc.{BodyParsers, Result, Results}
import play.api.mvc.Results.Ok
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core.retrieve.EmptyRetrieval
import play.api.test.FakeRequest
import scala.concurrent.ExecutionContext.Implicits.global


import scala.collection.mutable
import scala.concurrent.Future

class CheckSignedInActionSpec extends SpecBase with MockitoSugar {
  val app = applicationBuilder(userAnswers = None)
  val testContent                            = "Test"
  val defaultBodyParser: BodyParsers.Default = app.injector().instanceOf[BodyParsers.Default]
  val mockAuthConnector: AuthConnector       = mock[AuthConnector]

  val checkSignedInAction = new CheckSignedInActionImpl(mockAuthConnector, defaultBodyParser)

  val signedInKey = "signedIn"

  def testAction(signedInStore: mutable.Map[String, Boolean]): SignedInRequest[_] => Future[Result] = { request =>
    signedInStore.synchronized {
      signedInStore.put(signedInKey, request.signedIn)
    }

    Future(Ok(testContent))
  }

  "invokeBlock" - {

    "execute the block and return signed in if signed in to Government Gateway" in {
      when(
        mockAuthConnector.authorise(
          eqTo(
            AuthProviders(GovernmentGateway)
          ),
          eqTo(EmptyRetrieval)
        )(any(), any())
      )
        .thenReturn(Future.unit)

      val signedInStore = mutable.Map[String, Boolean]()

      val result: Future[Result] = checkSignedInAction.invokeBlock(FakeRequest(), testAction(signedInStore))

      status(result)          mustBe OK
      contentAsString(result) mustBe testContent

      signedInStore.synchronized {
        signedInStore(signedInKey) mustBe true
      }
    }

    "execute the block and return not signed in if no longer authorised or never logged in" in {
      List(
        BearerTokenExpired(),
        MissingBearerToken(),
        InvalidBearerToken(),
        SessionRecordNotFound()
      ).foreach { exception =>
        when(
          mockAuthConnector.authorise[Unit](
            eqTo(
              AuthProviders(GovernmentGateway)
            ),
            eqTo(EmptyRetrieval)
          )(any(), any())
        ).thenReturn(Future.failed(exception))

        val signedInStore = mutable.Map[String, Boolean]()

        val result: Future[Result] = checkSignedInAction.invokeBlock(FakeRequest(), testAction(signedInStore))

        status(result)          mustBe OK
        contentAsString(result) mustBe testContent

        signedInStore.synchronized {
          signedInStore(signedInKey) mustBe false
        }
      }
    }
  }
}
