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

import config.FrontendAppConfig
import connectors.returns.GetReturnsConnector
import controllers.actions.{ApprovedVapingManufacturerAuthAction, CheckInsolvencyAction}
import controllers.actions.returns.*
import models.BtaLink
import models.identifiers.PeriodKey
import models.obligations.ObligationDetails
import models.requests.IdentifierRequest
import models.returns.view.ReturnDisplayResponse
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import services.payments.FinancialDataService
import services.returns.ObligationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ReturnsDateUtils
import viewmodels.returns.submit.ConfirmationViewModel
import views.html.returns.submit.ConfirmationEmailView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmationController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        identify: ApprovedVapingManufacturerAuthAction,
                                        checkInsolvency: CheckInsolvencyAction,
                                        returnsEnabled: ReturnsEnabledAction,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ConfirmationEmailView,
                                        getReturnsConnector: GetReturnsConnector,
                                        obligationService: ObligationService,
                                        financialDataService: FinancialDataService,
                                        config: FrontendAppConfig,
                                        returnsDateUtils: ReturnsDateUtils
                                      )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen checkInsolvency andThen returnsEnabled).async { implicit request =>
    buildConfirmationPage(getPeriodKey())
  }

  private def buildConfirmationPage(periodKey: PeriodKey)
                                   (using request: IdentifierRequest[?]): Future[Result] =
    for {
      returnsResponse <- getReturnsConnector.getReturn(periodKey, request.enrolmentVpdId)
      paymentDueDate  <- getPaymentDetail(request, returnsResponse)
      obligation      <- getObligation(periodKey)
    } yield {
      val viewModel = ConfirmationViewModel(returnsResponse, obligation, BtaLink(config), paymentDueDate, returnsDateUtils)
      Ok(view(viewModel))
    }

  private def getPaymentDetail(request: IdentifierRequest[?], returnsResponse: ReturnDisplayResponse)
                              (using HeaderCarrier): Future[Option[LocalDate]] = {
    val chargeRef = returnsResponse.success.chargeDetails.flatMap(_.chargeReference).getOrElse("")

    financialDataService.getPaymentDueDate(request.enrolmentVpdId, chargeRef)
  }

  private def getObligation(periodKey: PeriodKey)
                           (using request: IdentifierRequest[?]): Future[ObligationDetails] =
    obligationService.getObligationByPeriodKey(request.enrolmentVpdId, periodKey).map {
      case Some(obligation) => obligation
      case None             =>
        // scalafix:off DisableSyntax.throw
        throw new RuntimeException("Obligation not found")
    }

  private def getPeriodKey()(using request: Request[?]): PeriodKey =
    request.getQueryString("period").fold(PeriodKey("99XX"))(PeriodKey(_))
}