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
import models.{NormalMode, TaskStatus}
import models.obligations.Obligation
import models.requests.returns.ReturnsDataRequest
import models.returns.{AdjustmentsEligibility, ReturnsUserAnswers}
import navigation.ReturnsNavigator
import pages.returns.DeclarationPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.returns.ObligationsQuery
import services.returns.{ObligationService, ReturnsUserAnswersService, SubmitReturnService, TaskStatusService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ReturnsDateUtils
import views.html.returns.submit.DeclarationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       sessionRepository: ReturnsUserAnswersService,
                                       identify: ApprovedVapingManufacturerAuthAction,
                                       checkInsolvency: CheckInsolvencyAction,
                                       getData: ReturnsDataRetrievalAction,
                                       requireData: ReturnsDataRequiredAction,
                                       returnsEnabled: ReturnsEnabledAction,
                                       submitReturnService: SubmitReturnService,
                                       obligationService: ObligationService,
                                       returnsDateUtils: ReturnsDateUtils,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: DeclarationView,
                                       navigator: ReturnsNavigator,
                                       userAnswersService: ReturnsUserAnswersService
                                     )(using ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen checkInsolvency andThen returnsEnabled andThen getData andThen requireData).async {
    implicit request =>
      obligationService.getObligations(request.enrolmentVpdId).flatMap { obligations =>
        if (!readyToSubmit(request.userAnswers, obligations)) {
          Future.successful(Redirect(s"${controllers.returns.submit.routes.ReturnSubmissionRecoveryController.onPageLoad().url}?period=${request.periodKey.value}"))
        } else {
          val periodDisplay = returnsDateUtils.getPeriodDisplay(request.periodKey, obligations)

          saveObligations(request.userAnswers, obligations).map { _ =>
            Ok(view(request.periodKey, periodDisplay.month, periodDisplay.year))
          }
        }
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen returnsEnabled andThen getData andThen requireData).async {
    implicit request =>
      getObligations.flatMap { obligations =>
        submitAndContinue(request.userAnswers, obligations)
      }
  }

  private def readyToSubmit(userAnswers: ReturnsUserAnswers, obligations: Seq[Obligation]): Boolean =
    TaskStatusService.allTasksCompleted(userAnswers, AdjustmentsEligibility.fromObligations(obligations)) &&
      TaskStatusService.yourDetailsSubTaskStatus(userAnswers) == TaskStatus.Completed

 
  private def getObligations(using request: ReturnsDataRequest[?]): Future[Seq[Obligation]] =
    obligationService.getObligations(request.userAnswers, request.enrolmentVpdId)

  private def saveObligations(userAnswers: ReturnsUserAnswers, obligations: Seq[Obligation])
                             (using request: ReturnsDataRequest[?]): Future[Unit] =
    Future.fromTry(userAnswers.set(ObligationsQuery, obligations))
      .flatMap(sessionRepository.set)
      .map {
        case Right(_) => ()
        case Left(error) => logger.warn(s"Failed to save obligations in user answers: ${error.message}")
      }

  private def submitAndContinue(updatedAnswers: ReturnsUserAnswers, obligations: Seq[Obligation])
                               (using request: ReturnsDataRequest[?]): Future[Result] = {
                               
    submitReturnService.submit(updatedAnswers, obligations)
      .flatMap(_ => userAnswersService.clear(request.enrolmentVpdId, request.periodKey))
      .map(_ => Redirect(navigator.nextPage(DeclarationPage, NormalMode, updatedAnswers)))
      .recover { case e =>
        logger.error("Failed to submit return", e)  
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }
}
