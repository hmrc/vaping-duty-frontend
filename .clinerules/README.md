# Cline Rules - READ THIS FIRST

## ⚠️ Memory Bank: Read This First

At task start, read:
1. `memory-bank/projectbrief.md` — always
2. `memory-bank/systemPatterns.md` — always
3. `memory-bank/techContext.md` — only if task involves config or tooling
4. `memory-bank/[feature]-implementation-plan.md` — only if one exists for the current feature

**Do not write to any memory bank file mid-task or at task completion unless asked.**

See `memory-bank-maintenance.md` for full details.

---

## Critical Rules to Check Before ANY Task

### 1. Check What Already Exists
- **For tests:** Read `SpecBase` to see what's provided (cc, fakeRequest, appConfig, fakeAuthorisedAction, hc, ec, etc.)
- **For constants:** Read `AppConfig` to see what's already defined
- **For patterns:** Find similar existing code in the project

### 2. No Magic Values
All strings and numbers must be named constants:
- **Single file use:** Declare a `val` at the top of the file
- **Multi-file use:** Add to `AppConfig` or create a constants object
- ❌ **DON'T:** `if (status == "active")`
- ✅ **DO:** `val STATUS_ACTIVE = "active"` then `if (status == STATUS_ACTIVE)`

### 3. One Model Per File
Never put multiple case classes in one file — each gets its own file with companion object.

### 4. Never Redeclare from SpecBase
Don't redeclare `implicit val hc`, `fakeRequest`, `ec`, etc. — use them from SpecBase.

---

## Rule Files by Task Type

### Using MCP Tools
- **[mcp-rules.md](mcp-rules.md)** — DuckDuckGo, GitHub, Confluence rules

### Creating Backend Components
- **[base-structuring-rules.md](base-structuring-rules.md)** — Architecture, code style, imports. Read FIRST for any backend work.
- **[model-rules.md](model-rules.md)** — Case classes and companion objects
- **[using-forms-rules.md](using-forms-rules.md)** — Form structure and reuse

### Creating Views/Pages
- **[page-creation-rules.md](page-creation-rules.md)** — Views, templates, routing, implementing from prototypes, dynamic content

### Writing Tests
- **[testing-rules.md](testing-rules.md)** — ⚠️ Always read the CRITICAL section first

### Implementing from Spec
- **[document-rules.md](document-rules.md)** — Only when given a journey specification document

### Memory Bank Operations
- **[memory-bank-maintenance.md](memory-bank-maintenance.md)** — Lightweight update rules

---

## Quick Reference: Common Mistakes to Avoid

### Code Style
❌ Hard-coded strings/numbers inline
❌ Multiple models in one file
❌ Fully qualified imports
❌ Magic strings like `"active"`, `"GET"`, `"/some-url"`

✅ Named constants in AppConfig or file-level vals
✅ One model per file with companion object
✅ Short imports
✅ All strings/numbers as named constants

### Testing
❌ Redeclaring `implicit val hc` from SpecBase
❌ `new FakeAuthorisedAction()` without parameters
❌ Using `Instant.now()` in tests
❌ Testing only status code without response body

✅ Use `hc` directly from SpecBase
✅ Use `fakeAuthorisedAction` from SpecBase
✅ Use fixed `Clock` from TestData
✅ Always test both status AND body

### Architecture
❌ Controller calling connector directly
❌ Business logic in controllers
❌ Multiple pages in one controller

✅ route → controller → service → connector
✅ Controllers only delegate and return Play responses
✅ One page per controller (show + submit)

### Memory Bank
❌ Reading all memory bank files at task start
❌ Writing to memory bank files mid-task

✅ Read `projectbrief.md` + `systemPatterns.md` at task start
✅ Read the relevant implementation plan if one exists
✅ Only update memory bank files when explicitly asked