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
import models.identifiers.PeriodKey
import models.obligations.ObligationsResponse
import models.requests.returns.ReturnsDataRequest
import models.returns.adjustments.AdjustmentList
import navigation.ReturnsNavigator
import pages.returns.adjustments.{AddAnotherAdjustmentPage, AdjustmentListPage, DeclareAdjustmentPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.returns.{DutyRateService, ObligationService, ReturnsUserAnswersService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.submit.adjustments.AdjustmentCheckYourAnswersViewModel
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
                                                      obligationService: ObligationService,
                                                      dutyRateService: DutyRateService,
                                                      val controllerComponents: MessagesControllerComponents,
                                                      view: AdjustmentCheckYourAnswersView
                                                    )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>

      val declareAdjustment = request.userAnswers.get(DeclareAdjustmentPage)
      val adjustmentList = request.userAnswers.get(AdjustmentListPage)

      withObligations { obligations =>
        getDutyRatesForAdjustments(adjustmentList).map { dutyRatesMap =>
          val vm = buildViewModel(declareAdjustment, adjustmentList, obligations, request.periodKey, dutyRatesMap)

          val preparedForm = request.userAnswers.get(AddAnotherAdjustmentPage) match {
            case None => form
            case Some(value) => form.fill(value)
          }

          Ok(view(request.periodKey, vm, preparedForm))
        }
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>

      val declareAdjustment = request.userAnswers.get(DeclareAdjustmentPage)
      val adjustmentList = request.userAnswers.get(AdjustmentListPage)

      declareAdjustment match {
        case Some(false) =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(AddAnotherAdjustmentPage, false))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(AddAnotherAdjustmentPage, NormalMode, updatedAnswers))

        case _ =>
          form.bindFromRequest().fold(
            formWithErrors =>
              withObligations { obligations =>
                getDutyRatesForAdjustments(adjustmentList).map { dutyRatesMap =>
                  val vm = buildViewModel(declareAdjustment, adjustmentList, obligations, request.periodKey, dutyRatesMap)
                  BadRequest(view(request.periodKey, vm, formWithErrors))
                }
              },

            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(AddAnotherAdjustmentPage, value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(AddAnotherAdjustmentPage, NormalMode, updatedAnswers))
          )
      }
  }

  private def buildViewModel(
    declareAdjustment: Option[Boolean],
    adjustmentList: Option[AdjustmentList],
    obligations: ObligationsResponse,
    periodKey: PeriodKey,
    dutyRatesMap: Map[String, BigDecimal]
  )(implicit messages: Messages): AdjustmentCheckYourAnswersViewModel = {
    AdjustmentCheckYourAnswersViewModel(
      declareAdjustment,
      adjustmentList,
      obligations,
      periodKey,
      dutyRatesMap
    )
  }

  private def withObligations(
    block: ObligationsResponse => Future[Result]
  )(implicit request: ReturnsDataRequest[AnyContent]): Future[Result] = {
    obligationService.getObligations(request.enrolmentVpdId).flatMap(block)
      .recover {
        case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  private def getDutyRatesForAdjustments(adjustmentList: Option[AdjustmentList])
                                        (implicit request: ReturnsDataRequest[_]): Future[Map[String, BigDecimal]] = {

    val adjustmentPeriods = adjustmentList.map(_.adjustments.map(_.period)).getOrElse(Seq.empty)

    Future.sequence(adjustmentPeriods.map { period =>
      dutyRateService.getDutyRate(request.enrolmentVpdId, period).map(rate => period.toString -> rate)
    }).map(_.toMap)
  }

}
