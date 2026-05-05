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

package viewmodels.returns.view

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, TableRow, Tag, Text}
import uk.gov.hmrc.govukfrontend.views.html.components.GovukTag
import views.html.components.Link

case class OutstandingReturnRow(
  monthDisplay: String,
  status: String,
  statusClass: String,
  submitLink: String
) extends ReturnRow {

  private val govukTag = GovukTag()
  private val link = Link()

  override def toTableRows(implicit messages: Messages): Seq[TableRow] = {
    Seq(
      TableRow(
        content = Text(monthDisplay)
      ),
      TableRow(
        content = HtmlContent(govukTag(Tag(
          content = Text(status),
          classes = statusClass
        )))
      ),
      TableRow(
        content = HtmlContent(link(
          id = "submit-link",
          href = submitLink,
          text = messages("returns.overview.outstanding.submitReturn")
        ))
      )
    )
  }
}
