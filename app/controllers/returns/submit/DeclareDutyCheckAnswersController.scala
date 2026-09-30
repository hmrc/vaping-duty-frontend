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

import controllers.actions.{ApprovedVapingManufacturerAuthAction, CheckInsolvencyAction}
import controllers.actions.returns.*
import models.{CheckMode, Mode, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.{DutyRateService, ObligationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ReturnsDateUtils
import viewmodels.returns.submit.DeclareDutyCheckAnswersViewModel
import views.html.returns.submit.DeclareDutyCheckAnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclareDutyCheckAnswersController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   identify: ApprovedVapingManufacturerAuthAction,
                                                   checkInsolvency: CheckInsolvencyAction,
                                                   getData: ReturnsDataRetrievalAction,
                                                   requireData: ReturnsDataRequiredAction,
                                                   returnsEnabled: ReturnsEnabledAction,
                                                   obligationService: ObligationService,
                                                   dutyRateService: DutyRateService,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   view: DeclareDutyCheckAnswersView,
                                                   returnsDateUtils: ReturnsDateUtils
                                                 )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(mode: Mode = NormalMode): Action[AnyContent] = (identify andThen checkInsolvency andThen returnsEnabled andThen getData andThen requireData).async { implicit request =>
    val pk = request.periodKey

    for {
      obligationOpt <- obligationService.getObligationByPeriodKey(request.enrolmentVpdId, pk)
      obligation <- obligationOpt match {
        case Some(obligation) => Future.successful(obligation)
        case None             => Future.failed(Exception(s"Failed to find obligation for $pk"))
      }
      dutyRate = dutyRateService.getDutyRateForDate(obligation.iCFromDate)
    }
    yield {
      DeclareDutyCheckAnswersViewModel(request.userAnswers, dutyRate, pk, mode, returnsDateUtils.getPeriodDisplay(obligation)) match {
        case Some(vm) => Ok(view(pk, vm, mode))
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }
  }

  def onSubmit(mode: Mode = NormalMode): Action[AnyContent] = (identify andThen checkInsolvency andThen returnsEnabled andThen getData andThen requireData) { implicit request =>
    mode match {
      case CheckMode => Redirect(controllers.returns.submit.routes.CheckYourAnswersController.onPageLoad().url + s"?period=${request.periodKey.value}")
      case NormalMode => Redirect(controllers.returns.submit.routes.TaskListController.onPageLoad().url + s"?period=${request.periodKey.value}")
    }
  }
}