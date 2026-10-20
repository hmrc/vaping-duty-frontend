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

package scheduling

import config.FrontendAppConfig
import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import services.nrs.{NrsService, NrsWorkItemMetrics}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

@Singleton
class ScheduleProcessor @Inject()(actorSystem: ActorSystem,
                                  nrsService: NrsService,
                                  nrsWorkItemMetrics: NrsWorkItemMetrics,
                                  appConfig: FrontendAppConfig)
                                 (using ec: ExecutionContext) extends Logging {

  private val initialDelay: FiniteDuration = appConfig.nrsSchedulerInitialDelay
  private val interval: FiniteDuration = appConfig.nrsSchedulerInterval

  def scheduleProcessing(): Unit =
    actorSystem.scheduler.scheduleAtFixedRate(
      initialDelay = initialDelay,
      interval = interval
    ) { () =>
      if (appConfig.nrsSubmissionEnabled) {
        logger.debug("NRS submission scheduler triggered")
        nrsService.processAll().recover { case ex =>
          logger.error("Error processing NRS work items", ex)
          ()
        }
      } else {
        logger.debug("NRS submission disabled - skipping scheduled processing")
      }
      nrsWorkItemMetrics.updateStatusCount()
      ()
    }
}