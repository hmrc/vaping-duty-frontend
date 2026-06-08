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

import controllers.actions.*
import controllers.actions.returns.{ReturnsDataRetrievalAction, ReturnsEnabledAction}
import models.identifiers.PeriodKey
import models.returns.ReturnsUserAnswers
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.{ObligationService, ReturnsUserAnswersService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.returns.submit.BeforeYouStartViewModel
import views.html.returns.submit.BeforeYouStartView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class BeforeYouStartController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          identify: ApprovedVapingManufacturerAuthAction,
                                          sessionRepository: ReturnsUserAnswersService,
                                          returnsEnabledAction: ReturnsEnabledAction,
                                          obligationService: ObligationService,
                                          val controllerComponents: MessagesControllerComponents,
                                          view: BeforeYouStartView,
                                          getData: ReturnsDataRetrievalAction
                                     )(using ExecutionContext) extends FrontendBaseController with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen returnsEnabledAction andThen getData).async {
    implicit request =>
      val periodKey = PeriodKey(request.getQueryString("period").getOrElse(""))

      val ua = request.userAnswers match {
        case Some(existingUa) if existingUa.periodKey == periodKey.toString =>
          existingUa
        case _ =>
          ReturnsUserAnswers.getEmptyReturnsUA(request.enrolmentVpdId, periodKey)
      }

      obligationService.getObligations(request.enrolmentVpdId).flatMap { obligations =>
        sessionRepository.set(ua).map(_ =>
          Ok(view(periodKey, BeforeYouStartViewModel(obligations.obligation, periodKey)))
        )
      }

  }
}