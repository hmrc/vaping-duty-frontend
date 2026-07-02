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

import cats.data.Validated.{Invalid, Valid}
import controllers.actions.ApprovedVapingManufacturerAuthAction
import controllers.actions.returns.*
import models.NormalMode
import models.returns.validation.ValidationError.{DeclareDutyAnswerMissing, DutyAmountInvalid, DutyAmountMissing}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.{DutyRateService, UserAnswersValidationService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.submit.DeclareDutyCheckAnswersViewModel
import views.html.returns.submit.DeclareDutyCheckAnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclareDutyCheckAnswersController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   identify: ApprovedVapingManufacturerAuthAction,
                                                   getData: ReturnsDataRetrievalAction,
                                                   requireData: ReturnsDataRequiredAction,
                                                   returnsEnabled: ReturnsEnabledAction,
                                                   dutyRateService: DutyRateService,
                                                   validationService: UserAnswersValidationService,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   view: DeclareDutyCheckAnswersView
                                                 )(using ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen returnsEnabled andThen getData andThen requireData).async { implicit request =>
    validationService.validateDeclareDuty(request.userAnswers) match {
      case Valid(_) =>
        val pk = request.periodKey

        dutyRateService.getDutyRate(request.enrolmentVpdId, pk).map { dutyRate =>
          DeclareDutyCheckAnswersViewModel(request.userAnswers, dutyRate, pk) match {
            case Some(vm) => Ok(view(pk, vm))
            case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
        }

      case Invalid(errors) =>
        logger.warn(s"[DeclareDutyCheckAnswersController] Validation failed with errors: ${errors.map(_.message)}")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }


  def onSubmit: Action[AnyContent] = (identify andThen returnsEnabled andThen getData andThen requireData) { implicit request =>
    Redirect(controllers.returns.submit.routes.TaskListController.onPageLoad().url + s"?period=${request.periodKey.value}")
  }
}