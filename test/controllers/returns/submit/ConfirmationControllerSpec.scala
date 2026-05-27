/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.returns.submit

import base.SpecBase
import connectors.SubscriptionConnector
import connectors.returns.GetReturnsConnector
import models.contactPreference.SubscriptionContactPreferences
import models.identifiers.PeriodKey
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse
import viewmodels.returns.submit.ConfirmationViewModel
import views.html.returns.submit.ConfirmationEmailView

import scala.concurrent.Future

class ConfirmationControllerSpec extends SpecBase {

  "ConfirmationController" - {

    val viewReturnUrl = controllers.returns.view.routes.ViewIndividualReturnController.onPageLoad(periodKey).url

    "must return OK and the correct view for a GET" in {

      val mockSubscriptionConnector = mock[SubscriptionConnector]
      val mockGetReturnsConnector = mock[GetReturnsConnector]

      val application = applicationBuilder(returnsUserAnswers = Option(returnsUserAnswers))
        .overrides(bind[SubscriptionConnector].toInstance(mockSubscriptionConnector))
        .overrides(bind[GetReturnsConnector].toInstance(mockGetReturnsConnector))
        .build()

      running(application) {


        val contactPreference = SubscriptionContactPreferences(true, Option(emailAddress))

        when(mockSubscriptionConnector.getSubscriptionContactPreferences(any())(any()))
          .thenReturn(Future.successful(Right(contactPreference)))

        when(mockGetReturnsConnector.getReturn(any(), any())(using any()))
          .thenReturn(Future.successful(createReturnDisplayResponse()))

        val request = FakeRequest(GET, controllers.returns.submit.routes.ConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmationEmailView]

        val chargeReference = createReturnDisplayResponse().success.chargeDetails.get.chargeReference.get

        val vm = ConfirmationViewModel(returnsUserAnswers, emailAddress, chargeReference, btaLink, PeriodKey(periodKey), viewReturnUrl)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(vm)(request, messages(application)).toString
      }
    }

    "must redirect to journey recovery when there is an issue calling subscription summary API" in {

      val mockConnector = mock[SubscriptionConnector]

      val application = applicationBuilder(returnsUserAnswers = Option(returnsUserAnswers))
        .overrides(bind[SubscriptionConnector].toInstance(mockConnector))
        .build()

      running(application) {

        when(mockConnector.getSubscriptionContactPreferences(any())(any()))
          .thenReturn(Future.successful(Left(ErrorResponse(BAD_REQUEST, "There was an issue"))))

        val request = FakeRequest(GET, controllers.returns.submit.routes.ConfirmationController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
