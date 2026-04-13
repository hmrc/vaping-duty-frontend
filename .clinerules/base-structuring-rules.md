# Base Structuring Rules

## CRITICAL: Before Writing Any Code

1. **Check for existing patterns first** - Read similar files in the project to understand established patterns

2. **No magic values allowed** - All strings and numbers MUST be named constants:
   - **Single file use:** Declare a `val` at the top of the file
   - **Multi-file use:** Add to `AppConfig` or create a constants object
   
   ```scala
   // ❌ DON'T DO THIS:
   if (status == "active") { ... }
   val url = s"/some-endpoint/$id"
   if (retryCount < 3) { ... }
   
   // ✅ DO THIS:
   val STATUS_ACTIVE = "active"
   val ENDPOINT_PREFIX = "/some-endpoint/"
   val MAX_RETRIES = 3
   
   if (status == STATUS_ACTIVE) { ... }
   val url = s"$ENDPOINT_PREFIX$id"
   if (retryCount < MAX_RETRIES) { ... }
   ```

3. **Where to put constants:**
   - **AppConfig (for cross-file use):**
     ```scala
     // In AppConfig.scala
     val vpdServiceId: String = "vpd"
     val maxRetries: Int = 3
     ```
   - **File-level val (for single-file use):**
     ```scala
     // At top of your file
     object MyController {
       private val DEFAULT_PAGE_SIZE = 10
       private val STATUS_PENDING = "pending"
       // ... rest of code
     }
     ```

4. **Follow the architecture flow** - Never skip layers:
   ```
   route → controller → service → connector
   ```

---

## Architecture
A service must follow this flow strictly:

route <-> controller <-> service <-> connector

- Controllers must have as little logic as possible. The only code permitted in a controller
  beyond delegation is Play Action responses: `Ok(...)`, `BadRequest(...)`, `Redirect(...)` etc.
- Controllers serve a single page only, with at most a `show()` and `submit()` function.
  If a request involves multiple pages, create multiple controllers.
- Services handle business logic, data aggregation, and orchestration of connector calls.
- Connectors handle ALL external HTTP calls. A controller must never call a connector directly.

## Code Style
- Implicit values must always be explicitly typed.
- Never declare magic strings or numbers inline. Declare a `val` at the top of the file,
  or add it to a constants file if the value is used in more than one place.

## Folder Structure
/controllers
/models
/forms
/views
/connectors
/config
/services

## Views
- Never reference views as `views.html.myView` — use the injected reference directly.

## Imports
Prefer short imports over fully qualified names. Examples:
- `play.api.libs.json.Json.toJson` → import `Json`, use `Json.toJson`