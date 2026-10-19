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

import base.SpecBase
import com.codahale.metrics.MetricRegistry
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import repositories.NrsWorkItemRepository
import uk.gov.hmrc.mongo.workitem.ProcessingStatus

import scala.concurrent.Future

class NrsWorkItemMetricsSpec extends SpecBase {

  private def gauge(registry: MetricRegistry, name: String): Any =
    registry.getGauges.get(s"nrs-work-items.$name").getValue

  "NrsWorkItemMetrics" - {
    "must register gauges for each status and the max failure count, starting at 0" in {
      val registry = new MetricRegistry
      new NrsWorkItemMetrics(mock[NrsWorkItemRepository], registry)

      Seq("todo", "in-progress", "failed", "permanently-failed", "succeeded", "max-failure-count").foreach { name =>
        withClue(s"$name: ") {
          gauge(registry, name) mustBe 0L
        }
      }
    }

    "refresh must update gauges from the repository" in {
      val registry       = new MetricRegistry
      val mockRepository = mock[NrsWorkItemRepository]
      when(mockRepository.count(ProcessingStatus.ToDo)).thenReturn(Future.successful(3L))
      when(mockRepository.count(ProcessingStatus.InProgress)).thenReturn(Future.successful(1L))
      when(mockRepository.count(ProcessingStatus.Failed)).thenReturn(Future.successful(2L))
      when(mockRepository.count(ProcessingStatus.PermanentlyFailed)).thenReturn(Future.successful(4L))
      when(mockRepository.count(ProcessingStatus.Succeeded)).thenReturn(Future.successful(10L))
      when(mockRepository.maxFailureCount()).thenReturn(Future.successful(7))

      val metrics = new NrsWorkItemMetrics(mockRepository, registry)
      metrics.refresh().futureValue

      gauge(registry, "todo") mustBe 3L
      gauge(registry, "in-progress") mustBe 1L
      gauge(registry, "failed") mustBe 2L
      gauge(registry, "permanently-failed") mustBe 4L
      gauge(registry, "succeeded") mustBe 10L
      gauge(registry, "max-failure-count") mustBe 7L
    }

    "refresh must not fail when the repository errors" in {
      val registry       = new MetricRegistry
      val mockRepository = mock[NrsWorkItemRepository]
      when(mockRepository.count(ProcessingStatus.ToDo)).thenReturn(Future.failed(new RuntimeException("mongo down")))
      when(mockRepository.count(ProcessingStatus.InProgress)).thenReturn(Future.successful(1L))
      when(mockRepository.count(ProcessingStatus.Failed)).thenReturn(Future.successful(2L))
      when(mockRepository.count(ProcessingStatus.PermanentlyFailed)).thenReturn(Future.successful(4L))
      when(mockRepository.count(ProcessingStatus.Succeeded)).thenReturn(Future.successful(10L))
      when(mockRepository.maxFailureCount()).thenReturn(Future.successful(7))

      val metrics = new NrsWorkItemMetrics(mockRepository, registry)
      metrics.refresh().futureValue

      gauge(registry, "todo") mustBe 0L
    }
  }
}
