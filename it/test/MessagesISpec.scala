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

import org.scalactic.source.Position
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.nio.file.{Path, Paths}
import scala.io.Source.fromFile as readFile

class MessagesISpec extends AnyFreeSpec with Matchers {
  private val illegalChars = List("'")

  private def readFileLines(path: String) = {
    val source   = readFile(path)
    val lines    = source.getLines.toList
    val linesMap = lines.zipWithIndex.map { case (line, idx) => idx -> line }.toMap

    source.close()

    linesMap
  }

  private def assertNoIllegalCharsPresent(lines: Map[Int, String]): Unit = {
    lines foreach { case (idx, line) =>
      illegalChars foreach { illegal =>
        line must not include(illegal)
      }
    }
  }

  "messages files " - {
    s"must not contain any illegal characters (${illegalChars.mkString(",")}) in conf/messages.en" in {
      val messagesFilePath = resolveMessageFile("messages.en")
      val lines            = readFileLines(messagesFilePath)
      
      assertNoIllegalCharsPresent(lines)
    }

    s"must not contain any illegal characters (${illegalChars.mkString(",")}) in conf/messages.cy" in {
      val messagesFilePath = resolveMessageFile("messages.cy")
      val lines            = readFileLines(messagesFilePath)
      
      assertNoIllegalCharsPresent(lines)
    }
  }

  /** This series of methods resolve the path of the messages files.
    * @note
    *   the file path when executing it/test through sbt is different from that which is present when executing it/test through IDEs.
    *
    * This method attempts to resolve this by detecting the user dir of the executing process.
    */

  def moveToConfFile(path: Path, messageFileName: String) = path
    .resolve("conf")
    .resolve(messageFileName)
    .toString

  private def resolveMessageFile(messageFileName: String): String = {
    val userDir = Paths.get(System.getProperty("user.dir"))

    if (userDir.endsWith("vaping-duty-frontend"))
      moveToConfFile(path = userDir, messageFileName)
    else
      moveToConfFile(path = userDir.getParent, messageFileName)
  }
}
