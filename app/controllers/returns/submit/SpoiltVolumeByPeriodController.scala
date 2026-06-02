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

import controllers.actions.ApprovedVapingManufacturerAuthAction
import controllers.actions.returns.*
import forms.returns.SpoiltVolumeByPeriodFormProvider
import models.identifiers.PeriodKey
import models.returns.SpoiltVolumeByPeriod
import pages.returns.SpoiltVolumeByPeriodPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.{ObligationService, ReturnsUserAnswersService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.submit.SpoiltVolumeByPeriodViewModel
import views.html.returns.submit.SpoiltVolumeByPeriodView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SpoiltVolumeByPeriodController @Inject()(
  override val messagesApi: MessagesApi,
  sessionRepository: ReturnsUserAnswersService,
  identify: ApprovedVapingManufacturerAuthAction,
  getData: ReturnsDataRetrievalAction,
  requireData: ReturnsDataRequiredAction,
  formProvider: SpoiltVolumeByPeriodFormProvider,
  returnsEnabledAction: ReturnsEnabledAction,
  obligationService: ObligationService,
  val controllerComponents: MessagesControllerComponents,
  view: SpoiltVolumeByPeriodView
)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[Int] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>
      
      request.getQueryString("spoiltPeriod") match {
        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Some(spoiltPeriodStr) =>
          val spoiltPeriod = PeriodKey(spoiltPeriodStr)
          
          obligationService.getObligationByPeriodKey(request.enrolmentVpdId, spoiltPeriod).map {
            case Some(obligation) =>
              val viewModel = SpoiltVolumeByPeriodViewModel(obligation, spoiltPeriod, request.periodKey)
              
              val preparedForm = request.userAnswers.get(SpoiltVolumeByPeriodPage) match {
                case Some(spoiltVolume) if spoiltVolume.periodKey == spoiltPeriod => form.fill(spoiltVolume.volume)
                case _ => form
              }
              
              Ok(view(viewModel, preparedForm))
              
            case None =>
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }.recover {
            case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>
      
      request.getQueryString("spoiltPeriod") match {
        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Some(spoiltPeriodStr) =>
          val spoiltPeriod = PeriodKey(spoiltPeriodStr)
          
          obligationService.getObligationByPeriodKey(request.enrolmentVpdId, spoiltPeriod).flatMap {
            case Some(obligation) =>
              val viewModel = SpoiltVolumeByPeriodViewModel(obligation, spoiltPeriod, request.periodKey)
              
              form.bindFromRequest().fold(
                formWithErrors =>
                  Future.successful(BadRequest(view(viewModel, formWithErrors))),

                volume =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(SpoiltVolumeByPeriodPage, SpoiltVolumeByPeriod(volume, spoiltPeriod)))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(s"${controllers.returns.submit.routes.TaskListController.onPageLoad().url}?period=${request.periodKey.value}")
              )
              
            case None =>
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }.recover {
            case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      }
  }
}