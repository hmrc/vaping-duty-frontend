/*
 * Copyright 2024 HM Revenue & Customs
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

package repositories

import com.codahale.metrics.MetricRegistry
import com.mongodb.client.model.{Filters, Sorts}
import config.FrontendAppConfig
import crypto.CryptoProvider
import models.nrs.NrsSubmissionWorkItem
import org.bson.types.ObjectId
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem, WorkItemFields, WorkItemRepository}

import java.time.{Clock, Duration, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NrsWorkItemRepository @Inject()(
                                       mongoComponent: MongoComponent,
                                       appConfig: FrontendAppConfig,
                                       clock: Clock,
                                       cryptoProvider: CryptoProvider,
                                       metricRegistry: MetricRegistry
                                     )(using ec: ExecutionContext)
  extends WorkItemRepository[NrsSubmissionWorkItem](
    collectionName = "nrs-work-items",
    mongoComponent = mongoComponent,
    itemFormat = NrsSubmissionWorkItem.mongoFormat(cryptoProvider.getCrypto),
    workItemFields = WorkItemFields.default
  ) with Logging {

  private val retryCounter = metricRegistry.counter("nrs-work-items.retry")

  override def now(): Instant = Instant.now(clock)

  override lazy val inProgressRetryAfter: Duration =
    Duration.ofMillis(appConfig.nrsWorkItemRetryAfter.toMillis)

  override def completeAndDelete(id: ObjectId): Future[Boolean] =
    super.completeAndDelete(id)

  override lazy val requiresTtlIndex: Boolean = true

  override def pullOutstanding(failedBefore: Instant, availableAt: Instant): Future[Option[WorkItem[NrsSubmissionWorkItem]]] =
    super.pullOutstanding(failedBefore, availableAt)

  override def markAs(id: ObjectId, status: ProcessingStatus, availableAt: Option[Instant] = None): Future[Boolean] = {
    status match {
      case ProcessingStatus.Failed =>
        collection.find(Filters.eq("_id", id))
          .headOption()
          .flatMap {
            case Some(workItem) =>
              val failureCount = workItem.failureCount + 1
              val maxBackoffSteps = appConfig.nrsWorkItemMaxRetries

              // Calculate delay with exponential backoff, but cap at maxBackoffSteps
              val baseDelayMinutes = appConfig.nrsWorkItemRetryAfter.toMinutes
              val backoffFactor = appConfig.nrsWorkItemExponentialBackoffFactor

              // Cap the effective failure count to prevent delay from growing beyond max retries
              val numBackoffSteps = Math.min(failureCount, maxBackoffSteps)
              val delayMinutes = (baseDelayMinutes * Math.pow(backoffFactor, numBackoffSteps - 1)).toLong
              val nextAvailableAt = now().plus(Duration.ofMinutes(delayMinutes))

              retryCounter.inc()

              if (failureCount >= maxBackoffSteps) {
                logger.info(
                  s"NRS work item $id has reached max retries ($failureCount), " +
                  s"continuing with capped delay of $delayMinutes minutes"
                )
              } else {
                logger.info(
                  s"NRS work item $id failed (attempt $failureCount/$maxBackoffSteps), " +
                  s"retrying in $delayMinutes minutes"
                )
              }

              // Always mark as Failed (never PermanentlyFailed) to allow continued retries
              super.markAs(id, status, Some(nextAvailableAt))

            case None =>
              logger.warn(s"NRS work item $id not found when marking as Failed, using default behavior")
              super.markAs(id, status, availableAt)
          }

      case _ =>
        super.markAs(id, status, availableAt)
    }
  }

  def maxFailureCount(): Future[Int] =
    collection
      .find(Filters.in(workItemFields.status, ProcessingStatus.ToDo.name, ProcessingStatus.InProgress.name, ProcessingStatus.Failed.name))
      .sort(Sorts.descending(workItemFields.failureCount))
      .limit(1)
      .headOption()
      .map(_.fold(0)(_.failureCount))
}
