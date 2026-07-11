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
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.DutyRateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.ReturnsDateUtils
import viewmodels.returns.submit.CheckYourAnswersViewModel
import views.html.returns.submit.CheckYourAnswersView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: ApprovedVapingManufacturerAuthAction,
                                            getData: ReturnsDataRetrievalAction,
                                            requireData: ReturnsDataRequiredAction,
                                            returnsEnabled: ReturnsEnabledAction,
                                            dutyRateService: DutyRateService,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: CheckYourAnswersView,
                                            returnsDateUtils: ReturnsDateUtils
                                          )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen returnsEnabled andThen getData andThen requireData).async { implicit request =>
    val pk = request.periodKey
    
    dutyRateService.getDutyRate(request.enrolmentVpdId, pk).map { dutyRate =>
      Ok(view(pk, CheckYourAnswersViewModel(request.userAnswers, dutyRate.dutyRateInPoundsPerMl, pk, returnsDateUtils)))
    }
  }

  def onSubmit: Action[AnyContent] = (identify andThen returnsEnabled andThen getData andThen requireData) { implicit request =>
      Redirect(s"${controllers.returns.submit.routes.DeclarationController.onPageLoad().url}?period=${request.periodKey}")
  }
}
