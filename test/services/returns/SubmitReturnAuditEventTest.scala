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

package services.returns

import models.identifiers.*
import models.returns.submit.ReturnSubmittedResponse
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsString, Json}

class SubmitReturnAuditEventTest extends AnyFreeSpec, Matchers {

  private val etmpSubmission = Json.parse(
    """
          {
            "periodKey": "24KA",
            "vapingProductsProduced": {
              "vapingProdManufactured": "1",
              "returns": [
                {
                  "taxType": "641",
                  "dutyRate": 10.5,
                  "amountProducedLiquid": 1500.25,
                  "dutyDue": 15752.63
                }
              ]
            },
            "underDeclaration": {
              "underDeclFilled": "1",
              "reasonForUnderDecl": "Incorrect reporting in previous return",
              "underDeclarationProducts": [
                {
                  "returnPeriodAffected": "23KB",
                  "taxType": "641",
                  "dutyRate": 10.5,
                  "amountUnderDeclared": 200,
                  "dutyDue": 2100
                }
              ]
            },
            "overDeclaration": {
              "overDeclFilled": "1",
              "reasonForOverDecl": "Duplicate entry in prior submission",
              "overDeclarationProducts": [
                {
                  "returnPeriodAffected": "23KC",
                  "taxType": "641",
                  "dutyRate": 10.5,
                  "amountOverDeclared": 100,
                  "dutyDue": 1050
                }
              ]
            },
            "spoiltProduct": {
              "spoiltProductFilled": "1",
              "spoiltProducts": [
                {
                  "returnPeriodAffected": "24KA",
                  "taxType": "641",
                  "dutyRate": 10.5,
                  "amountSpoilt": 50,
                  "dutyDue": 525
                }
              ]
            },
            "totalDutyDue": {
              "totalDutyDueVapingProducts": 15752.63,
              "totalDutyOverDeclaration": 1050,
              "totalDutyUnderDeclaration": 2100,
              "totalDutySpoiltProduct": 525,
              "totalDue": 16277.63
            },
            "otherOptions": {
              "vapingProductUnderDutySuspense": "1",
              "volumeMovedFromDutySuspense": 300,
              "volumeMovedToDutySuspense": 150
            },
            "declaration": {
              "fullName": "John Smith",
              "capacityInWhichSigned": "Director",
              "signeesEmailAddress": "john.smith@example.com"
            }
          }
      """)

  private val etmpResponse = Json.parse(
    """
      |{
      |  "success": {
      |    "processingDate": "2026-05-08T10:30:00Z",
      |    "vpdReferenceNumber": "GBWK1234567WK",
      |    "submissionId": "123456789012",
      |    "chargeReference": "AB123456789012",
      |    "amount": 16277.63,
      |    "paymentDueDate": "2026-06-07"
      |  }
      |}
      |""".stripMargin)("success").as[ReturnSubmittedResponse]

  private val identifiers = Identifiers(
    VpdId("GBWK1234567WK"),
    GroupId("6031A986-C7F1-4D14-BA98-04A3AD8871B7"),
    InternalId("not used"),
    CredentialId("Int-53b2f41d-af8b-4372-afce-b9bb95b2dd86"))

