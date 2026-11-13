package services.returns

import data.TestData
import models.returns.PeriodDisplay
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import utils.ReturnsDateUtils
import viewmodels.returns.submit.DeclarationViewModel

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class ReturnDeclarationPageServiceSpec extends AnyFreeSpec 
  with TestData with ScalaFutures with Matchers with MockitoSugar {

  "ReturnDeclarationPageService" - {
    "when building the view model" - {
      "should build the view model from the " in {
        val storedObligationsService = mock[StoredObligationsService]
        val submitReturnService = mock[SubmitReturnService]
        val returnsDateUtils = mock[ReturnsDateUtils]

        given HeaderCarrier = HeaderCarrier()
        given Messages = mock[Messages]
        given ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

        val pageService = ReturnDeclarationPageService(storedObligationsService, submitReturnService, returnsDateUtils)

        val userAnswers = returnsUserAnswers.copy(periodKey = june2026.value)
        val obligations = Seq(openObligation(june2026))

        when(storedObligationsService.savingObligations(vpdId, userAnswers))
          .thenReturn(Future.successful(obligations))
        when(returnsDateUtils.getPeriodDisplay(june2026, obligations))
          .thenReturn(PeriodDisplay("June", "2026"))

        whenReady(pageService.buildDeclarationViewModel(vpdId, june2026, userAnswers)) { viewModel =>
          viewModel mustBe DeclarationViewModel(june2026, PeriodDisplay("June", "2026")) 
        }
      }
    }
  }

}
