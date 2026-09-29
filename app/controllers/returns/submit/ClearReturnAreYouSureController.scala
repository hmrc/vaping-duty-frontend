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

import controllers.actions.{ApprovedVapingManufacturerAuthAction, CheckInsolvencyAction}
import controllers.actions.returns.{ReturnsDataRequiredAction, ReturnsDataRetrievalAction, ReturnsEnabledAction}
import forms.returns.ClearReturnAreYouSureFormProvider
import models.identifiers.PeriodKey
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.returns.ReturnsUserAnswersService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.returns.submit.ClearReturnAreYouSureView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClearReturnAreYouSureController @Inject()(
                                                  override val messagesApi: MessagesApi,
                                                  identify: ApprovedVapingManufacturerAuthAction,
                                                  checkInsolvency: CheckInsolvencyAction,
                                                  getData: ReturnsDataRetrievalAction,
                                                  requireData: ReturnsDataRequiredAction,
                                                  returnsEnabledAction: ReturnsEnabledAction,
                                                  formProvider: ClearReturnAreYouSureFormProvider,
                                                  userAnswersService: ReturnsUserAnswersService,
                                                  val controllerComponents: MessagesControllerComponents,
                                                  view: ClearReturnAreYouSureView
                                                )(using ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen checkInsolvency andThen returnsEnabledAction andThen getData andThen requireData) {
    implicit request =>
      Ok(view(form, request.periodKey))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen checkInsolvency andThen returnsEnabledAction andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, request.periodKey))),

        confirmed =>
          if (confirmed) {
            userAnswersService.clear(request.enrolmentVpdId, request.periodKey).map {
              case Right(_) =>
                logger.info(s"Return ${request.enrolmentVpdId.value}/${request.periodKey.value} cleared")
                Redirect(withPeriod(controllers.returns.submit.routes.BeforeYouStartController.onPageLoad().url, request.periodKey))
              case Left(error) =>
                logger.warn(s"Unable to clear user answers: ${request.enrolmentVpdId.value}/${request.periodKey.value}: ${error.statusCode} ${error.message}")
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            }
          } else {
            Future.successful(Redirect(withPeriod(controllers.returns.submit.routes.TaskListController.onPageLoad().url, request.periodKey)))
          }
      )
  }

  private def withPeriod(url: String, periodKey: PeriodKey): String = s"$url?period=${periodKey.value}"
}
