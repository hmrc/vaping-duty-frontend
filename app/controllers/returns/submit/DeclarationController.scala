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
import forms.returns.DeclarationFormProvider
import models.NormalMode
import models.identifiers.VpdId
import models.obligations.ObligationDetails
import models.requests.returns.ReturnsDataRequest
import models.returns.{DeclarationDetails, ReturnsUserAnswers}
import navigation.ReturnsNavigator
import pages.returns.DeclarationPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import queries.returns.ObligationsQuery
import services.returns.{ObligationService, ReturnsUserAnswersService, SubmitReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ReturnsDateUtils
import viewmodels.returns.submit.DeclarationViewModel
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
                                       formProvider: DeclarationFormProvider,
                                       submitReturnService: SubmitReturnService,
                                       obligationService: ObligationService,
                                       returnsDateUtils: ReturnsDateUtils,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: DeclarationView,
                                       navigator: ReturnsNavigator,
                                       userAnswersService: ReturnsUserAnswersService
                                     )(using ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[DeclarationDetails] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen checkInsolvency andThen returnsEnabled andThen getData andThen requireData).async {
    implicit request => {
      val vpdId = request.enrolmentVpdId
      val periodKey = request.periodKey
      val userAnswers = request.userAnswers

      obligationService.getObligations(vpdId).flatMap { obligations =>
        saveObligations(userAnswers, obligations).map { _ =>

          val preparedForm = userAnswers.get(DeclarationPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          val periodDisplay = returnsDateUtils.getPeriodDisplay(periodKey, obligations)
          val viewModel = DeclarationViewModel(periodKey, periodDisplay)

          Ok(view(preparedForm, viewModel))
        }
      }
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen returnsEnabled andThen getData andThen requireData).async {
    implicit request => {
      val vpdId = request.enrolmentVpdId
      val periodKey = request.periodKey
      val userAnswers = request.userAnswers

      getObligations(vpdId, userAnswers).flatMap { obligations =>
        form.bindFromRequest().fold(
          formWithErrors => {
            val periodDisplay = returnsDateUtils.getPeriodDisplay(periodKey, obligations)
            val viewModel = DeclarationViewModel(periodKey, periodDisplay)

            Future.successful(BadRequest(view(formWithErrors, viewModel)))
          },

          value => {
            Future.fromTry(userAnswers.set(DeclarationPage, value))
              .flatMap { updatedAnswers =>
                sessionRepository.set(updatedAnswers).flatMap { _ =>
                  submitAndContinue(updatedAnswers, obligations)
                }
              }
          }
        )
      }
    }
  }

  // Uses the obligations saved by onPageLoad so the submit doesn't wait on the Obligations API.
  // Falls back to the live call if nothing has been saved.
  private def getObligations(vpdId: VpdId, userAnswers: ReturnsUserAnswers)
                            (using HeaderCarrier): Future[Seq[ObligationDetails]] =
    userAnswers.get(ObligationsQuery) match {
      case Some(obligations) => Future.successful(obligations)
      case None              => obligationService.getObligations(vpdId)
    }

  // Saves the obligations in the user answers so onSubmit does not have to wait on the Obligations API.
  // An error response is logged and the page still renders; any other failure is left to the framework's error handler.
  private def saveObligations(userAnswers: ReturnsUserAnswers, obligations: Seq[ObligationDetails])
                             (using HeaderCarrier): Future[Unit] =
    Future.fromTry(userAnswers.set(ObligationsQuery, obligations))
      .flatMap(sessionRepository.set)
      .map {
        case Right(_) => ()
        case Left(error) => logger.warn(s"Failed to save obligations in user answers: ${error.message}")
      }

  private def submitAndContinue(updatedAnswers: ReturnsUserAnswers, obligations: Seq[ObligationDetails])
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
