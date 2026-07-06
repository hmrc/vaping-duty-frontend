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
import controllers.routes
import forms.returns.EnterDutyAmountFormProvider
import models.NormalMode
import models.returns.ReturnsUserAnswers
import navigation.{ReturnsFakeNavigator, ReturnsNavigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.EnterDutyAmountPage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.ReturnsUserAnswersService
import views.html.returns.submit.EnterDutyAmountView

import scala.concurrent.Future

class EnterDutyAmountControllerSpec extends SpecBase with MockitoSugar {

  private val mockFormProvider = mock[EnterDutyAmountFormProvider]
  private val testForm: Form[BigDecimal] = Form(
    "value" -> play.api.data.Forms.bigDecimal
  )

  def onwardRoute = Call("GET", "/foo")

  val validAnswer = BigDecimal("1000")

  lazy val enterDutyAmountRoute: String = controllers.returns.submit.routes.EnterDutyAmountController.onPageLoad(NormalMode).url

  "EnterDutyAmount Controller" - {

    "must return OK and the correct view for a GET" in {
      when(mockFormProvider.apply(any(), any())(any(), any()))
        .thenReturn(Future.successful(testForm))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[EnterDutyAmountFormProvider].toInstance(mockFormProvider))
        .build()

      running(application) {
        val request = FakeRequest(GET, enterDutyAmountRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EnterDutyAmountView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, testForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      when(mockFormProvider.apply(any(), any())(any(), any()))
        .thenReturn(Future.successful(testForm))

      val userAnswers = returnsUserAnswers.set(EnterDutyAmountPage, validAnswer).success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers))
        .overrides(bind[EnterDutyAmountFormProvider].toInstance(mockFormProvider))
        .build()

      running(application) {
        val request = FakeRequest(GET, enterDutyAmountRoute)

        val view = application.injector.instanceOf[EnterDutyAmountView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(periodKey, testForm.fill(validAnswer), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockFormProvider.apply(any(), any())(any(), any()))
        .thenReturn(Future.successful(testForm))

      val mockSessionRepository = mock[ReturnsUserAnswersService]

      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val application =
        applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
          .overrides(
            bind[ReturnsNavigator].toInstance(new ReturnsFakeNavigator(onwardRoute, mockAppConfig)),
            bind[ReturnsUserAnswersService].toInstance(mockSessionRepository),
            bind[EnterDutyAmountFormProvider].toInstance(mockFormProvider)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, enterDutyAmountRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      when(mockFormProvider.apply(any(), any())(any(), any()))
        .thenReturn(Future.successful(testForm))

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers))
        .overrides(bind[EnterDutyAmountFormProvider].toInstance(mockFormProvider))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, enterDutyAmountRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = testForm.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[EnterDutyAmountView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(periodKey, boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(returnsUserAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, enterDutyAmountRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(returnsUserAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, enterDutyAmountRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
