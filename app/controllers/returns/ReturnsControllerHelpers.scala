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

package controllers.returns

import models.identifiers.PeriodKey
import models.requests.returns.{ReturnsDataRequest, ReturnsOptionalDataRequest}
import play.api.mvc.{Call, Result, Results}

trait ReturnsControllerHelpers extends Results {
  
  /**
   * Redirect to a route while preserving the period query parameter
   */
  def redirectWithPeriod(call: Call)(implicit request: ReturnsOptionalDataRequest[?]): Result =
    Redirect(s"${call.url}?period=${request.periodKey.value}")

  def redirectWithPeriod(call: Call)(implicit request: ReturnsDataRequest[?]): Result =
    Redirect(s"${call.url}?period=${request.periodKey.value}")

  /**
   * Generate a URL with period query parameter
   */
  def urlWithPeriod(call: Call, periodKey: PeriodKey): String =
    s"${call.url}?period=${periodKey.value}"
}