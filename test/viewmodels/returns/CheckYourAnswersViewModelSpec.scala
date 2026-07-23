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

package viewmodels.returns

import base.SpecBase
import models.returns.DutyRate
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import models.returns.SpoiltVolumeByPeriod
import pages.returns.adjustments.{AdjustmentListPage, AdjustmentReasonPage, DeclareAdjustmentPage}
import pages.returns.{DeclareDutyPage, DeclareDutySuspensePage, DeclareSpoiltProductsPage, EnterDutyAmountPage, SpoiltVolumeByPeriodPage}
import play.api.i18n.Messages
import utils.{CurrencyFormatter, ReturnsDateUtils}
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}
import pages.returns.{DeclareDutyPage, DeclareDutySuspensePage, DeclareSpoiltProductsPage, EnterDutyAmountPage}
import pages.returns.adjustments.AdjustmentListPage
import utils.ReturnsDateUtils
import viewmodels.returns.submit.CheckYourAnswersViewModel

class CheckYourAnswersViewModelSpec extends SpecBase with CurrencyFormatter {

  private val returnsDateUtils = new ReturnsDateUtils(clock)
  implicit val messages: Messages = messages(applicationBuilder(None).build())

  "CheckYourAnswersViewModel" - {

    "must identify a nil return when no duty, spoilt products, or adjustments are declared" in {
      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, false).success.value
        .set(DeclareSpoiltProductsPage, false).success.value
        .set(DeclareAdjustmentPage, false).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, dutyRates, periodKey, returnsDateUtils)

