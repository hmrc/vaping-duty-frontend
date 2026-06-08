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

import models.returns.ConvertToMl
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
                                          totalDutyDueVapingProducts: String,
                                          totalDutyDue: String,
                                          monthYear: String,
                                          submittedOn: String,
                                          dutyRate: String
                                        ) {

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

  def dutyTotalsSummaryList(implicit messages: Messages): SummaryList =
    SummaryList(
      rows = Seq(
        SummaryListRow(
          key = Key(
            content = Text(messages("viewIndividualReturn.totalDutyDueVapingProducts"))
          ),
          value = Value(
            content = Text(totalDutyDueVapingProducts)
          )
        ),
        SummaryListRow(
          key = Key(
            content = Text(messages("viewIndividualReturn.totalDutyDue"))
          ),
          value = Value(
            content = Text(totalDutyDue)
          )
        )
      )
    )
}

object ViewIndividualReturnViewModel extends CurrencyFormatter {

  def apply(returnsData: ReturnDisplayResponse, dutyRate: BigDecimal)(using messages: Messages): ViewIndividualReturnViewModel = {
    val zeroValue = BigDecimal("0")
    val success = returnsData.success

    val chargeRef = success.chargeDetails
      .flatMap(_.chargeReference)
      .getOrElse("")

    val vapingProducts = success.vapingProductsProduced

    val hasDeclaration = vapingProducts
      .exists(vp => vp.regularReturn.nonEmpty)

    val (amountProduced, dutyDueAmount) = vapingProducts match {
      case Some(vp) if vp.regularReturn.nonEmpty =>
        val regularReturn = vp.regularReturn.head
        (Some(milliliterFormat(ConvertToMl(regularReturn.amountProducedLiquid).toMl)), Some(currencyFormat(regularReturn.dutyDue)))
      case _ =>
        (None, None)
    }

    val totalDutyDueVaping = success.totalDutyDue
      .fold(currencyFormat(zeroValue))(td => currencyFormat(td.totalDutyDueVapingProducts))

    val totalDuty = success.totalDutyDue
      .fold(currencyFormat(zeroValue))(td => currencyFormat(td.totalDutyDue))
    
    val year = success.chargeDetails.fold(LocalDate.now().getYear.toString)(_.periodFrom.getYear.toString)
    
    val monthFromLocalDate = success.chargeDetails.fold(LocalDate.now().getMonth)(_.periodFrom.getMonth)

    val receiptDate = success.chargeDetails.fold(Instant.now())(_.receiptDate)
    val submittedOn = LocalDateTime.ofInstant(receiptDate, ZoneId.systemDefault())
    val submittedOnDay = submittedOn.getDayOfMonth
    val submittedOnMonth = PeriodKeys.toDisplayName(submittedOn.getMonth)

    val receiptTime = LocalDateTime.ofInstant(receiptDate, ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("hh:mma"))

    val monthYearString = s"${getMonthMessage(monthFromLocalDate)} $year"
    val submittedOnString = s"$submittedOnDay $submittedOnMonth $year ${messages("viewIndividualReturn.chargeDetails.at")} $receiptTime"

    ViewIndividualReturnViewModel(
      chargeReference = chargeRef,
      hasVapingProductsDeclaration = hasDeclaration,
      amountProducedLiquid = amountProduced,
      dutyDue = dutyDueAmount,
      totalDutyDueVapingProducts = totalDutyDueVaping,
      totalDutyDue = totalDuty,
      monthYear = monthYearString,
      submittedOn = submittedOnString,
      dutyRate = currencyFormat(dutyRate)
    )
  }
}
