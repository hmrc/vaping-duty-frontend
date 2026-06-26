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

import models.returns.{ConvertToMl, DeclarationDetails}
import models.returns.view.*
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import utils.{CurrencyFormatter, PeriodKeys}
import utils.ReturnsDateUtils.*

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}

case class ViewIndividualReturnViewModel(
                                          chargeReference: String,
                                          hasVapingProductsDeclaration: Boolean,
                                          amountProducedLiquid: Option[String],
                                          dutyDue: Option[String],
                                          totalDutySpoiltProducts: String,
                                          totalDutyDue: String,
                                          monthYear: String,
                                          submittedOn: String,
                                          dutyRate: String,
                                          nilReturn: Boolean,
                                          declarationDetails: DeclarationDetails,
                                          spoiltProduct: Option[SpoiltProduct],
                                          dutySuspenseSummaryList: Option[SummaryList]
                                        ) extends CurrencyFormatter{

  private def formatPeriodKey(periodKey: String)(implicit messages: Messages): String = {
    val year = 2000 + periodKey.take(2).toInt
    val periodCode = periodKey.drop(2)
    
    val monthNumber = (periodCode.charAt(1) - 'A') + 1
    val month = java.time.Month.of(monthNumber)
    
    s"${getMonthMessage(month)} $year"
  }

  def personalDetailsSummaryList(implicit messages: Messages): SummaryList =
    SummaryList(
      rows = Seq(
        SummaryListRow(
          key = Key(
            content = Text(messages("viewIndividualReturn.personalDetails.fullName"))
          ),
          value = Value(
            content = Text(declarationDetails.fullName)
          )
        ),
        SummaryListRow(
          key = Key(
            content = Text(messages("viewIndividualReturn.personalDetails.email"))
          ),
          value = Value(
            content = Text(declarationDetails.signeesEmailAddress)
          )
        ),
        SummaryListRow(
          key = Key(
            content = Text(messages("viewIndividualReturn.personalDetails.capacity"))
          ),
          value = Value(
            content = Text(declarationDetails.capacityInWhichSigned)
          )
        )
      )
    )

  def vapingProductsDeclarationSummaryList(implicit messages: Messages): SummaryList =
    SummaryList(
      rows = Seq(
        SummaryListRow(
          key = Key(
            content = Text(messages("viewIndividualReturn.vapingProductsDeclaration.question"))
          ),
          value = Value(
            content = Text(
              if(hasVapingProductsDeclaration) {
                messages("viewIndividualReturn.vapingProductsDeclaration.yes")
              } else {
                messages("viewIndividualReturn.vapingProductsDeclaration.no")
              }
            )
          )
        )
      )
    )

  def productDetailsSummaryList(implicit messages: Messages): Option[SummaryList] =
    if(hasVapingProductsDeclaration) {
      Some(SummaryList(
        rows = Seq(
          amountProducedLiquid.map { amount =>
            SummaryListRow(
              key = Key(
                content = Text(messages("viewIndividualReturn.amountProducedLiquid"))
              ),
              value = Value(
                content = Text(messages("viewIndividualReturn.millilitres", amount))
              )
            )
          },
          dutyDue.map { duty =>
            SummaryListRow(
              key = Key(
                content = Text(messages("viewIndividualReturn.dutyDue"))
              ),
              value = Value(
                content = Text(duty)
              )
            )
          }
        ).flatten
      ))
    } else {
      None
    }

  def dutyTotalsSummaryList(implicit messages: Messages): SummaryList = {
    val spoiltProductsRows = spoiltProduct match {
      case Some(sp) =>
        val yesNoText = if (sp.spoiltProductFilled == "1") {
          messages("viewIndividualReturn.spoiltProducts.yes")
        } else {
          messages("viewIndividualReturn.spoiltProducts.no")
        }
        
        val questionRow = SummaryListRow(
          key = Key(
            content = Text(messages("viewIndividualReturn.spoiltProducts.question"))
          ),
          value = Value(
            content = Text(yesNoText)
          )
        )
        
        val detailRows = if (sp.spoiltProductFilled == "1") {
          sp.spoiltProducts.getOrElse(Seq.empty).flatMap { item =>
            Seq(
              SummaryListRow(
                key = Key(
                  content = Text(messages("viewIndividualReturn.spoiltProducts.month"))
                ),
                value = Value(
                  content = Text(formatPeriodKey(item.returnPeriodAffected))
                )
              ),
              SummaryListRow(
                key = Key(
                  content = Text(messages("viewIndividualReturn.spoiltProducts.spoiltProducts"))
                ),
                value = Value(
                  content = Text(messages("viewIndividualReturn.millilitres", milliliterFormat(ConvertToMl(item.amountSpoilt).toMl)))
                )
              )
            )
          }
        } else Seq.empty
        
        questionRow +: detailRows
      case None => Seq.empty
    }
    
    val spoiltDeclared = spoiltProduct.exists(_.spoiltProductFilled == "1")

    val totalRows = if (nilReturn) Seq.empty
    else {
      val spoiltTotalRow = if (spoiltDeclared) Seq(
        SummaryListRow(
          key = Key(
            content = Text(messages("viewIndividualReturn.totalDutySpoiltProducts"))
          ),
          value = Value(
            content = Text(totalDutySpoiltProducts)
          )
        )
      ) else Seq.empty

      spoiltTotalRow :+ SummaryListRow(
        key = Key(
          content = Text(messages("viewIndividualReturn.totalDutyDue"))
        ),
        value = Value(
          content = Text(totalDutyDue)
        )
      )
    }
    
    SummaryList(rows = spoiltProductsRows ++ totalRows)
  }

}

