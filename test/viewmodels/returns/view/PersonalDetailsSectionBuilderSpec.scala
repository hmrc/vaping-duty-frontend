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

package viewmodels.returns.view

import base.SpecBase
import models.returns.DeclarationDetails
import play.api.i18n.Messages

class PersonalDetailsSectionBuilderSpec extends SpecBase {

  implicit val messages: Messages = messages(applicationBuilder(None).build())

  private val declaration = DeclarationDetails(
    fullName = "Craig Cottingham",
    signeesEmailAddress = "craig@craigsvapes.co.uk",
    capacityInWhichSigned = "Manufacturer"
  )

  "PersonalDetailsSectionBuilder" - {

    "build must" - {

      "create summary list with 3 rows" in {
        val builder = PersonalDetailsSectionBuilder(declaration)
        val result = builder.build()

        result.rows.size mustBe 3
      }

      "include full name row" in {
        val builder = PersonalDetailsSectionBuilder(declaration)
        val result = builder.build()

        result.rows.head.key.content.asHtml.toString must include(messages("viewIndividualReturn.personalDetails.fullName"))
        result.rows.head.value.content.asHtml.toString must include("Craig Cottingham")
      }

      "include email row" in {
        val builder = PersonalDetailsSectionBuilder(declaration)
        val result = builder.build()

        result.rows(1).key.content.asHtml.toString must include(messages("viewIndividualReturn.personalDetails.email"))
        result.rows(1).value.content.asHtml.toString must include("craig@craigsvapes.co.uk")
      }

      "include capacity row" in {
        val builder = PersonalDetailsSectionBuilder(declaration)
        val result = builder.build()

        result.rows(2).key.content.asHtml.toString must include(messages("viewIndividualReturn.personalDetails.capacity"))
        result.rows(2).value.content.asHtml.toString must include("Manufacturer")
      }

      "handle special characters in names" in {
        val declarationWithSpecialChars = declaration.copy(
          fullName = "O'Brien-Smith & Co."
        )
        val builder = PersonalDetailsSectionBuilder(declarationWithSpecialChars)
        val result = builder.build()

        // HTML entities are encoded by Play's Text() wrapper
        val htmlOutput = result.rows.head.value.content.asHtml.toString()
        htmlOutput must include("O&#x27;Brien-Smith &amp; Co.")
      }
    }
  }
}