  "Return Submission Audit Event" - {
    "must contain the submission section" in {
      Option(SubmitReturnAuditEvent.buildExplicitAuditEvent(etmpSubmission, etmpResponse, identifiers)("submission")) must not be None
    }

    "must contain the response section" in {
      Option(SubmitReturnAuditEvent.buildExplicitAuditEvent(etmpSubmission, etmpResponse, identifiers)("response")) must not be None
    }

    "must contain the prePopulatedData section" in {
      Option(SubmitReturnAuditEvent.buildExplicitAuditEvent(etmpSubmission, etmpResponse, identifiers)("prePopulatedData")) must not be None
    }

    "response section" - {
      "includes submissionId if present in the etmp response" in {
        SubmitReturnAuditEvent.buildResponse(etmpResponse)("submissionId") mustBe JsString("123456789012")
      }
      "Does not include the submissionId if not present in the etmp response" in {
        SubmitReturnAuditEvent.buildResponse(etmpResponse.copy(submissionId = None)).as[JsObject].keys must not contain "submissionId"
      }

      "includes chargeReference if present in the etmp response" in {
        SubmitReturnAuditEvent.buildResponse(etmpResponse)("chargeReference") mustBe JsString("AB123456789012")
      }
      "Does not include the chargeReference if not present in the etmp response" in {
        SubmitReturnAuditEvent.buildResponse(etmpResponse.copy(chargeReference = None)).as[JsObject].keys must not contain "chargeReference"
      }

      "includes paymentDueDate if present in the etmp response" in {
        SubmitReturnAuditEvent.buildResponse(etmpResponse)("paymentDueDate") mustBe JsString("2026-06-07")
      }
      "Does not include the paymentDueDate if not present in the etmp response" in {
        SubmitReturnAuditEvent.buildResponse(etmpResponse.copy(paymentDueDate = None)).as[JsObject].keys must not contain "paymentDueDate"
      }

      "removes processingDate" in {
        SubmitReturnAuditEvent.buildResponse(etmpResponse).as[JsObject].keys must not contain "processingDate"
      }

      "removes vpdReferenceNumber" in {
        SubmitReturnAuditEvent.buildResponse(etmpResponse).as[JsObject].keys must not contain "vpdReferenceNumber"
      }

      "removes amount" in {
        SubmitReturnAuditEvent.buildResponse(etmpResponse).as[JsObject].keys must not contain "amount"
      }
    }

    "prePopulatedData section" - {
      "includes the approvalId" in {
        SubmitReturnAuditEvent.buildPrePopulatedData(identifiers)("approvalId") mustBe JsString("GBWK1234567WK")
      }

      "includes the credentialId" in {
        SubmitReturnAuditEvent.buildPrePopulatedData(identifiers)("credentialId") mustBe JsString("Int-53b2f41d-af8b-4372-afce-b9bb95b2dd86")
      }

      "includes the groupId" in {
        SubmitReturnAuditEvent.buildPrePopulatedData(identifiers)("groupId") mustBe JsString("6031A986-C7F1-4D14-BA98-04A3AD8871B7")
      }
    }

    "Submission section" - {
      "renames periodKey to returnPeriod" in {
        SubmitReturnAuditEvent.buildSubmission(etmpSubmission).as[JsObject].keys must not contain "periodKey"
        SubmitReturnAuditEvent.buildSubmission(etmpSubmission)("returnPeriod") mustBe JsString("24KA")
      }

      "renames decl to declaration" - {
        "under declaration" - {
          "renames underDeclFilled to underDeclarationFilled" in {
            val underDeclarationObj = SubmitReturnAuditEvent.buildSubmission(etmpSubmission)("underDeclaration")

            underDeclarationObj.as[JsObject].keys must not contain "underDeclFilled"
            underDeclarationObj("underDeclarationFilled") mustBe JsString("1")
          }

          "renames reasonForUnderDecl to reasonForUnderDeclaration" in {
            val underDeclarationObj = SubmitReturnAuditEvent.buildSubmission(etmpSubmission)("underDeclaration")

            underDeclarationObj.as[JsObject].keys must not contain "reasonForUnderDecl"
            underDeclarationObj("reasonForUnderDeclaration") mustBe JsString("Incorrect reporting in previous return")
          }
        }

        "over declaration" - {
          "renames overDeclFilled to overDeclarationFilled" in {
            val overDeclarationObj = SubmitReturnAuditEvent.buildSubmission(etmpSubmission)("overDeclaration")

            overDeclarationObj.as[JsObject].keys must not contain "overDeclFilled"
            overDeclarationObj("overDeclarationFilled") mustBe JsString("1")
          }

          "renames reasonForOverDecl to reasonForOverDeclaration" in {
            val overDeclarationObj = SubmitReturnAuditEvent.buildSubmission(etmpSubmission)("overDeclaration")

            overDeclarationObj.as[JsObject].keys must not contain "reasonForOverDecl"
            overDeclarationObj("reasonForOverDeclaration") mustBe JsString("Duplicate entry in prior submission")
          }
        }
      }

      "renames Prod to Products" - {
        "renames vapingProdManufactured to vapingProductsManufactured" in {
          val returnJsObj = SubmitReturnAuditEvent.buildSubmission(etmpSubmission)("vapingProductsProduced").as[JsObject]

          returnJsObj.keys must not contain "vapingProdManufactured"
          returnJsObj("vapingProductsManufactured") mustBe JsString("1")
        }
      }

      "renames volume fields to append Litres" - {

        "vapingProductsProduced.returns.amountProducedLiquid" in {
          val returnJsObj = SubmitReturnAuditEvent.buildSubmission(etmpSubmission)("vapingProductsProduced")("returns").as[JsArray].head

          returnJsObj.as[JsObject].keys must not contain "amountProducedLiquid"
          returnJsObj("amountProducedLiquidLitres") mustBe JsNumber(1500.25)
        }

        "underDeclaration.underDeclarationProducts.amountUnderDeclared" in {
          val underDeclarationObj = SubmitReturnAuditEvent.buildSubmission(etmpSubmission)("underDeclaration")("underDeclarationProducts").as[JsArray].head

          underDeclarationObj.as[JsObject].keys must not contain "amountUnderDeclared"
          underDeclarationObj("amountUnderDeclaredLitres") mustBe JsNumber(200)
        }

        "overDeclaration.overDeclarationProducts.amountOverDeclared" in {
          val overDeclarationObj = SubmitReturnAuditEvent.buildSubmission(etmpSubmission)("overDeclaration")("overDeclarationProducts").as[JsArray].head

          overDeclarationObj.as[JsObject].keys must not contain "amountOverDeclared"
          overDeclarationObj("amountOverDeclaredLitres") mustBe JsNumber(100)
        }

        "spoiltProduct.spoiltProducts.amountSpoilt" in {
          val spoiltProductObj = SubmitReturnAuditEvent.buildSubmission(etmpSubmission)("spoiltProduct")("spoiltProducts").as[JsArray].head

          spoiltProductObj.as[JsObject].keys must not contain "amountSpoilt"
          spoiltProductObj("amountSpoiltLitres") mustBe JsNumber(50)
        }

        "otherOptions.volumeMovedFromDutySuspense" in {
          val otherOptionsObj = SubmitReturnAuditEvent.buildSubmission(etmpSubmission)("otherOptions").as[JsObject]

          otherOptionsObj.keys must not contain "volumeMovedFromDutySuspense"
          otherOptionsObj("volumeMovedFromDutySuspenseLitres") mustBe JsNumber(300)
        }

        "otherOptions.volumeMovedToDutySuspense" in {
          val otherOptionsObj = SubmitReturnAuditEvent.buildSubmission(etmpSubmission)("otherOptions").as[JsObject]

          otherOptionsObj.keys must not contain "volumeMovedToDutySuspense"
          otherOptionsObj("volumeMovedToDutySuspenseLitres") mustBe JsNumber(150)
        }
      }
    }
  }
}
