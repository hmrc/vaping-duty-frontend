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

package services.nrs

import cats.implicits.*
import com.codahale.metrics.{Gauge, MetricRegistry}
import play.api.Logging
import repositories.NrsWorkItemRepository
import uk.gov.hmrc.mongo.workitem.ProcessingStatus

import java.util.concurrent.atomic.AtomicLong
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsWorkItemMetrics @Inject()(
                                    nrsWorkItemRepository: NrsWorkItemRepository,
                                    metricRegistry: MetricRegistry
                                  )(using ec: ExecutionContext) extends Logging {

  private val prefix = "nrs-work-items"

  private val statuses: Seq[ProcessingStatus] = Seq(
    ProcessingStatus.ToDo,
    ProcessingStatus.InProgress,
    ProcessingStatus.Failed,
    ProcessingStatus.PermanentlyFailed,
    ProcessingStatus.Succeeded
  )

  private val statusCounts: Map[ProcessingStatus, AtomicLong] =
    statuses.map(status => status -> registerGauge(status.name)).toMap

  private val maxFailureCount: AtomicLong = registerGauge("max-failure-count")

  private def registerGauge(name: String): AtomicLong = {
    val value = new AtomicLong(0)
    // gauge(name, supplier) returns the existing gauge if already registered, so re-binding in tests is safe
    metricRegistry.gauge(s"$prefix.$name", () => (() => value.get()): Gauge[Long])
    value
  }

  def refresh(): Future[Unit] =
    (
      statuses.traverse(status => nrsWorkItemRepository.count(status).map(status -> _)),
      nrsWorkItemRepository.maxFailureCount()
    ).mapN { (counts, maxFailures) =>
      counts.foreach { case (status, count) => statusCounts(status).set(count) }
      maxFailureCount.set(maxFailures.toLong)
    }.recover { case ex =>
      logger.warn("Failed to refresh NRS work item metrics", ex)
    }
}
