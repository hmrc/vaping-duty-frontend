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
import forms.returns.DeclareDutyFormProvider
import models.requests.returns.ReturnsDataRequest
import models.{Mode, NormalMode}
import navigation.ReturnsNavigator
import pages.returns.adjustments.{AddAnotherAdjustmentPage, AdjustmentListPage, DeclareAdjustmentPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.returns.{AdjustmentCheckYourAnswersService, ReturnsUserAnswersService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.returns.submit.adjustments.AdjustmentCheckYourAnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AdjustmentCheckYourAnswersController @Inject()(
                                                      override val messagesApi: MessagesApi,
                                                      sessionRepository: ReturnsUserAnswersService,
                                                      navigator: ReturnsNavigator,
                                                      identify: ApprovedVapingManufacturerAuthAction,
                                                      getData: ReturnsDataRetrievalAction,
                                                      requireData: ReturnsDataRequiredAction,
                                                      formProvider: DeclareDutyFormProvider,
                                                      returnsEnabledAction: ReturnsEnabledAction,
                                                      adjustmentCheckYourAnswersService: AdjustmentCheckYourAnswersService,
                                                      val controllerComponents: MessagesControllerComponents,
                                                      view: AdjustmentCheckYourAnswersView
                                                    )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode = NormalMode): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>
      buildViewModel(request, mode).map { vm =>
        val preparedForm = request.userAnswers.get(AddAnotherAdjustmentPage) match {
          case None => form
          case Some(value) => form.fill(value)
        }

        Ok(view(request.periodKey, vm, preparedForm, mode))
      }
  }

  def onSubmit(mode: Mode = NormalMode): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>
      val declareAdjustment = request.userAnswers.get(DeclareAdjustmentPage)

      buildViewModel(request, mode).flatMap { vm =>
        declareAdjustment match {
          case Some(false) =>
            redirectToNextPageWithoutAddingAnother(request, mode)

          case _ if !vm.hasAvailablePeriodsToAdd =>
            redirectToNextPageWithoutAddingAnother(request, mode)

          case _ =>
            form.bindFromRequest().fold(
              formWithErrors =>
                Future.successful(BadRequest(view(request.periodKey, vm, formWithErrors, mode))),

              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(AddAnotherAdjustmentPage, value))
                  _ <- sessionRepository.set(updatedAnswers)
                  currentReasonMandatory <- adjustmentCheckYourAnswersService.isReasonMandatory(
                    updatedAnswers,
                    request.enrolmentVpdId
                  )
                } yield Redirect(navigator.nextPage(
                  AddAnotherAdjustmentPage,
                  mode,
                  updatedAnswers,
                  currentReasonMandatory
                ))
            )
        }
      }
  }

  private def buildViewModel(request: ReturnsDataRequest[AnyContent], mode: Mode)
                            (using HeaderCarrier, Messages): Future[viewmodels.returns.submit.adjustments.AdjustmentCheckYourAnswersViewModel] = {
    val declareAdjustment = request.userAnswers.get(DeclareAdjustmentPage)
    val adjustmentList = request.userAnswers.get(AdjustmentListPage)

    adjustmentCheckYourAnswersService.buildViewModel(
      declareAdjustment,
      adjustmentList,
      request.periodKey,
      request.enrolmentVpdId,
      mode
    )
  }

  private def redirectToNextPageWithoutAddingAnother(
                                                      request: ReturnsDataRequest[AnyContent],
                                                      mode: Mode
                                                    )(using HeaderCarrier): Future[Result] = {
    for {
      currentReasonMandatory <- adjustmentCheckYourAnswersService.isReasonMandatory(
        request.userAnswers,
        request.enrolmentVpdId
      )
      result <- {
        if (adjustmentCheckYourAnswersService.shouldRedirectToReasonPage(request.userAnswers, currentReasonMandatory)) {
          Future.successful(Redirect(
            s"${controllers.returns.submit.routes.AdjustmentReasonController.onPageLoad(mode).url}?period=${request.periodKey.value}"
          ))
        } else {
          val cleanedAnswersTry = adjustmentCheckYourAnswersService.cleanupReasonIfNotRequired(
            request.userAnswers,
            currentReasonMandatory
          )
          for {
            cleanedAnswers <- Future.fromTry(cleanedAnswersTry)
            updatedAnswers <- Future.fromTry(cleanedAnswers.set(AddAnotherAdjustmentPage, false))
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(AddAnotherAdjustmentPage, mode, updatedAnswers, currentReasonMandatory))
        }
      }
    } yield result
  }

}
