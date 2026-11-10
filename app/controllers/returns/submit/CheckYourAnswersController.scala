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
import controllers.actions.{ApprovedVapingManufacturerAuthAction, CheckInsolvencyAction}
import controllers.actions.returns.*
import models.NormalMode
import models.obligations.ObligationDetails
import models.requests.returns.ReturnsDataRequest
import models.returns.{AdjustmentsEligibility, DeclarationDetails, ReturnsUserAnswers}
import navigation.ReturnsNavigator
import pages.returns.adjustments.AdjustmentListPage
import pages.returns.yourdetails.{YourBusinessEmailPage, YourFullNamePage, YourRolePage}
import pages.returns.{DeclarationPage, SpoiltVolumeByPeriodPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.returns.ObligationsQuery
import services.returns.{DutyRateService, ObligationService, ReturnsUserAnswersService, SubmitReturnService, TaskStatusService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ReturnsDateUtils
import viewmodels.returns.submit.CheckYourAnswersViewModel
import views.html.returns.submit.CheckYourAnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: ApprovedVapingManufacturerAuthAction,
                                            checkInsolvency: CheckInsolvencyAction,
                                            getData: ReturnsDataRetrievalAction,
                                            requireData: ReturnsDataRequiredAction,
                                            returnsEnabled: ReturnsEnabledAction,
                                            dutyRateService: DutyRateService,
                                            obligationService: ObligationService,
                                            submitReturnService: SubmitReturnService,
                                            userAnswersService: ReturnsUserAnswersService,
                                            navigator: ReturnsNavigator,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            returnsDateUtils: ReturnsDateUtils,
                                            appConfig: FrontendAppConfig
                                          )(using ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen checkInsolvency andThen returnsEnabled andThen getData andThen requireData).async { implicit request =>
    val pk = request.periodKey

    val adjustmentPeriods = request.userAnswers.get(AdjustmentListPage)
      .map(_.adjustments.map(_.period))
      .getOrElse(Seq.empty)

    val spoiltPeriods = request.userAnswers.get(SpoiltVolumeByPeriodPage)
      .map(_.map(_.periodKey))
      .getOrElse(List.empty)

    val allPeriods = (adjustmentPeriods ++ spoiltPeriods :+ pk).distinct

    for {
      obligationDetails <- obligationService.getObligations(request.enrolmentVpdId)
      dutyRates = dutyRateService.getDutyRatesForPeriods(allPeriods, obligationDetails)
      returnPeriod = returnsDateUtils.getPeriodDisplay(pk, obligationDetails)
    } yield {
      val adjustmentsEligibility = AdjustmentsEligibility.fromObligationDetails(obligationDetails)
      if (TaskStatusService.allTasksCompleted(request.userAnswers, adjustmentsEligibility, appConfig.yourDetailsEnabled)) {
        Ok(view(pk, CheckYourAnswersViewModel(request.userAnswers, dutyRates, pk, adjustmentsEligibility, returnPeriod), appConfig.yourDetailsEnabled))
      } else {
        Redirect(controllers.returns.submit.routes.ReturnSubmissionRecoveryController.onPageLoad().url + s"?period=${pk.value}")
      }
    }
  }

  // With your details enabled the return is submitted from here; otherwise the declaration page submits it
  def onSubmit: Action[AnyContent] = (identify andThen checkInsolvency andThen returnsEnabled andThen getData andThen requireData).async { implicit request =>
    if (appConfig.yourDetailsEnabled) {
      submitReturn()
    } else {
      Future.successful(Redirect(s"${controllers.returns.submit.routes.DeclarationController.onPageLoad().url}?period=${request.periodKey}"))
    }
  }

  private def submitReturn()(using request: ReturnsDataRequest[AnyContent]): Future[Result] =
    getObligations.flatMap { obligations =>
      val adjustmentsEligibility = AdjustmentsEligibility.fromObligationDetails(obligations)

      (TaskStatusService.allTasksCompleted(request.userAnswers, adjustmentsEligibility, yourDetailsEnabled = true), declarationDetails(request.userAnswers)) match {
        case (true, Some(declaration)) =>
          Future.fromTry(request.userAnswers.set(DeclarationPage, declaration))
            .flatMap(updatedAnswers => submitAndContinue(updatedAnswers, obligations))
        case _ =>
          Future.successful(Redirect(controllers.returns.submit.routes.ReturnSubmissionRecoveryController.onPageLoad().url + s"?period=${request.periodKey.value}"))
      }
    }

  private def declarationDetails(userAnswers: ReturnsUserAnswers): Option[DeclarationDetails] =
    for {
      fullName <- userAnswers.get(YourFullNamePage)
      role     <- userAnswers.get(YourRolePage)
      email    <- userAnswers.get(YourBusinessEmailPage)
    } yield DeclarationDetails(fullName, role, email)

  // Uses the obligations saved in the user answers when present, falling back to the Obligations API
  private def getObligations(using request: ReturnsDataRequest[?]): Future[Seq[ObligationDetails]] =
    request.userAnswers.get(ObligationsQuery) match {
      case Some(obligations) => Future.successful(obligations)
      case None              => obligationService.getObligations(request.enrolmentVpdId)
    }

  private def submitAndContinue(updatedAnswers: ReturnsUserAnswers, obligations: Seq[ObligationDetails])
                               (using request: ReturnsDataRequest[?]): Future[Result] =
    submitReturnService.submit(updatedAnswers, obligations)
      .flatMap(_ => userAnswersService.clear(request.enrolmentVpdId, request.periodKey))
      .map(_ => Redirect(navigator.nextPage(DeclarationPage, NormalMode, updatedAnswers)))
      .recover { case e =>
        logger.error("Failed to submit return", e)
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
}
