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

package models.audit

enum PreferenceAction:
  case EmailToEmail, EmailToPost, PostToEmail, PostToPost

object PreferenceAction {

  def apply(action: (Boolean, Boolean)): PreferenceAction = {
    action match {
      case (true, true)     => EmailToEmail
      case (false, true)    => EmailToPost
      case (true, false)    => PostToEmail
      // Journey does not allow submission of preference if attempting to change from post to post as address
      // cannot be updated in the service.  Case added to be complete.
      case (false, false)   => PostToPost
    }
  }
}
