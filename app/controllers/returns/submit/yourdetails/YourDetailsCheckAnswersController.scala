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

package controllers.returns.submit.yourdetails

import controllers.actions.{ApprovedVapingManufacturerAuthAction, CheckInsolvencyAction}
import controllers.actions.returns.*
import models.TaskStatus
import models.requests.returns.ReturnsDataRequest
import models.returns.PeriodDisplay
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.{ObligationService, TaskStatusService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ReturnsDateUtils
import viewmodels.returns.submit.YourDetailsCheckAnswersViewModel
import views.html.returns.submit.yourdetails.YourDetailsCheckAnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourDetailsCheckAnswersController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   identify: ApprovedVapingManufacturerAuthAction,
                                                   checkInsolvency: CheckInsolvencyAction,
                                                   returnsEnabled: ReturnsEnabledAction,
                                                   getData: ReturnsDataRetrievalAction,
                                                   requireData: ReturnsDataRequiredAction,
                                                   obligationService: ObligationService,
                                                   returnsDateUtils: ReturnsDateUtils,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   view: YourDetailsCheckAnswersView
                                                 )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen checkInsolvency andThen returnsEnabled andThen getData andThen requireData).async {
    implicit request =>
      val pk = request.periodKey

      if (TaskStatusService.yourDetailsTaskStatus(request.userAnswers) != TaskStatus.Completed) {
        Future.successful(Redirect(controllers.returns.submit.routes.ReturnSubmissionRecoveryController.onPageLoad().url + s"?period=${pk.value}"))
      } else {
        YourDetailsCheckAnswersViewModel(request.userAnswers, pk) match {
          case Some(summaryList) =>
            periodDisplay.map(period => Ok(view(pk, summaryList, period.month, period.year)))
          case None =>
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen checkInsolvency andThen returnsEnabled andThen getData andThen requireData) {
    implicit request =>
      Redirect(controllers.returns.submit.routes.DeclarationController.onPageLoad().url + s"?period=${request.periodKey.value}")
  }

  private def periodDisplay(using request: ReturnsDataRequest[?]): Future[PeriodDisplay] =
    obligationService.getObligations(request.userAnswers, request.enrolmentVpdId)
      .map(obligations => returnsDateUtils.getPeriodDisplay(request.periodKey, obligations))
}
