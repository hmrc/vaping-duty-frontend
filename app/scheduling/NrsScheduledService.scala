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

import config.FrontendAppConfig
import play.api.Logging

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.FiniteDuration

@Singleton
class NrsScheduledService @Inject()(
  appConfig: FrontendAppConfig,
  scheduleProcessor: ScheduleProcessor
) extends Logging {
  
  private val initialDelay: FiniteDuration = appConfig.nrsSchedulerInitialDelay
  private val interval: FiniteDuration = appConfig.nrsSchedulerInterval

  if (appConfig.nrsGenerationEnabled) {
    logger.info(
      s"NRS submission scheduler enabled - initial delay: $initialDelay, interval: $interval"
    )
    scheduleProcessor.scheduleProcessing()
  } else {
    logger.info("NRS submission scheduler disabled")
  }
}