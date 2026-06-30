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

package builders

import models.identifiers.PeriodKey
import models.obligations.{ObligationDetails, ObligationItem, ObligationStatus}

import java.time.LocalDate

trait ObligationsBuilders {

  val october2026  = PeriodKey("26AJ")
  val november2026 = PeriodKey("26AK")
  val december2026 = PeriodKey("26AL")
  val october2027  = PeriodKey("27AJ")
  val november2027 = PeriodKey("27AK")
  val december2027 = PeriodKey("27AL")

  // Synonyms for obligations in terms of returns
  def returns(returns: Seq[ObligationDetails]): Seq[ObligationItem] = obligations(returns)
  def completedReturn(periodKey: PeriodKey): ObligationDetails = fulfilledObligation(periodKey)
  def outstandingReturn(periodKey: PeriodKey): ObligationDetails = openObligation(periodKey)

  def obligations(obligations: Seq[ObligationDetails]): Seq[ObligationItem] = {
    obligations.map(obligationDetails =>
      ObligationItem(
        identification = None,
        obligationDetails = obligationDetails)
    )
  }
  def fulfilledObligation(periodKey: PeriodKey): ObligationDetails = obligationDetails(periodKey, ObligationStatus.F)
  def openObligation(periodKey: PeriodKey): ObligationDetails      = obligationDetails(periodKey, ObligationStatus.O)

  private def obligationDetails(periodKey: PeriodKey,
                                obligationStatus: ObligationStatus,
                                submittedOnTime: Boolean = false) = {
    ObligationDetails(
      openOrFulfilledStatus = obligationStatus.toString,
      iCFromDate            = firstDayOf(periodKey),
      iCToDate              = lastDayOf(periodKey),
      iCDateReceived        = dateReceived(periodKey, obligationStatus, submittedOnTime),
      iCDueDate             = dueDate(periodKey),
      periodKey             = periodKey.value
    )
  }

  private def firstDayOf(periodKey: PeriodKey) = LocalDate.of(year(periodKey), month(periodKey), 1)
  private def lastDayOf(periodKey: PeriodKey)  = firstDayOf(periodKey).plusMonths(1).minusDays(1)
  private def dueDate(periodKey: PeriodKey)    = firstDayOf(periodKey).plusMonths(1).plusDays(6)

  private def dateReceived(periodKey: PeriodKey,
                           obligationStatus: ObligationStatus,
                           submittedOnTime: Boolean) =
    obligationStatus match {
      case ObligationStatus.F => Some(
        submittedOnTime match {
          case true  => receivedBeforeDueDate(periodKey)
          case false => receivedAfterDueDate(periodKey)
        })
      case ObligationStatus.O => None
    }
  private def receivedBeforeDueDate(october2027: PeriodKey) = dueDate(october2027).minusDays(1)
  private def receivedAfterDueDate(october2027: PeriodKey)  = dueDate(october2027).plusDays(1)

  def month(periodKey: PeriodKey): Int = (periodKey.value.last - 'A') + 1
  def year(periodKey: PeriodKey): Int = ("20" + periodKey.value.take(2)).toInt

}
