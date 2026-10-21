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

package config.nrs

import connectors.nrs.NrsConnector

import javax.inject.*
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class Initialise @Inject()(submissionConnector: NrsConnector) {

  // Delete this once confirmation of successful request.
  Await.result(submissionConnector.invalidSubmit, 20.seconds)
}