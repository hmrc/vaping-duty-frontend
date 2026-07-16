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

import controllers.actions.ApprovedVapingManufacturerAuthAction
import controllers.actions.returns.{ReturnsDataRequiredAction, ReturnsDataRetrievalAction, ReturnsEnabledAction}
import models.Mode
import pages.returns.adjustments.AdjustmentListPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.ObligationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ReturnsDateUtils
import viewmodels.returns.submit.adjustments.SelectAdjustmentPeriodViewModel
import views.html.returns.submit.adjustments.SelectAdjustmentPeriodView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SelectAdjustmentPeriodController @Inject()(
                                                  override val messagesApi: MessagesApi,
                                                  identify: ApprovedVapingManufacturerAuthAction,
                                                  returnsEnabledAction: ReturnsEnabledAction,
                                                  getData: ReturnsDataRetrievalAction,
                                                  requireData: ReturnsDataRequiredAction,
                                                  obligationService: ObligationService,
                                                  returnsDateUtils: ReturnsDateUtils,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  view: SelectAdjustmentPeriodView
                                                )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(mode: Mode, year: Option[Int]): Action[AnyContent] =
    (identify andThen returnsEnabledAction andThen getData andThen requireData).async { implicit request =>

      obligationService.getObligationsDirectly(request.enrolmentVpdId).map { obligationDetails =>
        val adjustmentList = request.userAnswers.get(AdjustmentListPage)
        val vm = SelectAdjustmentPeriodViewModel(
          obligationDetails,
          year,
          request.periodKey,
          adjustmentList,
          returnsDateUtils,
          mode
        )
        Ok(view(vm, request.periodKey))
      }.recover {
        case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }
}
