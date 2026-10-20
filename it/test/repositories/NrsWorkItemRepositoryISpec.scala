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
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.model.{Filters, Updates}
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialStrength}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus
import base.ISpecBase
import config.FrontendAppConfig
import models.nrs.{IdentityData, NrsMetadata, NrsPayload, NrsSubmissionWorkItem}

import java.time.{Duration, Instant}
import scala.concurrent.Future

class NrsWorkItemRepositoryISpec extends ISpecBase with BeforeAndAfterEach {

  // metrics are disabled by default in tests, which swaps in a no-op MetricRegistry
  override def fakeApplication(): Application =
    applicationBuilder().configure("metrics.enabled" -> true).build()

  private val repository = app.injector.instanceOf[NrsWorkItemRepository]
  private val retryCounter = app.injector.instanceOf[MetricRegistry].counter("nrs-work-items.retry")

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.collection.deleteMany(Filters.empty()).toFuture())
  }

  private val testNrsPayload = NrsPayload(
    payload = "encodedPayload",
    metadata = NrsMetadata(
      businessId = "vpd",
      notableEvent = "vaping-duty-return-submitted",
      payloadContentType = "application/json",
      payloadSha256Checksum = "checksum123",
      userSubmissionTimestamp = Instant.now(clock).toString,
      identityData = IdentityData(
        internalId = Some("Int-123"),
        optionalCredentials = None,
        confidenceLevel = ConfidenceLevel.L50,
        groupIdentifier = None,
        credentialRole = None,
        affinityGroup = Some(AffinityGroup.Organisation),
        credentialStrength = Some(CredentialStrength.strong)
      ),
      userAuthToken = "Bearer token123",
      headerData = Map.empty[String, String],
      searchKeys = Map("approvalId" -> "XMVPD0000000123", "periodKey" -> "24AF")
    )
  )

  private val testWorkItem = NrsSubmissionWorkItem(testNrsPayload)

  "NrsWorkItemRepository must" - {
    "successfully push a new work item" in {
      val result = await(repository.pushNew(testWorkItem, Instant.now(clock), _ => ProcessingStatus.ToDo))

      result.item mustBe testWorkItem
      result.status mustBe ProcessingStatus.ToDo
    }

    "pull an outstanding work item" in {
      await(repository.pushNew(testWorkItem, Instant.now(clock).minusSeconds(120), _ => ProcessingStatus.ToDo))

      val result = await(repository.pullOutstanding(Instant.now(clock).minusSeconds(60), Instant.now(clock)))

      result.isDefined mustBe true
      result.get.item mustBe testWorkItem
      result.get.status mustBe ProcessingStatus.InProgress
    }

    "return None when no outstanding work items exist" in {
      val result = await(repository.pullOutstanding(Instant.now(clock).minusSeconds(60), Instant.now(clock)))

      result.isDefined mustBe false
    }

    "mark a work item as complete" in {
      val workItem = await(repository.pushNew(testWorkItem, Instant.now(clock), _ => ProcessingStatus.ToDo))

      await(repository.complete(workItem.id, ProcessingStatus.Succeeded))

      val retrieved = await(repository.pullOutstanding(Instant.now(clock).minusSeconds(60), Instant.now(clock)))
      retrieved mustBe None
    }

    "mark a work item as failed" in {
      val workItem = await(repository.pushNew(testWorkItem, Instant.now(clock), _ => ProcessingStatus.ToDo))

      val result = await(repository.markAs(workItem.id, ProcessingStatus.Failed, Some(Instant.now(clock).plusSeconds(60))))

      result mustBe true
    }

    "complete and delete a work item" in {
      val workItem = await(repository.pushNew(testWorkItem, Instant.now(clock), _ => ProcessingStatus.ToDo))

      await(repository.completeAndDelete(workItem.id))

      val result = await(repository.pullOutstanding(Instant.now(clock).minusSeconds(60), Instant.now(clock)))
      result.isDefined mustBe false
    }

    "handle multiple work items" in {
      val workItem1 = testWorkItem
      val workItem2 = testWorkItem.copy(payload = testNrsPayload.copy(payload = "encodedPayload2"))

      await(repository.pushNew(workItem1, Instant.now(clock).minusSeconds(120), _ => ProcessingStatus.ToDo))
      await(repository.pushNew(workItem2, Instant.now(clock).minusSeconds(120), _ => ProcessingStatus.ToDo))

      val result1 = await(repository.pullOutstanding(Instant.now(clock).minusSeconds(60), Instant.now(clock)))
      val result2 = await(repository.pullOutstanding(Instant.now(clock).minusSeconds(60), Instant.now(clock)))

      result1.isDefined mustBe true
      result2.isDefined mustBe true
      result1.get.item must not be result2.get.item
    }

    "implement exponential backoff for failed items" in {
      val appConfig = app.injector.instanceOf[FrontendAppConfig]
      val workItem = await(repository.pushNew(testWorkItem, Instant.now().minusSeconds(1), _ => ProcessingStatus.ToDo))
      
      // Mark as failed for the first time
      await(repository.markAs(workItem.id, ProcessingStatus.Failed))
      
      val afterFirstFailure = await(repository.collection.find(Filters.eq("_id", workItem.id)).toFuture()).head
      afterFirstFailure.failureCount mustBe 1
      afterFirstFailure.status mustBe ProcessingStatus.Failed
      
      // The availableAt should be set to ~10 minutes in the future (base delay)
      val expectedDelay = appConfig.nrsWorkItemRetryAfter.toMinutes
      val actualDelayMinutes = Duration.between(
        Instant.now(),
        afterFirstFailure.availableAt
      ).toMinutes
      
      // Allow 1 minute tolerance for test execution time
      Math.abs(actualDelayMinutes - expectedDelay) must be <= 1L
    }

    "continue retrying indefinitely with capped delay after max retries" in {
      val appConfig = app.injector.instanceOf[FrontendAppConfig]
      val workItem = await(repository.pushNew(testWorkItem, Instant.now(clock).minusSeconds(1), _ => ProcessingStatus.ToDo))
      
      val maxRetries = appConfig.nrsWorkItemMaxRetries
      val baseDelayMinutes = appConfig.nrsWorkItemRetryAfter.toMinutes
      val backoffFactor = appConfig.nrsWorkItemExponentialBackoffFactor
      val maxDelayMinutes = (baseDelayMinutes * Math.pow(backoffFactor, maxRetries - 1)).toLong
      
      // Update the work item to have failureCount = maxRetries - 1
      await(repository.collection.updateOne(
        Filters.equal("_id", workItem.id),
        Updates.set("failureCount", maxRetries - 1)
      ).toFuture())
      
      // Mark as failed to reach max retries
      await(repository.markAs(workItem.id, ProcessingStatus.Failed))
      
      val atMaxRetries = await(repository.collection.find(Filters.equal("_id", workItem.id)).toFuture()).head
      atMaxRetries.failureCount mustBe maxRetries
      atMaxRetries.status mustBe ProcessingStatus.Failed
      
      // Continue failing beyond max retries - delay should be capped
      (1 to 3).foreach { extraAttempt =>
        val beforeMark = Instant.now()
        await(repository.markAs(atMaxRetries.id, ProcessingStatus.Failed))
        
        val item = await(repository.collection.find(Filters.equal("_id", workItem.id)).toFuture()).head
        item.status mustBe ProcessingStatus.Failed
        item.failureCount mustBe (maxRetries + extraAttempt)
        
        // Verify delay is capped at max delay
        val actualDelayMinutes = Duration.between(beforeMark, item.availableAt).toMinutes
        Math.abs(actualDelayMinutes - maxDelayMinutes) must be <= 1L
      }
    }

    "handle marking as failed when work item not found" in {
      val nonExistentId = new org.bson.types.ObjectId()
      
      // Attempt to mark a non-existent work item as failed
      // This should use default behavior and return false
      val result = await(repository.markAs(nonExistentId, ProcessingStatus.Failed))
      
      // The operation should complete without error (uses default behavior)
      result mustBe false
    }

    "use default behavior for non-Failed statuses" in {
      val workItem = await(repository.pushNew(testWorkItem, Instant.now(clock).minusSeconds(1), _ => ProcessingStatus.ToDo))
      
      // Mark as InProgress (not Failed) - should use default parent behavior
      val result = await(repository.markAs(workItem.id, ProcessingStatus.InProgress))
      
      result mustBe true
      
      val updatedItem = await(repository.collection.find(Filters.equal("_id", workItem.id)).toFuture()).head
      updatedItem.status mustBe ProcessingStatus.InProgress
    }
    "increment the retry counter when a failed work item is scheduled for retry" in {
      val workItem = await(repository.pushNew(testWorkItem, Instant.now(clock).minusSeconds(1), _ => ProcessingStatus.ToDo))
      val before = retryCounter.getCount

      await(repository.markAs(workItem.id, ProcessingStatus.Failed))

      retryCounter.getCount mustBe before + 1
    }

    "increment the retry counter when a work item fails at or beyond max retries" in {
      val appConfig = app.injector.instanceOf[FrontendAppConfig]
      val workItem = await(repository.pushNew(testWorkItem, Instant.now(clock).minusSeconds(1), _ => ProcessingStatus.ToDo))
      await(repository.collection.updateOne(
        Filters.equal("_id", workItem.id),
        Updates.set("failureCount", appConfig.nrsWorkItemMaxRetries - 1)
      ).toFuture())
      val before = retryCounter.getCount

      await(repository.markAs(workItem.id, ProcessingStatus.Failed))
      retryCounter.getCount mustBe before + 1

      await(repository.markAs(workItem.id, ProcessingStatus.Failed))
      retryCounter.getCount mustBe before + 2
    }

    "not increment the retry counter when the failed work item is not found" in {
      val before = retryCounter.getCount

      await(repository.markAs(new org.bson.types.ObjectId(), ProcessingStatus.Failed))

      retryCounter.getCount mustBe before
    }

    "return 0 for maxFailureCount when there are no work items" in {
      await(repository.maxFailureCount()) mustBe 0
    }

    "return the highest failureCount of retryable work items, ignoring PermanentlyFailed and Succeeded" in {
      def pushWith(status: ProcessingStatus, failureCount: Int, payload: String) = {
        val item = await(repository.pushNew(
          testWorkItem.copy(payload = testNrsPayload.copy(payload = payload)),
          Instant.now(clock),
          _ => status
        ))
        await(repository.collection.updateOne(Filters.equal("_id", item.id), Updates.set("failureCount", failureCount)).toFuture())
      }

      pushWith(ProcessingStatus.ToDo, 0, "todo")
      pushWith(ProcessingStatus.Failed, 3, "failed")
      pushWith(ProcessingStatus.InProgress, 2, "in-progress")
      pushWith(ProcessingStatus.PermanentlyFailed, 9, "permanently-failed")
      pushWith(ProcessingStatus.Succeeded, 5, "succeeded")

      await(repository.maxFailureCount()) mustBe 3
    }
  }

  private def await[T](future: Future[T]): T = {
    scala.concurrent.Await.result(future, scala.concurrent.duration.Duration.Inf)
  }
}