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

import org.apache.pekko.actor.{ActorSystem, Cancellable, Scheduler}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import base.SpecBase
import config.FrontendAppConfig
import org.scalatestplus.mockito.MockitoSugar.mock
import services.nrs.{NrsService, NrsWorkItemMetrics}

import scala.concurrent.Future
import scala.concurrent.duration.*

class ScheduleProcessorSpec extends SpecBase with BeforeAndAfterEach {

  private val mockActorSystem: ActorSystem      = mock[ActorSystem]
  private val mockScheduler: Scheduler          = mock[Scheduler]
  private val mockNrsService: NrsService        = mock[NrsService]
  private val mockNrsWorkItemMetrics            = mock[NrsWorkItemMetrics]
  private val mockAppConfig: FrontendAppConfig  = mock[FrontendAppConfig]
  private val mockCancellable: Cancellable      = mock[Cancellable]

  private val expectedInitialDelay = 1.minute
  private val expectedInterval     = 30.seconds

  override def beforeEach(): Unit = {
    reset(mockNrsService, mockNrsWorkItemMetrics)
    when(mockNrsWorkItemMetrics.updateStatusCount()).thenReturn(Future.successful(()))
    super.beforeEach()
  }

  "ScheduleProcessor" - {
    "scheduleProcessing must" - {
      // scalafix:off DisableSyntax.var
      "set up scheduler with correct timing parameters" in {
        when(mockAppConfig.nrsSchedulerInitialDelay).thenReturn(expectedInitialDelay)
        when(mockAppConfig.nrsSchedulerInterval).thenReturn(expectedInterval)
        when(mockAppConfig.nrsSubmissionEnabled).thenReturn(true)
        when(mockActorSystem.scheduler).thenReturn(mockScheduler)
        when(mockScheduler.scheduleAtFixedRate(any[FiniteDuration], any[FiniteDuration])(any[Runnable])(any()))
          .thenReturn(mockCancellable)
        when(mockNrsService.processAll()).thenReturn(Future.successful(()))

        val processor = new ScheduleProcessor(mockActorSystem, mockNrsService, mockNrsWorkItemMetrics, mockAppConfig)
        processor.scheduleProcessing()

        verify(mockScheduler).scheduleAtFixedRate(
          eqTo(expectedInitialDelay),
          eqTo(expectedInterval)
        )(any[Runnable])(any())
      }

      "call nrsService.processAll() when nrsSubmissionEnabled is true" in {
        when(mockAppConfig.nrsSchedulerInitialDelay).thenReturn(expectedInitialDelay)
        when(mockAppConfig.nrsSchedulerInterval).thenReturn(expectedInterval)
        when(mockAppConfig.nrsSubmissionEnabled).thenReturn(true)
        when(mockActorSystem.scheduler).thenReturn(mockScheduler)
        when(mockNrsService.processAll()).thenReturn(Future.successful(()))

        // Capture the runnable that gets scheduled
        var capturedRunnable: Option[Runnable] = None
        when(mockScheduler.scheduleAtFixedRate(any[FiniteDuration], any[FiniteDuration])(any[Runnable])(any()))
          .thenAnswer { invocation =>
            capturedRunnable = Some(invocation.getArgument[Runnable](2))
            mockCancellable
          }

        val processor = new ScheduleProcessor(mockActorSystem, mockNrsService, mockNrsWorkItemMetrics, mockAppConfig)
        processor.scheduleProcessing()

        // Execute the captured runnable to simulate scheduler execution
        capturedRunnable.foreach(_.run())

        verify(mockNrsService).processAll()
      }

      "skip processing when nrsSubmissionEnabled is false" in {
        when(mockAppConfig.nrsSchedulerInitialDelay).thenReturn(expectedInitialDelay)
        when(mockAppConfig.nrsSchedulerInterval).thenReturn(expectedInterval)
        when(mockAppConfig.nrsSubmissionEnabled).thenReturn(false)
        when(mockActorSystem.scheduler).thenReturn(mockScheduler)

        // Capture the runnable that gets scheduled
        var capturedRunnable: Option[Runnable] = None
        when(mockScheduler.scheduleAtFixedRate(any[FiniteDuration], any[FiniteDuration])(any[Runnable])(any()))
          .thenAnswer { invocation =>
            capturedRunnable = Some(invocation.getArgument[Runnable](2))
            mockCancellable
          }

        val processor = new ScheduleProcessor(mockActorSystem, mockNrsService, mockNrsWorkItemMetrics, mockAppConfig)
        processor.scheduleProcessing()

        // Execute the captured runnable to simulate scheduler execution
        capturedRunnable.foreach(_.run())

        verify(mockNrsService, never()).processAll()
      }

      "refresh NRS work item metrics on each run, even when nrsSubmissionEnabled is false" in {
        when(mockAppConfig.nrsSchedulerInitialDelay).thenReturn(expectedInitialDelay)
        when(mockAppConfig.nrsSchedulerInterval).thenReturn(expectedInterval)
        when(mockAppConfig.nrsSubmissionEnabled).thenReturn(false)
        when(mockActorSystem.scheduler).thenReturn(mockScheduler)

        // Capture the runnable that gets scheduled
        var capturedRunnable: Option[Runnable] = None
        when(mockScheduler.scheduleAtFixedRate(any[FiniteDuration], any[FiniteDuration])(any[Runnable])(any()))
          .thenAnswer { invocation =>
            capturedRunnable = Some(invocation.getArgument[Runnable](2))
            mockCancellable
          }

        val processor = new ScheduleProcessor(mockActorSystem, mockNrsService, mockNrsWorkItemMetrics, mockAppConfig)
        processor.scheduleProcessing()

        capturedRunnable.foreach(_.run())

        verify(mockNrsWorkItemMetrics).updateStatusCount()
      }

      "handle errors from nrsService.processAll() without crashing" in {
        when(mockAppConfig.nrsSchedulerInitialDelay).thenReturn(expectedInitialDelay)
        when(mockAppConfig.nrsSchedulerInterval).thenReturn(expectedInterval)
        when(mockAppConfig.nrsSubmissionEnabled).thenReturn(true)
        when(mockActorSystem.scheduler).thenReturn(mockScheduler)
        when(mockNrsService.processAll()).thenReturn(Future.failed(new RuntimeException("Test error")))


        // Capture the runnable that gets scheduled
        var capturedRunnable: Option[Runnable] = None
        when(mockScheduler.scheduleAtFixedRate(any[FiniteDuration], any[FiniteDuration])(any[Runnable])(any()))
          .thenAnswer { invocation =>
            capturedRunnable = Some(invocation.getArgument[Runnable](2))
            mockCancellable
          }

        val processor = new ScheduleProcessor(mockActorSystem, mockNrsService, mockNrsWorkItemMetrics, mockAppConfig)
        processor.scheduleProcessing()

        // Execute the captured runnable - should not throw despite the error
        capturedRunnable.foreach(_.run())

        verify(mockNrsService).processAll()
        // Test passes if no exception is thrown
        succeed
      }
    }
  }
}
