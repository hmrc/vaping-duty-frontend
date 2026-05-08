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

import config.CurrencyFormatter
import models.returns.view.*
import play.api.i18n.Messages
import utils.ReturnsDateUtils.*

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneId}

case class ViewIndividualReturnViewModel(
  chargeReference: String,
  hasVapingProductsDeclaration: Boolean,
  amountProducedLiquid: Option[BigDecimal],
  dutyDue: Option[String],
  totalDutyDueVapingProducts: String,
  totalDutyDue: String,
  month: String,
  submittedOn: String
)

object ViewIndividualReturnViewModel extends CurrencyFormatter {
  
  def apply(returnsData: ReturnDisplayResponse)(using messages: Messages): ViewIndividualReturnViewModel = {
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
        (Some(regularReturn.amountProducedLiquid), Some(currencyFormat(regularReturn.dutyDue)))
      case _ =>
        (None, None)
    }
    
    val totalDutyDueVaping = success.totalDutyDue
      .fold(currencyFormat(BigDecimal(0)))(td => currencyFormat(td.totalDutyDueVapingProducts))
    
    val totalDuty = success.totalDutyDue
      .fold(currencyFormat(BigDecimal(0)))(td => currencyFormat(td.totalDutyDue))

    val periodFrom = returnsData.success.chargeDetails.get.periodFrom
    val monthYear = s"${getCurrentMonthMessage(periodFrom.getMonth)} ${periodFrom.getYear}"

    val receiptDate = returnsData.success.chargeDetails.get.receiptDate

    val receiptTime = LocalDateTime.ofInstant(receiptDate, ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM uuuu hh:mm"))
    
    ViewIndividualReturnViewModel(
      chargeReference = chargeRef,
      hasVapingProductsDeclaration = hasDeclaration,
      amountProducedLiquid = amountProduced,
      dutyDue = dutyDueAmount,
      totalDutyDueVapingProducts = totalDutyDueVaping,
      totalDutyDue = totalDuty,
      month = monthYear,
      submittedOn = receiptTime
    )
  }
}
