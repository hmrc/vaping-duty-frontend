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

import base.SpecBase

class NrsUtilsSpec extends SpecBase {

  private val nrsUtils = new NrsUtils()

  "NrsUtils" - {
    "encode" - {
      "must encode a string to Base64" in {
        val input = "test string"
        val result = nrsUtils.encode(input)
        
        result mustBe "dGVzdCBzdHJpbmc="
      }

      "must encode an empty string" in {
        val input = ""
        val result = nrsUtils.encode(input)
        
        result mustBe ""
      }

      "must encode JSON payload" in {
        val input = """{"key":"value"}"""
        val result = nrsUtils.encode(input)
        
        result mustBe "eyJrZXkiOiJ2YWx1ZSJ9"
      }
    }

    "sha256Hash" - {
      "must generate SHA256 hash for a string" in {
        val input = "test string"
        val result = nrsUtils.sha256Hash(input)
        
        result mustBe "d5579c46dfcc7f18207013e65b44e4cb4e2c2298f4ac457ba8f82743f31e930b"
      }

      "must generate SHA256 hash for an empty string" in {
        val input = ""
        val result = nrsUtils.sha256Hash(input)
        
        result mustBe "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
      }

      "must generate SHA256 hash for JSON payload" in {
        val input = """{"key":"value"}"""
        val result = nrsUtils.sha256Hash(input)
        
        result mustBe "e43abcf3375244839c012f9633f95862d232a95b00d5bc7348b3098b9fed7f32"
      }

      "must generate consistent hash for same input" in {
        val input = "consistent test"
        val result1 = nrsUtils.sha256Hash(input)
        val result2 = nrsUtils.sha256Hash(input)
        
        result1 mustBe result2
      }

      "must generate 64 character hash" in {
        val input = "any string"
        val result = nrsUtils.sha256Hash(input)
        
        result.length mustBe 64
      }
    }
  }
}