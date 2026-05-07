package controllers

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.ViewIndividualReturnView

class ViewIndividualReturnControllerSpec extends SpecBase {

  "ViewIndividualReturn Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(returnsUserAnswers = Some(returnsUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.ViewIndividualReturnController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ViewIndividualReturnView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }
  }
}
