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

package controllers.returns.submit.adjustments

import base.SpecBase
import models.returns.ReturnsUserAnswers
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.returns.adjustments.{AdjustmentListPage, DeclareAdjustmentPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.returns.ReturnsUserAnswersService

import scala.concurrent.Future

class RemoveAdjustmentControllerSpec extends SpecBase with MockitoSugar {

  val adjustmentPeriodKey = october2027
  val otherAdjustmentPeriodKey = november2027

  lazy val removeAdjustmentRoute: String =
    controllers.returns.submit.adjustments.routes.RemoveAdjustmentController.onPageLoad().url + s"?adjustmentPeriod=${adjustmentPeriodKey.value}"

  lazy val removeAdjustmentSubmitRoute: String =
    controllers.returns.submit.adjustments.routes.RemoveAdjustmentController.onSubmit().url + s"?adjustmentPeriod=${adjustmentPeriodKey.value}"

  "RemoveAdjustment Controller" - {

    "must return OK and the correct view for a GET" in {
      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(AdjustmentEntry(adjustmentPeriodKey, AdjustmentType.UnderDeclared, BigDecimal(1000)))))
        .success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, removeAdjustmentRoute)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "must redirect to Journey Recovery for a GET when no adjustmentPeriod is supplied" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.returns.submit.adjustments.routes.RemoveAdjustmentController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the summary page when confirmed removal leaves other entries" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(
          AdjustmentEntry(adjustmentPeriodKey, AdjustmentType.UnderDeclared, BigDecimal(1000)),
          AdjustmentEntry(otherAdjustmentPeriodKey, AdjustmentType.OverDeclared, BigDecimal(500))
        )))
        .success.value

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(bind[ReturnsUserAnswersService].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeAdjustmentSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(
          controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad().url
        )
        verify(mockSessionRepository).set(any())(any())
      }
    }

    "must redirect to the summary page and clear the declaration when confirmed removal leaves no entries" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]
      when(mockSessionRepository.set(any())(any())) thenReturn Future.successful(Right(true))

      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(AdjustmentEntry(adjustmentPeriodKey, AdjustmentType.UnderDeclared, BigDecimal(1000)))))
        .success.value

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(bind[ReturnsUserAnswersService].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeAdjustmentSubmitRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(
          controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad().url
        )

        val captor = ArgumentCaptor.forClass(classOf[ReturnsUserAnswers])
        verify(mockSessionRepository).set(captor.capture())(any())
        captor.getValue.get(DeclareAdjustmentPage) mustBe Some(false)
        captor.getValue.get(AdjustmentListPage) mustBe None
      }
    }

    "must redirect to the summary page without removing anything when the user cancels" in {
      val mockSessionRepository = mock[ReturnsUserAnswersService]

      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(AdjustmentEntry(adjustmentPeriodKey, AdjustmentType.UnderDeclared, BigDecimal(1000)))))
        .success.value

      val application =
        applicationBuilder(returnsUserAnswers = Some(userAnswers))
          .overrides(bind[ReturnsUserAnswersService].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, removeAdjustmentSubmitRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value must include(
          controllers.returns.submit.adjustments.routes.AdjustmentCheckYourAnswersController.onPageLoad().url
        )
        verify(mockSessionRepository, never).set(any())(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val userAnswers = returnsUserAnswers
        .set(AdjustmentListPage, AdjustmentList(Seq(AdjustmentEntry(adjustmentPeriodKey, AdjustmentType.UnderDeclared, BigDecimal(1000)))))
        .success.value

      val application = applicationBuilder(returnsUserAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, removeAdjustmentSubmitRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must redirect to Journey Recovery for a POST when no adjustmentPeriod is supplied" in {
      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.returns.submit.adjustments.routes.RemoveAdjustmentController.onSubmit().url)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(returnsUserAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, removeAdjustmentRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}