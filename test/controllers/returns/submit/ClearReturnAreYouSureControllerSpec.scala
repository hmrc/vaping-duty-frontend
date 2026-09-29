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
import forms.returns.ClearReturnAreYouSureFormProvider
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.ReturnsUserAnswersService
import uk.gov.hmrc.http.UpstreamErrorResponse
import views.html.returns.submit.ClearReturnAreYouSureView

import scala.concurrent.Future

class ClearReturnAreYouSureControllerSpec extends SpecBase with MockitoSugar {

  val formProvider = new ClearReturnAreYouSureFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val clearReturnAreYouSureRoute: String = routes.ClearReturnAreYouSureController.onPageLoad().url + s"?period=${periodKey.value}"

  "ClearReturnAreYouSure Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, clearReturnAreYouSureRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ClearReturnAreYouSureView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, periodKey)(request, messages(application)).toString
      }
    }

    "must redirect to TaskList when user selects No" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, clearReturnAreYouSureRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(routes.TaskListController.onPageLoad().url)
        redirectLocation(result).value must include(s"period=${periodKey.value}")
      }
    }

    "must clear user answers and redirect to BeforeYouStart when user selects Yes" in {

      val mockUserAnswersService = mock[ReturnsUserAnswersService]

      when(mockUserAnswersService.clear(eqTo(vpdId), eqTo(periodKey))(any()))
        .thenReturn(Future.successful(Right(())))

      val application =
        applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ReturnsUserAnswersService].toInstance(mockUserAnswersService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, clearReturnAreYouSureRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(routes.BeforeYouStartController.onPageLoad().url)
        redirectLocation(result).value must include(s"period=${periodKey.value}")

        verify(mockUserAnswersService).clear(eqTo(vpdId), eqTo(periodKey))(any())
      }
    }

    "must redirect to JourneyRecovery when clear fails" in {

      val mockUserAnswersService = mock[ReturnsUserAnswersService]

      when(mockUserAnswersService.clear(eqTo(vpdId), eqTo(periodKey))(any()))
        .thenReturn(Future.successful(Left(UpstreamErrorResponse("Error", INTERNAL_SERVER_ERROR))))

      val application =
        applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ReturnsUserAnswersService].toInstance(mockUserAnswersService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, clearReturnAreYouSureRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockUserAnswersService).clear(eqTo(vpdId), eqTo(periodKey))(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, clearReturnAreYouSureRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ClearReturnAreYouSureView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, periodKey)(request, messages(application)).toString
      }
    }
  }
}
