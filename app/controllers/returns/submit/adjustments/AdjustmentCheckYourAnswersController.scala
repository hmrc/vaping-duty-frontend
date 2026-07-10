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
import models.NormalMode
import models.requests.returns.ReturnsDataRequest
import navigation.ReturnsNavigator
import pages.returns.adjustments.{AddAnotherAdjustmentPage, AdjustmentListPage, DeclareAdjustmentPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
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

  def onPageLoad(): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>

      val declareAdjustment = request.userAnswers.get(DeclareAdjustmentPage)
      val adjustmentList = request.userAnswers.get(AdjustmentListPage)

      adjustmentCheckYourAnswersService
        .buildViewModel(declareAdjustment, adjustmentList, request.periodKey, request.enrolmentVpdId)
        .map { vm =>
          val preparedForm = request.userAnswers.get(AddAnotherAdjustmentPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(request.periodKey, vm, preparedForm))
        }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>

      val declareAdjustment = request.userAnswers.get(DeclareAdjustmentPage)
      val adjustmentList = request.userAnswers.get(AdjustmentListPage)

      adjustmentCheckYourAnswersService
        .buildViewModel(declareAdjustment, adjustmentList, request.periodKey, request.enrolmentVpdId)
        .flatMap { vm =>
          declareAdjustment match {
            case Some(false) => redirectToNextPageWithoutAddingAnother(request, vm.underDeclaredDutyTotal, vm.overDeclaredDutyTotal)
            case _ if !vm.hasAvailablePeriodsToAdd => redirectToNextPageWithoutAddingAnother(request, vm.underDeclaredDutyTotal, vm.overDeclaredDutyTotal)
            case _ =>
              form.bindFromRequest().fold(
                formWithErrors =>
                  Future.successful(BadRequest(view(request.periodKey, vm, formWithErrors))),

                value =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(AddAnotherAdjustmentPage, value))
                    _ <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(navigator.nextPage(
                    AddAnotherAdjustmentPage,
                    NormalMode,
                    updatedAnswers,
                    vm.underDeclaredDutyTotal,
                    vm.overDeclaredDutyTotal
                  ))
              )
          }
        }
  }

  private def redirectToNextPageWithoutAddingAnother(
                                                      request: ReturnsDataRequest[AnyContent],
                                                      underDeclaredDutyTotal: BigDecimal,
                                                      overDeclaredDutyTotal: BigDecimal
                                                    )(using HeaderCarrier): Future[Result] = {
    for {
      updatedAnswers <- Future.fromTry(request.userAnswers.set(AddAnotherAdjustmentPage, false))
      _ <- sessionRepository.set(updatedAnswers)
    } yield Redirect(navigator.nextPage(AddAnotherAdjustmentPage, NormalMode, updatedAnswers, underDeclaredDutyTotal, overDeclaredDutyTotal))
  }

}