      vm.nilReturn mustBe true
    }

    "must not identify a nil return when duty is declared" in {
      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, true).success.value
        .set(EnterDutyAmountPage, BigDecimal("1000")).success.value
        .set(DeclareSpoiltProductsPage, false).success.value
        .set(DeclareAdjustmentPage, false).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, dutyRates, periodKey, returnsDateUtils)

      vm.nilReturn mustBe false
    }

    "must not identify a nil return when spoilt products are declared" in {
      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, false).success.value
        .set(DeclareSpoiltProductsPage, true).success.value
        .set(DeclareAdjustmentPage, false).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, DutyRate(315), periodKey, returnsDateUtils)

      vm.nilReturn mustBe false
    }

    "must not identify a nil return when adjustments are declared" in {
      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, false).success.value
        .set(DeclareSpoiltProductsPage, false).success.value
        .set(DeclareAdjustmentPage, true).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, DutyRate(315), periodKey, returnsDateUtils)

      vm.nilReturn mustBe false
    }

    "must calculate total duty correctly with declare duty only" in {
      val volumeInMl = BigDecimal("1000")
      val dutyRate = DutyRate(315)
      val expectedDuty = dutyRate.calculateDuty(volumeInMl)

      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, true).success.value
        .set(EnterDutyAmountPage, volumeInMl).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, dutyRate, periodKey, returnsDateUtils)

      vm.totalDuty mustBe expectedDuty
      vm.formattedTotalDuty mustBe currencyFormat(expectedDuty)
    }

    "must calculate total duty correctly with declare duty and spoilt products" in {
      val volumeInMl = BigDecimal("1000")
      val spoiltVolume = BigDecimal("100")
      val dutyRate = DutyRate(315)
      val declareDuty = dutyRate.calculateDuty(volumeInMl)
      val spoiltDuty = dutyRate.calculateDuty(spoiltVolume)
      val expectedTotal = declareDuty - spoiltDuty

      val spoiltVolumes = List(SpoiltVolumeByPeriod(spoiltVolume, periodKey))

      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, true).success.value
        .set(EnterDutyAmountPage, volumeInMl).success.value
        .set(DeclareSpoiltProductsPage, true).success.value
        .set(SpoiltVolumeByPeriodPage, spoiltVolumes).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, dutyRate, periodKey, returnsDateUtils)

      vm.totalDuty mustBe expectedTotal
      vm.formattedTotalDuty mustBe currencyFormat(expectedTotal)
    }

    "must calculate total duty correctly with declare duty and adjustments" in {
      val volumeInMl = BigDecimal("1000")
      val underDeclaredVolume = BigDecimal("200")
      val overDeclaredVolume = BigDecimal("50")
      val dutyRate = DutyRate(315)

      val declareDuty = dutyRate.calculateDuty(volumeInMl)
      val underDeclaredDuty = dutyRate.calculateDuty(underDeclaredVolume)
      val overDeclaredDuty = dutyRate.calculateDuty(overDeclaredVolume)
      val expectedTotal = declareDuty + underDeclaredDuty - overDeclaredDuty

      val adjustmentList = AdjustmentList(Seq(
        AdjustmentEntry(periodKey, AdjustmentType.UnderDeclared, underDeclaredVolume),
        AdjustmentEntry(periodKey, AdjustmentType.OverDeclared, overDeclaredVolume)
      ))

      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, true).success.value
        .set(EnterDutyAmountPage, volumeInMl).success.value
        .set(DeclareAdjustmentPage, true).success.value
        .set(AdjustmentListPage, adjustmentList).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, dutyRate, periodKey, returnsDateUtils)

      vm.totalDuty mustBe expectedTotal
      vm.formattedTotalDuty mustBe currencyFormat(expectedTotal)
    }

    "must build declare duty card correctly" in {
      val volumeInMl = BigDecimal("1000")
      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, true).success.value
        .set(EnterDutyAmountPage, volumeInMl).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, DutyRate(315), periodKey, returnsDateUtils)

      val (title, summaryList, actions) = vm.declareDutyCard

      title mustBe messages("returns.CheckYourAnswers.card.declareDuty.title")
      summaryList.rows.size mustBe 2
      actions mustBe defined
      actions.get.size mustBe 1
    }

    "must build spoilt products card correctly when declared" in {
      val spoiltVolume = BigDecimal("100")
      val spoiltVolumes = List(SpoiltVolumeByPeriod(spoiltVolume, periodKey))

      val userAnswers = returnsUserAnswers
        .set(DeclareSpoiltProductsPage, true).success.value
        .set(SpoiltVolumeByPeriodPage, spoiltVolumes).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, DutyRate(315), periodKey, returnsDateUtils)

      val (title, summaryList, actions) = vm.spoiltProductsCard

      title mustBe messages("returns.CheckYourAnswers.card.spoilt.title")
      summaryList.rows.size mustBe 2
      actions mustBe defined
    }

    "must build adjustments card correctly with reason" in {
      val adjustmentList = AdjustmentList(Seq(
        AdjustmentEntry(periodKey, AdjustmentType.UnderDeclared, BigDecimal("200"))
      ))

      val userAnswers = returnsUserAnswers
        .set(DeclareAdjustmentPage, true).success.value
        .set(AdjustmentListPage, adjustmentList).success.value
        .set(AdjustmentReasonPage, "Test reason").success.value

      val vm = CheckYourAnswersViewModel(userAnswers, DutyRate(315), periodKey, returnsDateUtils)

      val (title, summaryList, actions) = vm.adjustmentsCard

      title mustBe messages("returns.CheckYourAnswers.card.adjustments.title")
      summaryList.rows.size mustBe 3 // question + total + reason
      actions mustBe defined

      // Verify reason row has its own change link
      val reasonRow = summaryList.rows.last
      reasonRow.actions mustBe defined
    }

    "must build duty suspended card when declared" in {
      val userAnswers = returnsUserAnswers
        .set(DeclareDutySuspensePage, true).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, DutyRate(315), periodKey, returnsDateUtils)

      vm.hasDutySuspended mustBe true
      vm.dutySuspendedCard mustBe defined

      val (title, summaryList, actions) = vm.dutySuspendedCard.get

      title mustBe messages("returns.CheckYourAnswers.card.dutySuspended.title")
      summaryList.rows.size mustBe 2
      actions mustBe defined
    }

    "must not build duty suspended card when not declared" in {
      val userAnswers = returnsUserAnswers
        .set(DeclareDutySuspensePage, false).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, dutyRates, periodKey, returnsDateUtils)

      vm.hasDutySuspended mustBe false
      vm.dutySuspendedCard mustBe None
    }

    "must show total due when DeclareDutyPage is false but adjustments exist" in {
      val adjustmentList = AdjustmentList(Seq(
        AdjustmentEntry(
          period = periodKey,
          adjustmentType = AdjustmentType.UnderDeclared,
          volumeInMl = BigDecimal(500)
        )
      ))

      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, false).success.value
        .set(DeclareSpoiltProductsPage, false).success.value
        .set(DeclareDutySuspensePage, false).success.value
        .set(AdjustmentListPage, adjustmentList).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, dutyRates, periodKey, returnsDateUtils)

      vm.dutyDue mustBe "£157.50"
      vm.nilReturn mustBe false
    }

    "must show negative total due when adjustments result in negative value" in {
      val adjustmentList = AdjustmentList(Seq(
        AdjustmentEntry(
          period = periodKey,
          adjustmentType = AdjustmentType.OverDeclared,
          volumeInMl = BigDecimal(500)
        )
      ))

      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, false).success.value
        .set(AdjustmentListPage, adjustmentList).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, dutyRates, periodKey, returnsDateUtils)

      vm.dutyDue mustBe "-£157.50"
    }

    "must not show total due row when DeclareDutyPage is false and no adjustments" in {
      val userAnswers = returnsUserAnswers
        .set(DeclareDutyPage, false).success.value
        .set(DeclareSpoiltProductsPage, false).success.value
        .set(DeclareDutySuspensePage, false).success.value

      val vm = CheckYourAnswersViewModel(userAnswers, dutyRates, periodKey, returnsDateUtils)

      vm.dutyDue mustBe "£0"
      vm.nilReturn mustBe true
    }
  }
}