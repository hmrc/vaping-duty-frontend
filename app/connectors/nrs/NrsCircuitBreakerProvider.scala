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

import config.FrontendAppConfig
import connectors.nrs.NrsConnector.NrsCircuitBreaker
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.pattern.CircuitBreaker
import play.api.Logging

import javax.inject.{Inject, Provider, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

@Singleton
class NrsCircuitBreakerProvider @Inject()(
  appConfig: FrontendAppConfig,
  system: ActorSystem
)(implicit ec: ExecutionContext)
  extends Provider[NrsCircuitBreaker] with Logging {

  private val maxFailures: Int                  = appConfig.nrsCircuitBreakerMaxFailures
  private val callTimeout: FiniteDuration       = appConfig.nrsCircuitBreakerCallTimeout
  private val resetTimeout: FiniteDuration      = appConfig.nrsCircuitBreakerResetTimeout
  private val maxResetTimeout: FiniteDuration   = appConfig.nrsCircuitBreakerMaxResetTimeout
  private val exponentialBackoffFactor: Double  = appConfig.nrsCircuitBreakerExponentialBackoffFactor

  private val breaker: CircuitBreaker =
    new CircuitBreaker(
      scheduler = system.scheduler,
      maxFailures = maxFailures,
      callTimeout = callTimeout,
      resetTimeout = resetTimeout,
      maxResetTimeout = maxResetTimeout,
      exponentialBackoffFactor = exponentialBackoffFactor
    )
      .onOpen {
        logger.warn("NRS Circuit Breaker has opened")
      }
      .onHalfOpen {
        logger.warn("NRS Circuit Breaker set to half open")
      }
      .onClose {
        logger.info("NRS Circuit Breaker has closed")
      }

  override def get(): NrsCircuitBreaker = NrsCircuitBreaker(breaker)
}