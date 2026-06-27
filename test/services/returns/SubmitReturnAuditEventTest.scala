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

package services.returns;

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsString, Json};

class SubmitReturnAuditEventTest extends AnyFreeSpec, Matchers {
    
    val submission = Json.parse(
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

    val response = Json.parse(
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
          |""".stripMargin)

    "Return Submission Audit Event" - {

        "must contain the submission section" in {
            SubmitReturnAuditEvent.buildExplicitAuditEvent(submission, response)("submission") must not be null
        }

        "must contain the response section" in {
            SubmitReturnAuditEvent.buildExplicitAuditEvent(submission, response)("response") mustBe response
        }

        "Submission section " - {
            "renames periodKey to returnPeriod" in {
                SubmitReturnAuditEvent.buildSubmission(submission).as[JsObject].keys must not contain "periodKey"
                SubmitReturnAuditEvent.buildSubmission(submission)("returnPeriod") mustBe JsString("24KA")
            }

            "renames volume fields to append Liters" in {
                SubmitReturnAuditEvent.buildSubmission(submission).as[JsObject].keys must not contain "amountProducedLiquid"
                SubmitReturnAuditEvent.buildSubmission(submission)("vapingProductsProduced")("returns").as[JsArray].head("amountProducedLiquidLitres") mustBe JsNumber(1500.25)
            }
        }
    }
}