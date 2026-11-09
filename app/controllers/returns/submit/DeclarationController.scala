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

import controllers.actions.returns.*
import controllers.actions.{ApprovedVapingManufacturerAuthAction, CheckInsolvencyAction}
import forms.returns.DeclarationFormProvider
import models.NormalMode
import models.identifiers.{PeriodKey, VpdId}
import models.obligations.ObligationDetails
import models.requests.returns.ReturnsDataRequest
import models.returns.{DeclarationDetails, ReturnsUserAnswers}
import navigation.ReturnsNavigator
import pages.returns.DeclarationPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.returns.{ReturnsUserAnswersService, StoredObligationsService, SubmitReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ReturnsDateUtils
import viewmodels.returns.submit.DeclarationViewModel
import views.html.returns.submit.DeclarationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       userAnswersService: ReturnsUserAnswersService,
                                       identify: ApprovedVapingManufacturerAuthAction,
                                       checkInsolvency: CheckInsolvencyAction,
                                       getData: ReturnsDataRetrievalAction,
                                       requireData: ReturnsDataRequiredAction,
                                       returnsEnabled: ReturnsEnabledAction,
                                       formProvider: DeclarationFormProvider,
                                       submitReturnService: SubmitReturnService,
                                       storedObligationsService: StoredObligationsService,
                                       returnsDateUtils: ReturnsDateUtils,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: DeclarationView,
                                       navigator: ReturnsNavigator,
                                     )(using ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[DeclarationDetails] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen checkInsolvency andThen returnsEnabled andThen getData andThen requireData).async {
    implicit request => {
      val vpdId = request.enrolmentVpdId
      val periodKey = request.periodKey
      val userAnswers = request.userAnswers

      val preparedForm = userAnswers.get(DeclarationPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      buildDeclarationViewModel(vpdId, periodKey, userAnswers)
        .map(vm => Ok(view(preparedForm, vm)))
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen returnsEnabled andThen getData andThen requireData).async {
    implicit request => {
      val vpdId = request.enrolmentVpdId
      val periodKey = request.periodKey
      val userAnswers = request.userAnswers

      form.bindFromRequest().fold(
        formWithErrors => {
          buildDeclarationViewModel(vpdId, periodKey, userAnswers)
            .map(vm => BadRequest(view(formWithErrors, vm)))
        },

        value => {
          Future.fromTry(userAnswers.set(DeclarationPage, value))
            .flatMap { updatedAnswers =>
              storedObligationsService.getObligations(vpdId, userAnswers).flatMap { obligations =>
                userAnswersService.set(updatedAnswers).flatMap { _ =>
                  submitAndContinue(updatedAnswers, obligations)
                }
              }
            }
        }
      )
    }
  }

  private def buildDeclarationViewModel(vpdId: VpdId,
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
