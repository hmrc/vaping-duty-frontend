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

import java.nio.file.Paths
import scala.io.Source.fromFile as readFile
import java.nio.file.Path

class MessagesISpec extends AnyFreeSpec {
  private val illegalChar: String = "'"

  private def readFileLines(path: String) = {
    val source = readFile(path)

    try source.getLines mkString "\n" finally source.close()
  }

  "messages files " - {
    "must not contain a non-smart punctuation apostrophe in conf/messages.en" in {
      val messagesFilePath = resolveMessageFile("messages.en")
      val lines = readFileLines(messagesFilePath)

      if (lines.contains(illegalChar)) {
        throw new Error(s"messages.en file contains illegal character \"$illegalChar\"; the smart punctuation variant should be used instead.")
      }
    }

    "must not contain a non-smart punctuation apostrophe in conf/messages.cy" in {
      val messagesFilePath: String = resolveMessageFile("messages.cy")
      val lines = readFileLines(messagesFilePath)

      if (lines.contains(illegalChar)) {
        throw new Error(s"messages.cy file contains illegal character \"$illegalChar\"; the smart punctuation variant should be used instead.")
      }
    }
  }

  /** This series of methods resolve the path of the messages files.
   * @note the file path when executing it/test through sbt is different
   *       from that which is present when executing it/test through IDEs.
   *
   *       This method attempts to resolve this by detecting the user dir
   *       of the executing process.
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
