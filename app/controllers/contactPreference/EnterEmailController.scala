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

package controllers.contactPreference

import config.FrontendAppConfig
import connectors.EmailVerificationConnector
import controllers.actions.*
import forms.contactPreference.EnterEmailFormProvider
import models.emailverification.{EmailVerificationRequest, VerificationDetails}
import models.{ContactPreferenceUserAnswers, Mode, NormalMode}
import navigation.Navigator
import pages.contactPreference.EnterEmailPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{EmailVerificationService, UserAnswersService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.StartEmailVerificationJourneyHelper
import views.html.contactPreference.EnterEmailView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnterEmailController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      sessionService: UserAnswersService,
                                      navigator: Navigator,
                                      identify: ApprovedVapingManufacturerAuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: EnterEmailFormProvider,
                                      config: FrontendAppConfig,
                                      emailVerificationConnector: EmailVerificationConnector,
                                      emailVerificationService: EmailVerificationService,
                                      val controllerComponents: MessagesControllerComponents,
                                      view: EnterEmailView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.emailAddress match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => {
          Future.successful(BadRequest(view(formWithErrors, mode)))
        },
        value => {
          val updatedAnswers = request.userAnswers.copy(emailAddress = Some(value))

          emailVerificationService
            .retrieveAddressStatusAndAddToCache(VerificationDetails(request.credId), value, updatedAnswers).value.flatMap {
              case Left(error) =>
                logger.info("[EnterEmailController][onSubmit] Error updating verified email list: " +
                  s"${error.status} and message: ${error.message}")
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              case Right(verificationDetails) if verificationDetails.isLocked =>
                Future.successful(Redirect(controllers.contactPreference.routes.LockedEmailController.onPageLoad()))
              case Right(verificationDetails) =>
                handleRedirect(updatedAnswers, verificationDetails.emailAddress, request.credId)
            }
        }
      )
  }

  private def handleRedirect(updatedAnswers: ContactPreferenceUserAnswers, email: String, credId: String)
                            (implicit hc: HeaderCarrier, messages: Messages) = {
    sessionService.set(updatedAnswers).flatMap {
      case Left(error) =>
        logger.info("[EnterEmailController][handleRedirect] Error setting user answers with status: " +
          s"${error.status} and message: ${error.message}")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Right(_) =>
        if (updatedAnswers.verifiedEmailAddresses.contains(email)) {
          Future.successful(Redirect(navigator.nextPage(EnterEmailPage, NormalMode, updatedAnswers)))
        } else {
          startEmailVerification(email, credId)
        }
    }
  }

  private def startEmailVerification(email: String, credId: String)
                                    (implicit hc: HeaderCarrier, messages: Messages) = {
    
    val evRequest = StartEmailVerificationJourneyHelper(config).createRequest(credId, email)
    handoffToEmailVerification(evRequest)
  }

  private def handoffToEmailVerification(evRequest: EmailVerificationRequest)
                                        (implicit hc: HeaderCarrier) = {

    emailVerificationConnector.startEmailVerification(evRequest).map {
      case Left(error) =>
        logger.info("[EnterEmailController][handoffToEmailVerification] Error starting email verification with status: " +
          s"${error.status} and message: ${error.message}")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Right(redirectUri) =>
        val redirectTo = s"${config.emailVerificationRedirectBaseUrl}${redirectUri.redirectUri}"
        Redirect(redirectTo)
    }
  }
}
