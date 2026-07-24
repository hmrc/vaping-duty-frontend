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

package viewmodels.returns.submit.adjustments

import base.SpecBase
import models.identifiers.PeriodKey
import models.returns.DutyRate
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

class AdjustmentCheckYourAnswersViewModelSpec extends SpecBase {

  val dutyRate = DutyRate(ratePencePer10Ml = 3000)
  val dutyRatesMap: Map[PeriodKey, DutyRate] = Map(october2027 -> dutyRate)

  "AdjustmentCheckYourAnswersViewModel" - {
    
    "must create view model with correct summary cards for under declared adjustment" in {
      val adjustment = AdjustmentEntry(
        period = october2027,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal("100.0")
      )
      val adjustmentList = AdjustmentList(Seq(adjustment))
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      val vm = AdjustmentCheckYourAnswersViewModel(
        Some(true),
        Some(adjustmentList),
        obligationDetails,
        periodKey,
        dutyRatesMap,
        returnsDateUtils
      )

      vm.hasAdjustments mustBe true
      vm.summaryCards.size mustBe 1
      vm.summaryCards.head.rows.size mustBe 4
    }

    "must build a remove link pointing at RemoveAdjustmentController for each summary card" in {
      val adjustment = AdjustmentEntry(
        period = october2027,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal("100.0")
      )
      val adjustmentList = AdjustmentList(Seq(adjustment))
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      val vm = AdjustmentCheckYourAnswersViewModel(
        Some(true),
        Some(adjustmentList),
        obligationDetails,
        periodKey,
        dutyRatesMap,
        returnsDateUtils
      )

      val expectedHref = controllers.returns.submit.adjustments.routes.RemoveAdjustmentController.onPageLoad(models.NormalMode).url +
        s"?period=${periodKey.value}&adjustmentPeriod=${october2027.value}"

      val removeAction = vm.summaryCards.head.card.actions.value.items.find(_.content == Text("Remove")).value
      removeAction.href mustBe expectedHref
    }

    "must calculate correct total adjustment for under declared" in {
      val adjustment = AdjustmentEntry(
        period = october2027,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal("1000.0")
      )
      val adjustmentList = AdjustmentList(Seq(adjustment))
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      val vm = AdjustmentCheckYourAnswersViewModel(
        Some(true),
        Some(adjustmentList),
        obligationDetails,
        periodKey,
        dutyRatesMap,
        returnsDateUtils
      )

      vm.totalAdjustment mustBe BigDecimal("3000.00")
      vm.formattedTotalAdjustment mustBe "£3,000"
    }

    "must calculate correct total adjustment for over declared (negative)" in {
      val adjustment = AdjustmentEntry(
        period = october2027,
        adjustmentType = AdjustmentType.OverDeclared,
        volumeInMl = BigDecimal("1000.0")
      )
      val adjustmentList = AdjustmentList(Seq(adjustment))
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      val vm = AdjustmentCheckYourAnswersViewModel(
        Some(true),
        Some(adjustmentList),
        obligationDetails,
        periodKey,
        dutyRatesMap,
        returnsDateUtils
      )

      vm.totalAdjustment mustBe BigDecimal("-3000.00")
      vm.formattedTotalAdjustment mustBe "-£3,000"
    }

    "must handle empty adjustment list" in {
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      val vm = AdjustmentCheckYourAnswersViewModel(
        Some(false),
        None,
        obligationDetails,
        periodKey,
        dutyRatesMap,
        returnsDateUtils
      )

      vm.hasAdjustments mustBe false
      vm.summaryCards.size mustBe 1
      vm.summaryCards.head.rows.size mustBe 1
      vm.totalAdjustment mustBe BigDecimal("0")
      vm.adjustmentReasonMandatory mustBe false
    }

    "must set adjustmentReasonMandatory when the under-declared duty total alone meets the £1000 threshold" in {
      val oneRatePerMl: Map[PeriodKey, DutyRate] = Map(october2027 -> DutyRate(1000))
      val adjustmentList = AdjustmentList(Seq(
        AdjustmentEntry(period = october2027, adjustmentType = AdjustmentType.UnderDeclared, volumeInMl = BigDecimal("1000")),
        AdjustmentEntry(period = october2027, adjustmentType = AdjustmentType.OverDeclared, volumeInMl = BigDecimal("500"))
      ))
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      val vm = AdjustmentCheckYourAnswersViewModel(
        Some(true),
        Some(adjustmentList),
        obligationDetails,
        periodKey,
        oneRatePerMl,
        returnsDateUtils
      )

      vm.adjustmentReasonMandatory mustBe true
    }

    "must not set adjustmentReasonMandatory when under-declared and over-declared duty totals are each below £1000, even combined they would exceed it" in {
      val oneRatePerMl: Map[PeriodKey, DutyRate] = Map(october2027 -> DutyRate(100))
      val adjustmentList = AdjustmentList(Seq(
        AdjustmentEntry(period = october2027, adjustmentType = AdjustmentType.UnderDeclared, volumeInMl = BigDecimal("800")),
        AdjustmentEntry(period = october2027, adjustmentType = AdjustmentType.OverDeclared, volumeInMl = BigDecimal("800"))
      ))
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      val vm = AdjustmentCheckYourAnswersViewModel(
        Some(true),
        Some(adjustmentList),
        obligationDetails,
        periodKey,
        oneRatePerMl,
        returnsDateUtils
      )

      vm.adjustmentReasonMandatory mustBe false
    }
  }
}
