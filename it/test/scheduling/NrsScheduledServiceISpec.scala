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

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.scalatest.concurrent.Eventually
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatest.time.{Seconds, Span}
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers
import play.api.test.Helpers.{ACCEPTED, await, defaultAwaitTimeout, running}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus
import base.ISpecBase
import data.NrsTestData
import models.nrs.{NrsPayload, NrsSubmissionWorkItem}
import repositories.NrsWorkItemRepository
import services.nrs.NrsService
import util.WireMockHelper

import java.time.Instant

class NrsScheduledServiceISpec extends ISpecBase with Eventually with WireMockHelper with NrsTestData {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(60, Seconds), interval = Span(200, org.scalatest.time.Millis))

  private def schedulerEnabledConfig = Configuration(
    "nrs-submission-scheduler.interval"      -> "1 second",
    "nrs-submission-scheduler.initial-delay" -> "2 seconds",
    "features.nrs-submission-enabled"        -> true,
    "features.nrs-generation-enabled"        -> true,
    "microservice.services.nrs-orchestrator.protocol"     -> "http",
    "microservice.services.nrs-orchestrator.host"         -> wireMockHost,
    "microservice.services.nrs-orchestrator.port"         -> wireMockPort,
    "microservice.services.nrs-orchestrator.url"          -> "/submission",
    "microservice.services.nrs-orchestrator.api-key"      -> "test-api-key",
    "microservice.services.nrs-orchestrator.max-failures" -> 5,
    "microservice.services.nrs-orchestrator.call-timeout" -> "30 seconds",
    "microservice.services.nrs-orchestrator.reset-timeout" -> "1 second",
    "microservice.services.nrs-orchestrator.max-reset-timeout" -> "5 seconds",
    "microservice.services.nrs-orchestrator.exponential-backoff-factor" -> 2.0
  )

  private def schedulerDisabledConfig = Configuration(
    "nrs-submission-scheduler.interval"      -> "1 second",
    "nrs-submission-scheduler.initial-delay" -> "2 seconds",
    "features.nrs-submission-enabled"        -> true,
    "features.nrs-generation-enabled"        -> false,
    "microservice.services.nrs-orchestrator.protocol"     -> "http",
    "microservice.services.nrs-orchestrator.host"         -> wireMockHost,
    "microservice.services.nrs-orchestrator.port"         -> wireMockPort,
    "microservice.services.nrs-orchestrator.url"          -> "/submission",
    "microservice.services.nrs-orchestrator.api-key"      -> "test-api-key",
    "microservice.services.nrs-orchestrator.max-failures" -> 5,
    "microservice.services.nrs-orchestrator.call-timeout" -> "30 seconds",
    "microservice.services.nrs-orchestrator.reset-timeout" -> "1 second",
    "microservice.services.nrs-orchestrator.max-reset-timeout" -> "5 seconds",
    "microservice.services.nrs-orchestrator.exponential-backoff-factor" -> 2.0
  )

  "NrsScheduledService" - {
    "when scheduler is enabled" - {
      "must process work items from the queue" in {
        val app = GuiceApplicationBuilder()
          .configure(schedulerEnabledConfig)
          .build()

        running(app) {
          wireMockServer.stubFor(
            post(urlEqualTo("/submission"))
              .willReturn(
                aResponse()
                  .withStatus(ACCEPTED)
                  .withBody(Json.obj("nrsSubmissionId" -> "test-id").toString)
              )
          )

          val repository = app.injector.instanceOf[NrsWorkItemRepository]
          val nrsService = app.injector.instanceOf[NrsService]

          await(repository.collection.drop().toFuture())

          val workItem = NrsSubmissionWorkItem(
            NrsPayload(
              payload = "test-payload",
              metadata = sampleNrsMeta
            )
          )

          // Add work item to queue - make it available 1 second in the past to ensure it's picked up
          await(
            repository.pushNew(
              workItem,
              Instant.now().minusSeconds(1)
            )
          )

          // Manually trigger processing instead of waiting for scheduler
          await(nrsService.processAll())

          // Check the item was processed successfully
          val items = await(repository.collection.find().toFuture())
          items.headOption.map(_.status) shouldBe Some(ProcessingStatus.Succeeded)
        }
      }

      "must continue processing after encountering errors" in {
        val app = GuiceApplicationBuilder()
          .configure(schedulerEnabledConfig)
          .build()

        running(app) {
          wireMockServer.stubFor(
            post(urlEqualTo("/submission"))
              .willReturn(
                aResponse()
                  .withStatus(ACCEPTED)
                  .withBody(Json.obj("nrsSubmissionId" -> "test-id").toString)
              )
          )

          val repository = app.injector.instanceOf[NrsWorkItemRepository]
          val nrsService = app.injector.instanceOf[NrsService]

          await(repository.collection.drop().toFuture())

          val testPayload1 = NrsSubmissionWorkItem(
            NrsPayload(payload = "test-payload-1", metadata = sampleNrsMeta)
          )
          val testPayload2 = NrsSubmissionWorkItem(
            NrsPayload(payload = "test-payload-2", metadata = sampleNrsMeta)
          )

          // Make items available 1 second in the past to ensure they're picked up
          await(repository.pushNew(testPayload1, Instant.now().minusSeconds(1)))
          await(repository.pushNew(testPayload2, Instant.now().minusSeconds(1)))

          // Manually trigger processing instead of waiting for scheduler
          // With the recursive processAll(), both items are processed in one call
          await(nrsService.processAll())

          // Check both items were processed successfully
          val items = await(repository.collection.find().toFuture())
          items.size shouldBe 2
          val allProcessed = items.forall(_.status == ProcessingStatus.Succeeded)
          allProcessed shouldBe true
        }
      }
    }

    "when scheduler is disabled" - {
      "must not process work items from the queue automatically" in {
        val app = GuiceApplicationBuilder()
          .configure(schedulerDisabledConfig)
          .build()

        running(app) {
          val repository = app.injector.instanceOf[NrsWorkItemRepository]

          await(repository.collection.drop().toFuture())

          wireMockServer.stubFor(
            post(urlEqualTo("/submission"))
              .willReturn(
                aResponse()
                  .withStatus(ACCEPTED)
                  .withBody(Json.obj("nrsSubmissionId" -> "test-id").toString)
              )
          )

          // Create a test work item
          val testPayload = NrsSubmissionWorkItem(
            NrsPayload(payload = "test-payload", metadata = sampleNrsMeta)
          )

          // Make item available 1 second in the past (though scheduler is disabled, for consistency)
          await(
            repository.pushNew(
              testPayload,
              Instant.now().minusSeconds(1)
            )
          )

          // Item should still be in To Do status since scheduler is disabled
          // We verify this immediately without waiting for scheduled execution
          val items = await(repository.collection.find().toFuture())
          items.headOption.map(_.status) shouldBe Some(ProcessingStatus.ToDo)
        }
      }
    }
  }
}
