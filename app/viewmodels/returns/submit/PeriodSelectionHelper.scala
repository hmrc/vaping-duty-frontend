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

import models.obligations.{Obligation, ObligationStatus}
import models.returns.ReturnsConstants
import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination.PaginationItem

import java.time.LocalDate

object PeriodSelectionHelper {

  def filterFulfilledWithinThreeYears(
    obligations: Seq[Obligation]
  ): Seq[Obligation] = {
    val currentDate = LocalDate.now()
    val threeYearsAgo = currentDate.minusYears(ReturnsConstants.YEARS_TO_SHOW)

    obligations
      .filter(_.openOrFulfilledStatus == ObligationStatus.F.toString)
      .filter(_.iCFromDate.isAfter(threeYearsAgo))
  }

  def extractAvailableYearsFromDetails(
    obligations: Seq[Obligation]
  ): Seq[Int] = {
    obligations
      .map(_.iCFromDate.getYear)
      .distinct
      .sorted(Ordering[Int].reverse)
  }

  def selectCurrentYear(
    availableYears: Seq[Int],
    selectedYear: Option[Int]
  ): Int = {
    selectedYear.getOrElse(
      availableYears.headOption.getOrElse(LocalDate.now().getYear)
    )
  }

  def filterObligationsByYearFromDetails(
    obligations: Seq[Obligation],
    year: Int
  ): Seq[Obligation] = {
    obligations
      .filter(_.iCFromDate.getYear == year)
      .sortBy(_.iCFromDate.getMonthValue)(Ordering[Int].reverse)
  }

  def buildPaginationItems(
    years: Seq[Int],
    currentYear: Int,
    hrefBuilder: Int => String
  ): Seq[PaginationItem] = {
    val paginationYears = years.take(ReturnsConstants.YEARS_TO_SHOW)

    paginationYears.map { year =>
      PaginationItem(
        href = hrefBuilder(year),
        number = Some(year.toString),
        visuallyHiddenText = Some(s"Page - year $year"),
        current = Some(year == currentYear)
      )
    }
  }
}