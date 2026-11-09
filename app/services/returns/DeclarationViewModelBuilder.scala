package services.returns

import com.google.inject.Inject
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.ObligationDetails
import models.returns.ReturnsUserAnswers
import play.api.i18n.Messages
import uk.gov.hmrc.http.HeaderCarrier
import utils.ReturnsDateUtils
import viewmodels.returns.submit.DeclarationViewModel

class DeclarationViewModelBuilder @Inject() (
                                              storedObligationsService: StoredObligationsService,
                                              returnsDateUtils: ReturnsDateUtils,
                                            ){

  def buildDeclarationViewModel(vpdId: VpdId,
                                periodKey: PeriodKey,
                                userAnswers: ReturnsUserAnswers)
                               (using HeaderCarrier, Messages) = {
    storedObligationsService.savingObligations(vpdId, userAnswers) { obligations =>
      buildViewModel(periodKey, obligations)
    }
  }

  private def buildViewModel(periodKey: PeriodKey,
                             obligations: Seq[ObligationDetails])
                            (using Messages) = {
    val periodDisplay = returnsDateUtils.getPeriodDisplay(periodKey, obligations)
    DeclarationViewModel(periodKey, periodDisplay)
  }

}
