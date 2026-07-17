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
import models.returns.ConvertToMl
import models.returns.view.*
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import utils.{CurrencyFormatter, PeriodKeys, ReturnsDateUtils}

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

    val personalDetails = PersonalDetailsSectionBuilder(success.declaration).build()
    val dutyDeclaration = DutyDeclarationSectionBuilder(hasDeclaration, amountProduced, dutyDueAmount).build()
    val spoiltLists = SpoiltProductSectionBuilder(success.spoiltProduct, isNilReturn, totalDutySpoiltProducts, obligations, returnsDateUtils).build()
    val adjustmentsLists = AdjustmentsSectionBuilder(success.overDeclaration, success.underDeclaration, obligations, returnsDateUtils).build()
    val dutySuspenseSummary = DutySuspenseSectionBuilder(success.otherOptions).build()
    val totalDutySummary = TotalDutySectionBuilder(success.totalDutyDue).build()

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
}
