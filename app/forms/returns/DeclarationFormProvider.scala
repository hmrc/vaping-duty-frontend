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

package forms.returns

import forms.mappings.Mappings
import models.returns.DeclarationDetails
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class DeclarationFormProvider @Inject() extends Mappings {

  val FULL_NAME = "fullName"
  val CAPACITY = "capacityInWhichSigned"
  val EMAIL = "email"

  def apply(): Form[DeclarationDetails] =
    Form(
      mapping(
        FULL_NAME -> textWithSpaces("returns.declaration.fullName.error.required")
          .verifying(maxLength(120, "returns.declaration.fullName.error.length"))
          .verifying(regexp("""^[a-zA-Z0-9\-\.\s']+$""", "returns.declaration.fullName.error.invalidCharacters")),
        
        CAPACITY -> textWithSpaces("returns.declaration.capacity.error.required")
          .verifying(maxLength(100, "returns.declaration.capacity.error.length")),
        
        EMAIL -> text("returns.declaration.emailAddress.error.required")
          .verifying(maxLength(132, "returns.declaration.emailAddress.error.length"))
          .verifying(email("returns.declaration.emailAddress.error.format"))
      )((fullName, capacityInWhichSigned, signeesEmailAddress) => 
        DeclarationDetails(fullName, capacityInWhichSigned, signeesEmailAddress))(o => Some((o.fullName, o.capacityInWhichSigned, o.signeesEmailAddress)))
    )
}