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

package utils

import base.SpecBase
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.time.Month

class ReturnsDateUtilSpec extends SpecBase with Matchers {

  private val months = Map(
    Month.JANUARY -> "January",
    Month.FEBRUARY -> "February",
    Month.MARCH -> "March",
    Month.MAY -> "April",
    Month.MAY -> "May",
    Month.JUNE -> "June",
    Month.JULY -> "July",
    Month.AUGUST -> "August",
    Month.SEPTEMBER -> "September",
    Month.OCTOBER -> "October",
    Month.NOVEMBER -> "November",
    Month.DECEMBER -> "December"
  )
  
  "ReturnsDateUtil.getCurrentMonthMessage" - {

    "must return the correct message" in {
      lazy val app = applicationBuilder().build()

      months.foreach(el =>
        ReturnsDateUtils.getMonthMessage(el._1)(messages(app)) mustBe el._2
      )
    }
  }

  "ReturnsDateUtil.getMonthMessageKey" - {

    "must return correct message key for all valid months" in {
      ReturnsDateUtils.getMonthMessageKey(1) mustBe "month.jan"
      ReturnsDateUtils.getMonthMessageKey(2) mustBe "month.feb"
      ReturnsDateUtils.getMonthMessageKey(3) mustBe "month.mar"
      ReturnsDateUtils.getMonthMessageKey(4) mustBe "month.apr"
      ReturnsDateUtils.getMonthMessageKey(5) mustBe "month.may"
      ReturnsDateUtils.getMonthMessageKey(6) mustBe "month.jun"
      ReturnsDateUtils.getMonthMessageKey(7) mustBe "month.jul"
      ReturnsDateUtils.getMonthMessageKey(8) mustBe "month.aug"
      ReturnsDateUtils.getMonthMessageKey(9) mustBe "month.sep"
      ReturnsDateUtils.getMonthMessageKey(10) mustBe "month.oct"
      ReturnsDateUtils.getMonthMessageKey(11) mustBe "month.nov"
      ReturnsDateUtils.getMonthMessageKey(12) mustBe "month.dec"
    }

    "must throw IllegalArgumentException for invalid month numbers" in {
      val exception0 = intercept[IllegalArgumentException] {
        ReturnsDateUtils.getMonthMessageKey(0)
      }
      exception0.getMessage must include("Invalid month number: 0")

      val exception13 = intercept[IllegalArgumentException] {
        ReturnsDateUtils.getMonthMessageKey(13)
      }
      exception13.getMessage must include("Invalid month number: 13")

      val exceptionNegative = intercept[IllegalArgumentException] {
        ReturnsDateUtils.getMonthMessageKey(-1)
      }
      exceptionNegative.getMessage must include("Invalid month number: -1")
    }
  }
}