object ViewIndividualReturnViewModel extends CurrencyFormatter {

  def apply(returnsData: ReturnDisplayResponse, dutyRate: Option[BigDecimal])(using messages: Messages): ViewIndividualReturnViewModel = {
    val zeroValue = BigDecimal("0")
    val success = returnsData.success
    
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

    val totalDuty = success.totalDutyDue
      .fold(currencyFormat(zeroValue))(td => currencyFormat(td.totalDue))
    
    val year = success.chargeDetails.fold(LocalDate.now().getYear.toString)(_.periodFrom.getYear.toString)
    
    val monthFromLocalDate = success.chargeDetails.fold(LocalDate.now().getMonth)(_.periodFrom.getMonth)

    val receiptDate = success.chargeDetails.fold(Instant.now())(_.receiptDate)
    val submittedOn = LocalDateTime.ofInstant(receiptDate, ZoneId.systemDefault())
    val submittedOnDay = submittedOn.getDayOfMonth
    val submittedOnMonth = PeriodKeys.toDisplayName(submittedOn.getMonth)


    val monthYearString = s"${getMonthMessage(monthFromLocalDate)} $year"
    val submittedOnString = s"$submittedOnDay $submittedOnMonth $year"

    val dutySuspenseSummary = success.otherOptions.map { options =>
      val yesNoText = if (options.vapingProductUnderDutySuspense == "1") {
        messages("viewIndividualReturn.dutySuspense.yes")
      } else {
        messages("viewIndividualReturn.dutySuspense.no")
      }

      val questionRow = SummaryListRow(
        key = Key(
          content = Text(messages("viewIndividualReturn.dutySuspense.question"))
        ),
        value = Value(
          content = Text(yesNoText)
        )
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
            key = Key(
              content = Text(messages("viewIndividualReturn.dutySuspense.productReceived"))
            ),
            value = Value(
              content = Text(receivedValue)
            )
          ),
          SummaryListRow(
            key = Key(
              content = Text(messages("viewIndividualReturn.dutySuspense.productMoved"))
            ),
            value = Value(
              content = Text(movedValue)
            )
          )
        )
      } else Seq.empty

      SummaryList(rows = questionRow +: detailRows)
    }

    ViewIndividualReturnViewModel(
      chargeReference = chargeRef,
      hasVapingProductsDeclaration = hasDeclaration,
      amountProducedLiquid = amountProduced,
      dutyDue = dutyDueAmount,
      totalDutySpoiltProducts = totalDutySpoiltProducts,
      totalDutyDue = totalDuty,
      monthYear = monthYearString,
      submittedOn = submittedOnString,
      dutyRate = currencyFormat(dutyRate.getOrElse(zeroValue)),
      nilReturn = isNilReturn,
      declarationDetails = success.declaration,
      spoiltProduct = success.spoiltProduct,
      dutySuspenseSummaryList = dutySuspenseSummary
    )
  }
}
