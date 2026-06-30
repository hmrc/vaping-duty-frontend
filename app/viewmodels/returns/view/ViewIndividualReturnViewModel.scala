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

import models.obligations.ObligationsResponse
import models.returns.view.*
import models.returns.{ConvertToMl, DeclarationDetails, TotalDutyDue}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.ReturnsDateUtils.*
import utils.{CurrencyFormatter, PeriodKeys}

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}

case class ViewIndividualReturnViewModel(
                                          chargeReference: String,
                                          hasVapingProductsDeclaration: Boolean,
                                          amountProducedLiquid: Option[String],
                                          dutyDue: Option[String],
                                          totalDutySpoiltProducts: String,
                                          monthYear: String,
                                          submittedOn: String,
                                          dutyRate: String,
                                          nilReturn: Boolean,
                                          personalDetailsSummaryList: SummaryList,
                                          dutyDeclarationSummaryList: Option[SummaryList],
                                          spoiltSummaryList: SummaryList,
                                          totalDutySummaryList: SummaryList,
                                          dutySuspenseSummaryList: Option[SummaryList]
                                        )

object ViewIndividualReturnViewModel extends CurrencyFormatter {

  def apply(
             returnsData: ReturnDisplayResponse,
             obligations: ObligationsResponse
           )(using messages: Messages): ViewIndividualReturnViewModel = {

    val zeroValue = BigDecimal("0")
    val success = returnsData.success

    val dutyRate = returnsData.success.vapingProductsProduced
      .flatMap(_.returns.headOption)
      .map(_.dutyRate)

    val isNilReturn = dutyRate.forall(_ == zeroValue)

    val chargeRef = success.chargeDetails
      .flatMap(_.chargeReference)
      .getOrElse("")

    val vapingProducts = success.vapingProductsProduced

    val hasDeclaration = vapingProducts
      .exists(vp => vp.returns.nonEmpty)

    val (amountProduced, dutyDueAmount) = vapingProducts match {
      case Some(vp) if vp.returns.nonEmpty =>
        val regularReturn = vp.returns.head
        (Some(milliliterFormat(ConvertToMl(regularReturn.amountProducedLiquid).toMl)), Some(currencyFormat(regularReturn.dutyDue)))
      case _ =>
        (None, None)
    }

    val totalDutySpoiltProducts = success.totalDutyDue
      .fold(currencyFormat(zeroValue)) { td =>
        val formatted = currencyFormat(td.totalDutySpoiltProduct)
        if (td.totalDutySpoiltProduct == zeroValue) formatted
        else formatted.replace("£", "-£")
      }

    val year = success.chargeDetails.fold(LocalDate.now().getYear.toString)(_.periodFrom.getYear.toString)

    val monthFromLocalDate = success.chargeDetails.fold(LocalDate.now().getMonth)(_.periodFrom.getMonth)

    val receiptDate = success.chargeDetails.fold(Instant.now())(_.receiptDate)
    val submittedOn = LocalDateTime.ofInstant(receiptDate, ZoneId.systemDefault())
    val submittedOnDay = submittedOn.getDayOfMonth
    val submittedOnMonth = PeriodKeys.toDisplayName(submittedOn.getMonth)

    val monthYearString = s"${getMonthMessage(monthFromLocalDate)} $year"
    val submittedOnString = s"$submittedOnDay $submittedOnMonth $year"

    val personalDetails = buildPersonalDetailsSummaryList(success.declaration)
    val dutyDeclaration = buildDutyDeclarationSummaryList(hasDeclaration, amountProduced, dutyDueAmount)
    val spoilt = buildSpoiltSummaryList(success.spoiltProduct, isNilReturn, totalDutySpoiltProducts, obligations)
    val totalDutySummary = buildTotalDutySummaryList(success.totalDutyDue)
    val dutySuspenseSummary = buildDutySuspenseSummaryList(success.otherOptions)

    ViewIndividualReturnViewModel(
      chargeReference = chargeRef,
      hasVapingProductsDeclaration = hasDeclaration,
      amountProducedLiquid = amountProduced,
      dutyDue = dutyDueAmount,
      totalDutySpoiltProducts = totalDutySpoiltProducts,
      monthYear = monthYearString,
      submittedOn = submittedOnString,
      dutyRate = currencyFormat(dutyRate.getOrElse(zeroValue)),
      nilReturn = isNilReturn,
      personalDetailsSummaryList = personalDetails,
      dutyDeclarationSummaryList = dutyDeclaration,
      spoiltSummaryList = spoilt,
      totalDutySummaryList = totalDutySummary,
      dutySuspenseSummaryList = dutySuspenseSummary
    )
  }

  private def buildPersonalDetailsSummaryList(declarationDetails: DeclarationDetails)(using messages: Messages): SummaryList =
    SummaryList(
      rows = Seq(
        SummaryListRow(
          key = Key(content = Text(messages("viewIndividualReturn.personalDetails.fullName"))),
          value = Value(content = Text(declarationDetails.fullName))
        ),
        SummaryListRow(
          key = Key(content = Text(messages("viewIndividualReturn.personalDetails.email"))),
          value = Value(content = Text(declarationDetails.signeesEmailAddress))
        ),
        SummaryListRow(
          key = Key(content = Text(messages("viewIndividualReturn.personalDetails.capacity"))),
          value = Value(content = Text(declarationDetails.capacityInWhichSigned))
        )
      )
    )

