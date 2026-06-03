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
import forms.returns.DeclareSpoiltProductsFormProvider
import models.NormalMode
import models.returns.ReturnsUserAnswers
import navigation.{ReturnsFakeNavigator, ReturnsNavigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.DeclareSpoiltProductsPage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.ReturnsUserAnswersService
import views.html.returns.submit.DeclareSpoiltProductsView

import scala.concurrent.Future

class DeclareSpoiltProductsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new DeclareSpoiltProductsFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val declareSpoiltProductsRoute: String = controllers.returns.submit.routes.DeclareSpoiltProductsController.onPageLoad(NormalMode).url

  "DeclareSpoiltProducts Controller" - {

    "must return OK and the correct view for a GET" in {
      when(mockAppConfig.claimDutyBackGuidance)
        .thenReturn("https://www.gov.uk/government/publications/excise-notice-207-excise-duty-drawback/excise-notice-207-excise-duty-drawback")

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, declareSpoiltProductsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeclareSpoiltProductsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, form, NormalMode, mockAppConfig.claimDutyBackGuidance)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      when(mockAppConfig.claimDutyBackGuidance)
        .thenReturn("https://www.gov.uk/government/publications/excise-notice-207-excise-duty-drawback/excise-notice-207-excise-duty-drawback")
      
      val userAnswers = returnsUserAnswers.set(DeclareSpoiltProductsPage, true).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, declareSpoiltProductsRoute)

        val view = application.injector.instanceOf[DeclareSpoiltProductsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, form.fill(true), NormalMode, mockAppConfig.claimDutyBackGuidance)(request, messages(application)).toString
      }
    }

    "must redirect to the next page and clear entered spoilt products amount when false" in {

      val mockSessionRepository = mock[ReturnsUserAnswersService]

      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, declareSpoiltProductsRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      when(mockAppConfig.claimDutyBackGuidance)
        .thenReturn("https://www.gov.uk/government/publications/excise-notice-207-excise-duty-drawback/excise-notice-207-excise-duty-drawback")

      val mockSessionRepository = mock[ReturnsUserAnswersService]

      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, declareSpoiltProductsRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockAppConfig.claimDutyBackGuidance)
        .thenReturn("https://www.gov.uk/government/publications/excise-notice-207-excise-duty-drawback/excise-notice-207-excise-duty-drawback")

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, declareSpoiltProductsRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeclareSpoiltProductsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(periodKey, boundForm, NormalMode, mockAppConfig.claimDutyBackGuidance)(request, messages(application)).toString
      }
    }
  }
}
