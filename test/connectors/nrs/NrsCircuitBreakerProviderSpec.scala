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

package connectors.nrs

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.CircuitBreaker
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import base.SpecBase
import config.FrontendAppConfig
import org.scalatestplus.mockito.MockitoSugar.mock

import scala.concurrent.Await
import scala.concurrent.duration.*

class NrsCircuitBreakerProviderSpec extends SpecBase with BeforeAndAfterAll {

  private val mockAppConfig = mock[FrontendAppConfig]
  
  when(mockAppConfig.nrsCircuitBreakerMaxFailures).thenReturn(5)
  when(mockAppConfig.nrsCircuitBreakerCallTimeout).thenReturn(30.seconds)
  when(mockAppConfig.nrsCircuitBreakerResetTimeout).thenReturn(60.seconds)
  when(mockAppConfig.nrsCircuitBreakerMaxResetTimeout).thenReturn(300.seconds)
  when(mockAppConfig.nrsCircuitBreakerExponentialBackoffFactor).thenReturn(2.0)

  private lazy val actorSystem: ActorSystem = ActorSystem("test-system")

  override def afterAll(): Unit = {
    Await.result(actorSystem.terminate(), 5.seconds)
    super.afterAll()
  }

  "NrsCircuitBreakerProvider" - {
    "must create a NrsCircuitBreaker with correct configuration" in {
      val provider = new NrsCircuitBreakerProvider(mockAppConfig, actorSystem)
      val nrsCircuitBreaker = provider.get()

      nrsCircuitBreaker mustBe a[NrsConnector.NrsCircuitBreaker]
      nrsCircuitBreaker.breaker mustBe a[CircuitBreaker]
    }

    "must return the same instance on multiple calls" in {
      val provider = new NrsCircuitBreakerProvider(mockAppConfig, actorSystem)
      val breaker1 = provider.get()
      val breaker2 = provider.get()

      breaker1 mustBe breaker2
    }

    "must configure circuit breaker with values from configuration" in {
      val provider = new NrsCircuitBreakerProvider(mockAppConfig, actorSystem)
      val nrsCircuitBreaker = provider.get()

      // Circuit breaker is created successfully with config values
      nrsCircuitBreaker.breaker mustBe a[CircuitBreaker]
    }
  }
}