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

package controllers.returns.submit.spoilt

import controllers.actions.ApprovedVapingManufacturerAuthAction
import controllers.actions.returns.*
import controllers.returns.PeriodKeyExtraction
import forms.returns.SpoiltVolumeByPeriodFormProvider
import models.{Mode, NormalMode}
import models.identifiers.PeriodKey
import models.obligations.ObligationDetails
import models.requests.returns.ReturnsDataRequest
import models.returns.SpoiltVolumeByPeriod
import navigation.ReturnsNavigator
import pages.returns.SpoiltVolumeByPeriodPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import services.returns.{ObligationService, ReturnsUserAnswersService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.submit.SpoiltVolumeByPeriodViewModel
import views.html.returns.submit.spoilt.SpoiltVolumeByPeriodView

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
                                                navigator: ReturnsNavigator,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: SpoiltVolumeByPeriodView
                                              )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with PeriodKeyExtraction {

  def onPageLoad(mode: Mode = NormalMode): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>
      withPeriodKey("spoiltPeriod") { spoiltPeriod =>
        withObligation(spoiltPeriod) { spoiltObligation =>
          formProvider(spoiltPeriod, request.enrolmentVpdId).flatMap { form =>
            val vm = createViewModel(spoiltObligation)

            val preparedForm = request.userAnswers.get(SpoiltVolumeByPeriodPage) match {
              case Some(list) =>
                list.find(_.periodKey == spoiltPeriod) match {
                  case Some(spoiltVolume) => form.fill(spoiltVolume.volume)
                  case None => form
                }
              case None => form
            }

            Future.successful(Ok(view(vm, preparedForm, mode)))
          }
        }
      }
  }

  def onSubmit(mode: Mode = NormalMode): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>
      withPeriodKey("spoiltPeriod") { spoiltPeriod =>
        withObligation(spoiltPeriod) { spoiltObligation =>
          formProvider(spoiltPeriod, request.enrolmentVpdId).flatMap { form =>
            val vm = createViewModel(spoiltObligation)

            form.bindFromRequest().fold(
              formWithErrors =>
                Future.successful(BadRequest(view(vm, formWithErrors, mode))),

              volume =>
                val existingList = request.userAnswers.get(SpoiltVolumeByPeriodPage).getOrElse(List.empty)
                val updatedList = existingList.filterNot(_.periodKey == spoiltPeriod) :+ SpoiltVolumeByPeriod(volume, spoiltPeriod)

                for {
                  updatedAnswers  <- Future.fromTry(request.userAnswers.set(SpoiltVolumeByPeriodPage, updatedList))
                  _               <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(SpoiltVolumeByPeriodPage, mode, updatedAnswers))
            )
          }
        }
      }
  }

  private def withObligation(spoiltPeriod: PeriodKey)(block: ObligationDetails => Future[Result])
                            (implicit request: ReturnsDataRequest[AnyContent]): Future[Result] = {

    obligationService.getObligationByPeriodKey(request.enrolmentVpdId, spoiltPeriod).flatMap {
      case Some(spoiltObligation) => block(spoiltObligation)
      case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }.recover {
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  private def createViewModel(spoiltObligation: ObligationDetails)
                             (implicit request: ReturnsDataRequest[AnyContent]): SpoiltVolumeByPeriodViewModel = {

    SpoiltVolumeByPeriodViewModel(spoiltObligation, request.periodKey)
  }
}
