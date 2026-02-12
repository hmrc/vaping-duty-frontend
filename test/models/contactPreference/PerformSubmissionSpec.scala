package models.contactPreference

import base.SpecBase
import connectors.SubmitPreferencesConnector
import data.TestData
import models.contactPreference.PaperlessPreference.{Email, Post}
import models.emailverification.ErrorModel
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}

import scala.concurrent.Future

class PerformSubmissionSpec extends AnyFreeSpec with Matchers with TestData with SpecBase {

  implicit val request: DataRequest[?] = DataRequest(FakeRequest(), vpdId, userId, credId, emptyUserAnswers)

  val mockConnector: SubmitPreferencesConnector = mock[SubmitPreferencesConnector]

  "PerformSubmission must" - {

    "redirect to email confirmation page when submission is successful" in {

      when(mockConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Right(testSubmissionResponse)))

      val result = PerformSubmission(mockConnector, contactPreferenceSubmissionEmail, Email).getResult

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.contactPreference.routes.EmailConfirmationController.onPageLoad().url
   }

    "redirect to postal confirmation page when submission is successful" in {

      when(mockConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Right(testSubmissionResponse)))

      val result = PerformSubmission(mockConnector, contactPreferenceSubmissionPost, Post).getResult

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.contactPreference.routes.PostalConfirmationController.onPageLoad().url
    }

    "redirect to journey recovery when submission response is an error" in {

      when(mockConnector.submitContactPreferences(any(), any())(any()))
        .thenReturn(Future.successful(Left(ErrorModel(INTERNAL_SERVER_ERROR, "There was a problem"))))

      val result = PerformSubmission(mockConnector, contactPreferenceSubmissionNewEmail, Post).getResult

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
    }
  }
}
