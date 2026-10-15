/*
 * Copyright 2025 HM Revenue & Customs
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

package scheduling

import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import base.SpecBase
import config.FrontendAppConfig
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.duration.*

class NrsScheduledServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val mockScheduleProcessor: ScheduleProcessor = mock[ScheduleProcessor]
  private val mockAppConfig: FrontendAppConfig         = mock[FrontendAppConfig]

  override def beforeEach(): Unit = {
    reset(mockScheduleProcessor)
    super.beforeEach()
  }

  "NrsScheduledService" - {
    "when scheduler is enabled" - {
      "must call scheduleProcessor.scheduleProcessing()" in {
        when(mockAppConfig.nrsSchedulerInterval).thenReturn(30.seconds)
        when(mockAppConfig.nrsSchedulerInitialDelay).thenReturn(1.minute)
        when(mockAppConfig.nrsGenerationEnabled).thenReturn(true)

        new NrsScheduledService(mockAppConfig, mockScheduleProcessor)

        verify(mockScheduleProcessor).scheduleProcessing()
      }
    }

    "when scheduler is disabled" - {
      "must not call scheduleProcessor.scheduleProcessing()" in {
        when(mockAppConfig.nrsSchedulerInterval).thenReturn(30.seconds)
        when(mockAppConfig.nrsSchedulerInitialDelay).thenReturn(1.minute)
        when(mockAppConfig.nrsGenerationEnabled).thenReturn(false)

        new NrsScheduledService(mockAppConfig, mockScheduleProcessor)

        verify(mockScheduleProcessor, never()).scheduleProcessing()
      }
    }
  }
}
