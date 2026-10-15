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

package utils

import java.lang.String.format
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest.getInstance
import java.util.Base64
import javax.inject.Singleton

@Singleton
class NrsUtils {

  def encode(str: String): String =
    Base64.getEncoder.encodeToString(str.getBytes(StandardCharsets.UTF_8))

  def sha256Hash(text: String): String =
    format(
      "%064x",
      new BigInteger(
        1,
        getInstance("SHA-256")
          .digest(text.getBytes(StandardCharsets.UTF_8))
      )
    )
}