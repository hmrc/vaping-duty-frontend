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

package viewmodels.enrolment

import base.UnitSpec
import config.FrontendAppConfig

import org.mockito.Mockito.*


class OrganisationSignInViewModelSpec extends UnitSpec {

  "OrganisationSignInViewModel" - {

    val config = mock[FrontendAppConfig]
    when(config.organisationAcctGuidanceUrl).thenReturn("gg-organisation-create-account-guidance")
    when(config.organisationSignInUrl).thenReturn("gg-organisation-account-sign-in")

    val vm = OrganisationSignInViewModel(config)

    "returns correct URL for orgSignIn" in {
        vm.getOrgSignInURL mustBe config.organisationSignInUrl
    }

    "returns correct URL for orgAcctGuidance" in {
      vm.getGuidanceURL mustBe config.organisationAcctGuidanceUrl
    }

  }
}
