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

package forms.returns

import forms.mappings.Mappings
import models.returns.DutySuspenseVolumes
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class EnterDutySuspenseFormProvider @Inject() extends Mappings {

  private val VOLUME_RECEIVED_FIELD = "volumeReceived"
  private val VOLUME_MOVED_FIELD = "volumeMoved"
  private val ZERO = 0

  def apply(): Form[DutySuspenseVolumes] =
    Form(
      mapping(
        VOLUME_RECEIVED_FIELD -> int(
          "returns.enterDutySuspense.volumeReceived.error.required",
          "returns.enterDutySuspense.volumeReceived.error.wholeNumber",
          "returns.enterDutySuspense.volumeReceived.error.nonNumeric")
            .verifying(inRange(ZERO, Int.MaxValue, "returns.enterDutySuspense.volumeReceived.error.outOfRange")),

        VOLUME_MOVED_FIELD -> int(
          "returns.enterDutySuspense.volumeMoved.error.required",
          "returns.enterDutySuspense.volumeMoved.error.wholeNumber",
          "returns.enterDutySuspense.volumeMoved.error.nonNumeric")
            .verifying(inRange(ZERO, Int.MaxValue, "returns.enterDutySuspense.volumeMoved.error.outOfRange"))
      )((received, moved) => DutySuspenseVolumes(received, moved))(o => Some((o.volumeReceived, o.volumeMoved)))
    )
}