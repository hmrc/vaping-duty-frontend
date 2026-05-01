/*
 * Copyright 2025 HM Revenue & Customs
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

package base

import config.FrontendAppConfig
import connectors.contactPreference.EmailVerificationConnector
import controllers.actions.*
import controllers.actions.contactPreference.{DataRequiredAction, DataRequiredActionImpl, DataRetrievalAction, FakeDataRetrievalAction}
import controllers.actions.returns.{FakeReturnsDataRetrievalAction, ReturnsDataRetrievalAction}
import controllers.actions.enrolment.*
import data.TestData
import models.contactPreference.PreferenceUserAnswers
import models.enrolment.EnrolmentUserAnswers
import models.returns.ReturnsUserAnswers
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience
    with TestData {

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def applicationBuilder(userAnswers: Option[PreferenceUserAnswers] = None,
                                   enrolmentUserAnswers: Option[EnrolmentUserAnswers] = None,
                                   returnsUserAnswers: Option[ReturnsUserAnswers] = None,
                                   returnsEnabled: Boolean = true): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[ApprovedVapingManufacturerAuthAction].to[FakeApprovedVapingManufacturerAuthAction],
        bind[EnrolmentClaimAuthAction].to[FakeClaimEnrolmentAuthAction],
        bind[CheckSignedInAction].to[FakeCheckSignedInAction],
        bind[EnrolmentClaimAuthAction].to[FakeClaimEnrolmentAuthAction],
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[EnrolmentDataRetrievalAction].toInstance(new FakeEnrolmentDataRetrievalAction(enrolmentUserAnswers)),
        bind[ReturnsDataRetrievalAction].toInstance(new FakeReturnsDataRetrievalAction(returnsUserAnswers))
      )
      .configure(
        "features.returnsEnabled" -> returnsEnabled
      )
  
  given hc: HeaderCarrier = HeaderCarrier()
  given ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  given Messages = messages(app)

  private lazy val app: Application = applicationBuilder().build()
  
  val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val mockEmailVerificationConnector: EmailVerificationConnector = mock[EmailVerificationConnector]
}
