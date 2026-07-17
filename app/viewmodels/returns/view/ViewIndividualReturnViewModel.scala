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
import utils.{CssConstants, CurrencyFormatter, PeriodKeys, ReturnsDateUtils}

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
                                          spoiltSummaryLists: Seq[SummaryList],
                                          adjustmentsSummaryLists: Seq[SummaryList],
                                          totalDutySummaryList: SummaryList,
                                          dutySuspenseSummaryList: Option[SummaryList]
                                         )

object ViewIndividualReturnViewModel extends CurrencyFormatter {

  def apply(
             returnsData: ReturnDisplayResponse,
             obligations: ObligationsResponse,
             returnsDateUtils: ReturnsDateUtils
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

    val monthYearString = s"${returnsDateUtils.getMonthMessage(monthFromLocalDate)} $year"
    val submittedOnString = s"$submittedOnDay $submittedOnMonth $year"

    val personalDetails = buildPersonalDetailsSummaryList(success.declaration)
    val dutyDeclaration = buildDutyDeclarationSummaryList(hasDeclaration, amountProduced, dutyDueAmount)
    val spoiltLists = buildSpoiltSummaryLists(success.spoiltProduct, isNilReturn, totalDutySpoiltProducts, obligations, returnsDateUtils)
    val adjustmentsLists = buildAdjustmentsSummaryLists(success.overDeclaration, success.underDeclaration, obligations, returnsDateUtils)
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
      spoiltSummaryLists = spoiltLists,
      adjustmentsSummaryLists = adjustmentsLists,
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
            value = Value(content = Text(duty), classes = CssConstants.boldFontWeight)
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

  private def buildSpoiltSummaryLists(
                                       spoiltProduct: Option[SpoiltProduct],
                                       nilReturn: Boolean,
                                       totalDutySpoiltProducts: String,
                                       obligations: ObligationsResponse,
                                       returnsDateUtils: ReturnsDateUtils
                                     )(using messages: Messages): Seq[SummaryList] = {
    spoiltProduct match {
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

        if (sp.spoiltProductFilled == "1") {
          val items = sp.spoiltProducts.getOrElse(Seq.empty)
          
          if (items.isEmpty) {
            Seq(SummaryList(rows = Seq(questionRow)))
          } else {
            // First list includes question + first item
            val firstItem = items.head
            val firstDutyDue = currencyFormat(firstItem.dutyDue.abs).replace("£", "-£")
            val firstList = SummaryList(rows = Seq(
              questionRow,
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.spoiltProducts.returnPeriodAffected"))),
                value = Value(content = Text(lookupPeriodKey(firstItem.returnPeriodAffected, obligations, returnsDateUtils)))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.spoiltProducts.amountSpoilt"))),
                value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(firstItem.amountSpoilt).toMl))))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.dutyDue"))),
                value = Value(content = Text(firstDutyDue), classes = CssConstants.boldFontWeight)
              )
            ))
            
