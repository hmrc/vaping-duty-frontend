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

package viewmodels.returns.submit

import base.SpecBase
import models.returns.SpoiltVolumeByPeriod
import utils.ReturnsDateUtils

class SelectSpoiltPeriodViewModelSpec extends SpecBase {

  private val returnsDateUtils = new ReturnsDateUtils(clock)

  "SelectSpoiltPeriodViewModel" - {

    "must create view model with available periods" in {
      val obligationDetails = obligations(Seq(
        fulfilledObligation(october2027),
        fulfilledObligation(december2027)
      )).map(_.obligationDetails)

      val vm = SelectSpoiltPeriodViewModel(
        obligationDetails,
        None,
        periodKey,
        None,
        returnsDateUtils
      )

      vm.periods.size mustBe 2
      vm.currentYear mustBe 2027
    }

    "must filter out current return period" in {
      val obligationDetails = obligations(Seq(
        fulfilledObligation(periodKey),
        fulfilledObligation(october2027)
      )).map(_.obligationDetails)

      val vm = SelectSpoiltPeriodViewModel(
        obligationDetails,
        None,
        periodKey,
        None,
        returnsDateUtils
      )

      vm.periods.size mustBe 1
      vm.periods.head.href.value must include(october2027.value)
    }

    "must filter out periods already in spoilt list" in {
      val spoiltEntry = SpoiltVolumeByPeriod(volume = BigDecimal("100.0"), periodKey = october2027)
      val spoiltList = List(spoiltEntry)

      val obligationDetails = obligations(Seq(
        fulfilledObligation(october2027),
        fulfilledObligation(december2027)
      )).map(_.obligationDetails)

      val vm = SelectSpoiltPeriodViewModel(
        obligationDetails,
        None,
        periodKey,
        Some(spoiltList),
        returnsDateUtils
      )

      vm.periods.size mustBe 1
      vm.periods.head.href.value must include(december2027.value)
    }

    "must handle empty obligations" in {
      val obligationDetails = Seq.empty

      val vm = SelectSpoiltPeriodViewModel(
        obligationDetails,
        None,
        periodKey,
        None,
        returnsDateUtils
      )

      vm.periods mustBe empty
      vm.paginationItems mustBe empty
    }
  }
}
