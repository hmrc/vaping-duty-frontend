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
import models.NormalMode
import navigation.ReturnsNavigator
import pages.returns.DeclarationPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.DeclarationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.returns.submit.DeclarationView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DeclarationController @Inject()(
                                       override val messagesApi: MessagesApi,
                                       identify: ApprovedVapingManufacturerAuthAction,
                                       checkInsolvency: CheckInsolvencyAction,
                                       getData: ReturnsDataRetrievalAction,
                                       requireData: ReturnsDataRequiredAction,
                                       returnsEnabled: ReturnsEnabledAction,
                                       declarationService: DeclarationService,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: DeclarationView,
                                       navigator: ReturnsNavigator
                                     )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen checkInsolvency andThen returnsEnabled andThen getData andThen requireData).async {
    implicit request =>
      declarationService
        .buildViewModel(request.enrolmentVpdId, request.periodKey, request.userAnswers).map {
          case Some(vm) => Ok(view(vm))
          case None =>
            Redirect(s"${controllers.returns.submit.routes.ReturnSubmissionRecoveryController.onPageLoad().url}" +
              s"?period=${request.periodKey.value}")
        }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen returnsEnabled andThen getData andThen requireData).async {
    implicit request =>
      declarationService.submit(request.enrolmentVpdId, request.periodKey, request.userAnswers).map {
        case Right(_) => Redirect(navigator.nextPage(DeclarationPage, NormalMode, request.userAnswers))
        case Left(_)  => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }
}
