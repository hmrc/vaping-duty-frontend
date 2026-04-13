# MCP Rules

## CRITICAL: General Principles for All MCP Tools

MCP tools extend what Cline can do significantly. With that comes responsibility
around what data leaves the project, and what actions can be taken on external
systems. These rules apply to ALL MCP tool usage.

**The two core obligations:**
1. **Query/input hygiene** — be careful what project data you send to external services
2. **Write operation gates** — never perform a destructive or mutating action without explicit per-action approval

---

## DuckDuckGo MCP (Web Search)

### What you may do
Search freely for any topic you judge will help complete the task. There is no
restriction on search topics or frequency.

### Rule 1: Never include internal identifiers in search queries

The following must NEVER appear in a search query:

- Class, object, trait, or method names from this codebase
- Package names or namespaces specific to this project
- Internal variable names or identifiers
- File paths or module names from this project
- Any project-specific naming conventions

Generalise before searching. Describe the problem in terms of the technology,
framework, and symptom only.

```
✅ "Play Framework Future recovery for-comprehension Scala 2.13"
❌ "PensionTransferSessionRepository MDC context lost flatMap"

✅ "WireMock stub fault simulation connector test Scala"
❌ "FakeCheckVpdIdAction ConnectorFixture INTERNAL_SERVER_ERROR stub"
```

### Rule 2: Never include raw error messages in search queries

Do not copy and paste error text, stack trace lines, or compiler output
directly into a search query. Identify the type of error and the technology
involved, and construct a generalised query from those.

```
✅ "Scala type mismatch Future Option implicit resolution Play"
❌ "type mismatch found Option[VpdSummaryResponse] required SummaryResponse"
```

### Rule 3: Never use Confluence content in search queries

If you have read any Confluence pages during a task, do not include page
titles, space names, or any text extracted from those pages in a web search
query. Summarise or generalise the concept before searching.

### Rule 4: Permission required to include project-specific detail

If you have tried to solve a problem and believe that including the actual
error message, internal identifier, or Confluence content in a query would
materially improve your chances of finding a solution, you MUST stop and ask:

> "I've been unable to solve this with generalised searches. I think including
> [describe what you want to include] in the query would help. Are you happy
> for me to include this?"

Wait for explicit approval before proceeding with that search.

### DuckDuckGo quick reference

| Content | Allowed in query? |
|---|---|
| Technology names (Play, Scala, WireMock, ZIO) | ✅ Yes |
| Framework concepts (for-comprehension, Future, implicit) | ✅ Yes |
| Error *type* described in your own words | ✅ Yes |
| Any topic you judge relevant to the task | ✅ Yes |
| Internal class / method / variable names | ❌ Never without permission |
| Raw error messages or stack trace text | ❌ Never without permission |
| Text or titles sourced from Confluence | ❌ Never without permission |

---

## GitHub MCP

### What you may do freely
- Read repository contents, file structure, and code
- Read issues, pull requests, and comments
- Read branches, commit history, and diffs
- Read repository settings and metadata

### Rule 1: No write operations without explicit per-action approval

You must NEVER perform any of the following without an explicit instruction
from the user for that specific action:

- Pushing or committing code to any branch
- Creating, renaming, or deleting branches
- Opening, closing, or merging pull requests
- Creating or closing issues
- Modifying repository settings, webhooks, or permissions
- Any operation that mutates the state of a repository

If you believe a write operation would help complete the task, describe
exactly what you want to do and ask for approval first:

> "To complete this I would need to [describe the specific write action —
> e.g. push a commit to branch X / open a PR from Y to Z]. Are you happy
> for me to do this?"

Wait for explicit confirmation before proceeding.

### Rule 2: Never include repo content in search queries

Do not use repository names, internal package names, or code extracted from
the GitHub MCP in a DuckDuckGo search query. Apply the same generalisation
rules as for any other project content.

### Rule 3: Treat all accessible repos as sensitive

Your GitHub PAT may grant access to more repositories than the one you are
currently working in. Do not read from, reference, or act on any repository
other than the one relevant to the current task unless explicitly instructed.

### GitHub quick reference

| Operation | Allowed? |
|---|---|
| Read code, files, structure | ✅ Yes |
| Read issues, PRs, history | ✅ Yes |
| Push commits / create branches | ❌ Ask first |
| Open / close / merge PRs | ❌ Ask first |
| Delete branches or repos | ❌ Ask first |
| Modify settings or webhooks | ❌ Ask first |
| Access repos outside current task | ❌ Ask first |

---

## Atlassian / Confluence MCP

### What you may do freely
- Read Confluence pages and spaces to gather context for a task
- Read page content, comments, and attachments to understand requirements
- Search Confluence for relevant documentation

### Rule 1: No write operations without explicit per-action approval

You must NEVER perform any of the following without an explicit instruction
from the user for that specific action:

- Creating new Confluence pages or spaces
- Editing or updating existing page content
- Deleting pages, comments, or attachments
- Modifying space permissions or settings
- Adding or removing labels or metadata

If you believe creating or editing a page would help, ask first:

> "To complete this I would need to [describe the specific action — e.g.
> create a new page in space X / update section Y on page Z]. Are you happy
> for me to do this?"

Wait for explicit confirmation before proceeding.

### Rule 2: Treat Confluence content as confidential

Content read from Confluence is internal documentation. You must not:

- Include page titles, space names, or extracted text in DuckDuckGo search queries
- Reproduce significant portions of Confluence content in responses beyond
  what is necessary to complete the task
- Reference Confluence content in GitHub commits, PR descriptions, or
  issue comments unless explicitly instructed

### Rule 3: Only read what is necessary

Do not browse Confluence speculatively. Only read pages that are directly
relevant to the current task. If you are unsure whether a page is relevant,
describe it and ask rather than reading it preemptively.

### Confluence quick reference

| Operation | Allowed? |
|---|---|
| Read pages and documentation | ✅ Yes |
| Search for relevant pages | ✅ Yes |
| Use content to inform your work | ✅ Yes |
| Include content in DDG searches | ❌ Never without permission |
| Create or edit pages | ❌ Ask first |
| Delete pages or content | ❌ Ask first |
| Modify space settings | ❌ Ask first |

---

## Cross-MCP Rules

These rules apply whenever two or more MCP tools are used in combination
during the same task.

### No data laundering between tools

Do not pass content retrieved from one MCP tool directly into another MCP
tool as input without first checking whether doing so would breach the rules
above.

Common scenarios to watch for:

- Reading a Confluence page → constructing a GitHub search or DDG query from its content ❌
- Reading GitHub code → including class/method names in a DDG search query ❌
- Reading DDG search results → creating a Confluence page from them without approval ❌

### Summarise and generalise when chaining tools

If you need to use content from one tool to inform use of another, generalise
and abstract the content first — exactly as you would when constructing a
search query.