            // Remaining items get their own lists
            val remainingLists = items.tail.map { item =>
              val dutyDue = currencyFormat(item.dutyDue.abs).replace("£", "-£")
              SummaryList(rows = Seq(
                SummaryListRow(
                  key = Key(content = Text(messages("viewIndividualReturn.spoiltProducts.returnPeriodAffected"))),
                  value = Value(content = Text(lookupPeriodKey(item.returnPeriodAffected, obligations, returnsDateUtils)))
                ),
                SummaryListRow(
                  key = Key(content = Text(messages("viewIndividualReturn.spoiltProducts.amountSpoilt"))),
                  value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(item.amountSpoilt).toMl))))
                ),
                SummaryListRow(
                  key = Key(content = Text(messages("viewIndividualReturn.dutyDue"))),
                  value = Value(content = Text(dutyDue), classes = CssConstants.boldFontWeight)
                )
              ))
            }
            Seq(firstList) ++ remainingLists
          }
        } else {
          Seq(SummaryList(rows = Seq(questionRow)))
        }
      case None => Seq.empty
    }
  }

  private def buildTotalDutySummaryList(totalDutyDue: Option[TotalDutyDue])(using messages: Messages): SummaryList = {
    val zeroValue = BigDecimal("0")

    totalDutyDue match {
      case Some(tdd) =>
        val rows = Seq(
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.totals.totalDutyDueVapingProducts")), classes = CssConstants.boldFontWeight),
            value = Value(content = Text(currencyFormat(tdd.totalDutyDueVapingProducts)))
          ),
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.totals.totalDutySpoiltProduct")), classes = CssConstants.boldFontWeight),
            value = Value(content = Text(
              if (tdd.totalDutySpoiltProduct == zeroValue) currencyFormat(tdd.totalDutySpoiltProduct)
              else currencyFormat(tdd.totalDutySpoiltProduct.abs).replace("£", "-£")
            ))
          ),
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.totals.totalDutyUnderDeclaration")), classes = CssConstants.boldFontWeight),
            value = Value(content = Text(currencyFormat(tdd.totalDutyUnderDeclaration)))
          ),
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.totals.totalDutyOverDeclaration")), classes = CssConstants.boldFontWeight),
            value = Value(content = Text(
              if (tdd.totalDutyOverDeclaration == zeroValue) currencyFormat(tdd.totalDutyOverDeclaration)
              else currencyFormat(tdd.totalDutyOverDeclaration.abs).replace("£", "-£")
            ))
          ),
          SummaryListRow(
            key = Key(content = Text(messages("viewIndividualReturn.totals.totalDue")), classes = CssConstants.boldFontWeight),
            value = Value(
              content = Text(
                if (tdd.totalDue < zeroValue) currencyFormat(tdd.totalDue.abs).replace("£", "-£")
                else currencyFormat(tdd.totalDue)
              ),
              classes = CssConstants.boldFontWeight
            )
          )
        )
        SummaryList(rows = rows)
      case None =>
        SummaryList(rows = Seq.empty)
    }
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

  private def buildAdjustmentsSummaryLists(
                                            overDeclaration: Option[OverDeclaration],
                                            underDeclaration: Option[UnderDeclaration],
                                            obligations: ObligationsResponse,
                                            returnsDateUtils: ReturnsDateUtils
                                          )(using messages: Messages): Seq[SummaryList] = {
    val hasOverDeclarations = overDeclaration.exists(_.overDeclFilled == "1")
    val hasUnderDeclarations = underDeclaration.exists(_.underDeclFilled == "1")
    val hasAnyAdjustments = hasOverDeclarations || hasUnderDeclarations

    val questionRow = SummaryListRow(
      key = Key(content = Text(messages("viewIndividualReturn.adjustments.question"))),
      value = Value(content = Text(
        if (hasAnyAdjustments) {
          messages("viewIndividualReturn.adjustments.yes")
        } else {
          messages("viewIndividualReturn.adjustments.no")
        }
      ))
    )

    if (hasAnyAdjustments) {
      val underDeclarationItems = underDeclaration
        .flatMap(_.underDeclarationProducts)
        .getOrElse(Seq.empty)

      val overDeclarationItems = overDeclaration
        .flatMap(_.overDeclarationProducts)
        .getOrElse(Seq.empty)

      val allItems = underDeclarationItems ++ overDeclarationItems

      if (allItems.isEmpty) {
        Seq(SummaryList(rows = Seq(questionRow)))
      } else {
        // First list includes question + first item
        val firstList = if (underDeclarationItems.nonEmpty) {
          val firstItem = underDeclarationItems.head
          SummaryList(rows = Seq(
            questionRow,
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.returnPeriodAffected"))),
              value = Value(content = Text(lookupPeriodKey(firstItem.returnPeriodAffected, obligations, returnsDateUtils)))
            ),
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.amountUnderDeclared"))),
              value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(firstItem.amountUnderDeclared).toMl))))
            ),
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.dutyDue"))),
              value = Value(content = Text(currencyFormat(firstItem.dutyDue)), classes = CssConstants.boldFontWeight)
            )
          ))
        } else {
          val firstItem = overDeclarationItems.head
          val dutyDueFormatted = currencyFormat(firstItem.dutyDue.abs).replace("£", "-£")
          SummaryList(rows = Seq(
            questionRow,
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.returnPeriodAffected"))),
              value = Value(content = Text(lookupPeriodKey(firstItem.returnPeriodAffected, obligations, returnsDateUtils)))
            ),
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.amountOverDeclared"))),
              value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(firstItem.amountOverDeclared).toMl))))
            ),
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.dutyDue"))),
              value = Value(content = Text(dutyDueFormatted), classes = CssConstants.boldFontWeight)
            )
          ))
        }

        // Remaining under declarations
        val remainingUnderLists = if (underDeclarationItems.nonEmpty) {
          underDeclarationItems.tail.map { item =>
            SummaryList(rows = Seq(
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.returnPeriodAffected"))),
                value = Value(content = Text(lookupPeriodKey(item.returnPeriodAffected, obligations, returnsDateUtils)))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.amountUnderDeclared"))),
                value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(item.amountUnderDeclared).toMl))))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.dutyDue"))),
                value = Value(content = Text(currencyFormat(item.dutyDue)), classes = CssConstants.boldFontWeight)
              )
            ))
          }
        } else {
          Seq.empty
        }

        // All over declarations (or remaining if first was over)
        val overLists = if (underDeclarationItems.isEmpty) {
          overDeclarationItems.tail.map { item =>
            val dutyDueFormatted = currencyFormat(item.dutyDue.abs).replace("£", "-£")
            SummaryList(rows = Seq(
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.returnPeriodAffected"))),
                value = Value(content = Text(lookupPeriodKey(item.returnPeriodAffected, obligations, returnsDateUtils)))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.amountOverDeclared"))),
                value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(item.amountOverDeclared).toMl))))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.dutyDue"))),
                value = Value(content = Text(dutyDueFormatted), classes = CssConstants.boldFontWeight)
              )
            ))
          }
        } else {
          overDeclarationItems.map { item =>
            val dutyDueFormatted = currencyFormat(item.dutyDue.abs).replace("£", "-£")
            SummaryList(rows = Seq(
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.returnPeriodAffected"))),
                value = Value(content = Text(lookupPeriodKey(item.returnPeriodAffected, obligations, returnsDateUtils)))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.amountOverDeclared"))),
                value = Value(content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(item.amountOverDeclared).toMl))))
              ),
              SummaryListRow(
                key = Key(content = Text(messages("viewIndividualReturn.adjustments.dutyDue"))),
                value = Value(content = Text(dutyDueFormatted), classes = CssConstants.boldFontWeight)
              )
            ))
          }
        }

        val adjustmentReason = underDeclaration.flatMap(_.reasonForUnderDecl)
          .orElse(overDeclaration.flatMap(_.reasonForOverDecl))

        val reasonList = adjustmentReason.map { reason =>
          SummaryList(rows = Seq(
            SummaryListRow(
              key = Key(content = Text(messages("viewIndividualReturn.adjustments.reason"))),
              value = Value(content = Text(reason))
            )
          ))
        }.toSeq

        Seq(firstList) ++ remainingUnderLists ++ overLists ++ reasonList
      }
    } else {
      Seq(SummaryList(rows = Seq(questionRow)))
    }
  }

  private def lookupPeriodKey(periodKey: String, obligations: ObligationsResponse, returnsDateUtils: ReturnsDateUtils)(using messages: Messages): String = {
    obligations.obligation
      .map(_.obligationDetails)
      .find(_.periodKey == periodKey)
      .map { obligation =>
        val month = obligation.iCFromDate.getMonth
        val year = obligation.iCFromDate.getYear
        s"${returnsDateUtils.getMonthMessage(month)} $year"
      }
      .getOrElse {
        // scalafix:off DisableSyntax.throw
        throw new IllegalStateException(s"Period key $periodKey not found in obligations. This indicates a data integrity issue.")
      }
  }
}