  private def buildDutyDeclarationSummaryList(
                                               hasVapingProductsDeclaration: Boolean,
                                               amountProducedLiquid: Option[String],
                                               dutyDue: Option[String]
                                             )(using messages: Messages): Option[SummaryList] = {
    val detailRows = if (hasVapingProductsDeclaration) {
      Seq(
        amountProducedLiquid.map { amount =>
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.amountProducedLiquid"))),
            value = Value(content = Text(messages("viewIndividualReturn.millilitres", amount)))
          )
        },
        dutyDue.map { duty =>
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.dutyDue"))),
            value = Value(content = Text(duty))
          )
        }
      ).flatten
    } else Seq.empty

    Some(SummaryList(
      rows = Seq(
        SummaryListRow(
          key = Key(content = Text(messages("viewIndividualReturn.vapingProductsDeclaration.question"))),
          value = Value(
            content = Text(
              if (hasVapingProductsDeclaration) {
                messages("viewIndividualReturn.vapingProductsDeclaration.yes")
              } else {
                messages("viewIndividualReturn.vapingProductsDeclaration.no")
              }
            )
          )
        )) ++ detailRows
    ))
  }

  private def buildSpoiltSummaryList(
                                      spoiltProduct: Option[SpoiltProduct],
                                      nilReturn: Boolean,
                                      totalDutySpoiltProducts: String,
                                      obligations: ObligationsResponse
                                    )(using messages: Messages): SummaryList = {
    val spoiltProductsRows = spoiltProduct match {
      case Some(sp) =>
        val yesNoText = if (sp.spoiltProductFilled == "1") {
          messages("viewIndividualReturn.spoiltProducts.yes")
        } else {
          messages("viewIndividualReturn.spoiltProducts.no")
        }

        val questionRow = SummaryListRow(
          key = Key(content = Text(messages("viewIndividualReturn.spoiltProducts.question"))),
          value = Value(content = Text(yesNoText))
        )

        val detailRows = if (sp.spoiltProductFilled == "1") {
          sp.spoiltProducts.getOrElse(Seq.empty).flatMap { item =>
            Seq(
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.spoiltProducts.month"))),
                value = Value(content = Text(lookupPeriodKey(item.returnPeriodAffected, obligations)))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.spoiltProducts.spoiltProducts"))),
                value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(item.amountSpoilt).toMl))))
              )
            )
          }
        } else Seq.empty

        questionRow +: detailRows
      case None => Seq.empty
    }

    val spoiltDeclared = spoiltProduct.exists(_.spoiltProductFilled == "1")

    val totalRows = if (nilReturn) {
      Seq.empty
    } else {
      if (spoiltDeclared) Seq(
        SummaryListRow(
          key = Key(content = Text(messages("viewIndividualReturn.totalDutySpoiltProducts"))),
          value = Value(content = Text(totalDutySpoiltProducts))
        )
      ) else Seq.empty
    }

    SummaryList(rows = spoiltProductsRows ++ totalRows)
  }

  private def buildTotalDutySummaryList(totalDutyDue: Option[TotalDutyDue])(using messages: Messages): SummaryList = {
    val zeroValue = BigDecimal("0")

    val totalDuty = totalDutyDue.fold(zeroValue)(_.totalDue)

    val displayTotalDutyDue =
      if (totalDuty < zeroValue) currencyFormat(totalDuty.abs).replace("£", "-£")
      else currencyFormat(totalDuty)


    SummaryList(
      if (totalDuty != zeroValue) {
        Seq(SummaryListRow(
          key = Key(content = Text(messages("viewIndividualReturn.totalDutyDue"))),
          value = Value(content = Text(displayTotalDutyDue))
        ))
      } else {
        Seq.empty
      }
    )
  }

  private def buildDutySuspenseSummaryList(otherOptions: Option[OtherOptions])(using messages: Messages): Option[SummaryList] = {
    otherOptions.map { options =>
      val yesNoText = if (options.vapingProductUnderDutySuspense == "1") {
        messages("viewIndividualReturn.dutySuspense.yes")
      } else {
        messages("viewIndividualReturn.dutySuspense.no")
      }

      val questionRow = SummaryListRow(
        key = Key(content = Text(messages("viewIndividualReturn.dutySuspense.question"))),
        value = Value(content = Text(yesNoText))
      )

      val detailRows = if (options.vapingProductUnderDutySuspense == "1") {
        val receivedValue = options.volumeMovedFromDutySuspense match {
          case Some(volume) if volume > 0 => messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(volume).toMl))
          case _ => messages("viewIndividualReturn.dutySuspense.nothingToDeclare")
        }

        val movedValue = options.volumeMovedToDutySuspense match {
          case Some(volume) if volume > 0 => messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(volume).toMl))
          case _ => messages("viewIndividualReturn.dutySuspense.nothingToDeclare")
        }

        Seq(
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.dutySuspense.productReceived"))),
            value = Value(content = Text(receivedValue))
          ),
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.dutySuspense.productMoved"))),
            value = Value(content = Text(movedValue))
          )
        )
      } else Seq.empty

      SummaryList(rows = questionRow +: detailRows)
    }
  }

  private def lookupPeriodKey(periodKey: String, obligations: ObligationsResponse)(using messages: Messages): String = {
    obligations.obligation
      .map(_.obligationDetails)
      .find(_.periodKey == periodKey)
      .map { obligation =>
        val month = obligation.iCFromDate.getMonth
        val year = obligation.iCFromDate.getYear
        s"${getMonthMessage(month)} $year"
      }
      .getOrElse {
        // scalafix:off DisableSyntax.throw
        throw new IllegalStateException(s"Period key $periodKey not found in obligations. This indicates a data integrity issue.")
      }
  }
}
