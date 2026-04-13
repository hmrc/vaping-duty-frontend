# Page Creation Rules

## Structure & Injection
1. Every page/view must follow Play's injection pattern, starting with:
   ```scala
   @this()
   @(...)
   ```
   where `@this(...)` contains injected variables and `@(...)` contains passed-in variables.

2. Every page/view must have its own controller with a `show()` function, and a `submit()` function if it needs to submit data. Auxiliary functions may be used, but only in support of `show()` and `submit()`.

3. Do NOT use more than one page per controller unless it is an error page.
   (See [base-structuring-rules.md](base-structuring-rules.md) for controller architecture flow: route → controller → service → connector)

## Reusability
4. When creating new form pages, check to see if there are any generic form pages that can be used first.

5. If there are no existing generic form pages that fit your purpose, create one if it is reasonable.

6. If the page requires a lot of preamble or context text, you may make it a unique page to keep generic pages simple.

7. Pages that are just content with no form submission may be unique.

8. Pages must use reusable inline functions where possible. For example:
   ```html
   <!-- Don't do this -->
   <div>This is a Dog</div>
   <div>This is a Cat</div>
   <div>This is a Mouse</div>
   ```
   ```scala
   <!-- Do this -->
   @lineItem(animal: String) {
     <div>This is a @animal</div>
   }
   ```

## Scala in Templates
9. Ensure all lines of Scala are reasonably tagged with `@`:
   ```scala
   @iterable.map { thing =>
     @this.functionOnThing.map { innerThing =>
       <div>@{innerThing.name}</div>
     }
   }
   ```

10. Unless specifically asked, do not `.flatMap` in a view. When creating HTML, it is okay to `.map` inside a `.map`.

## Layout & Standards
11. ALWAYS use HMRC standards and twirl templates.

12. Search for a view called `Layout` or similar to use as the base layout wrapper. Only if you cannot find this may you use `GovukLayout`.

13. Forms must include the Scala Play CSRF helper to comply with CSRF requirements.

## Routing
14. Make sure to add a route for all new controller endpoints.

15. Forms added to pages must use reverse routing for their `action` attribute, not manual URL strings.

---

## Implementing Pages from Prototypes

When given a prototype (HTML, GitHub-hosted project, or project saved locally) to implement as a Twirl view, follow
this sequence exactly. Do not skip steps or improvise.

### Step 1 — Identify the components in the prototype

Read the prototype carefully. List every UI component present — headings, inputs, radios,
checkboxes, summary lists, buttons, error messages, etc. Do not begin writing code until you
have a clear inventory.

### Step 2 — Verify each component against the GOV.UK / HMRC frontend library

For each component identified, verify it exists in the official component library before
implementing it. Use the component name from the prototype as your search key.

**Component library locations to check (in order):**
1. HMRC-specific components — `https://design-system.service.gov.uk/components/`

If a component exists in the HMRC library, prefer it over the GOV.UK equivalent.

**If no matching component is found:**
> ⛔ STOP. Do not improvise with custom HTML. Ask the user:
> "I could not find a standard component for [describe what you need]. How would you like
> me to handle this?"

### Step 3 — Find the Scala Play equivalent

Once you have confirmed the correct frontend component, search `play-frontend-hmrc` for the
corresponding Twirl template using the same component name.

**Search location:** `https://github.com/hmrc/play-frontend-hmrc`

Match by component name. For example, if the prototype uses `govukInput`, search for
`GovukInput` or `govukInput` in the Twirl components directory.

**If no Scala equivalent is found:**
> ⛔ STOP. Do not fall back to raw HTML. Ask the user:
> "I found the frontend component [name] but could not locate its Twirl equivalent in
> play-frontend-hmrc. How would you like me to proceed?"

### Step 4 — Implement the view using the verified components

Build the Twirl view using only the components confirmed in steps 2 and 3.

### Step 5 — Content, messages, and dynamic values

**All visible text from the prototype must be reproduced exactly as written.**
Do not paraphrase, reword, or invent alternative copy. If the prototype says
"Enter your National Insurance number", the view must say exactly that.

All content must be externalised to the Play messages file (`messages.en` or equivalent).
No string literals may appear inline in the view.

```scala
// ❌ DON'T DO THIS — inline string:
@govukInput(Input(label = Label(content = Text("Enter your National Insurance number"))))

// ✅ DO THIS — messages key:
@govukInput(Input(label = Label(content = Text(messages("ninoPage.input.label")))))
```

Add a messages entry for every piece of content on the page. Use the page name as the key
prefix for grouping:

```
ninoPage.title = Enter your National Insurance number
ninoPage.heading = Enter your National Insurance number
ninoPage.hint = It's on your National Insurance card, benefit letter, payslip or P60. For example, 'QQ 12 34 56 C'.
ninoPage.input.label = National Insurance number
ninoPage.error.required = Enter your National Insurance number
```

#### Dynamic values in messages

Some message strings contain runtime values — for example a scheme name, a date, or a
reference number — that are not known until the page is rendered. These must use Play's
indexed message parameters (`{0}`, `{1}`, etc.) rather than being hardcoded.

**You cannot reliably infer from a prototype alone which values are dynamic.**
Follow these rules to determine how to handle content:

**Rule 1 — Explicit prompt instruction takes priority**

If the prompt explicitly identifies dynamic values, use message parameters for those values
and pass them as arguments to `messages(...)`:

```scala
// Prompt says: "the scheme name and deletion date are dynamic"
// messages.en:
// deletionWarningPage.warning = The update for {0} will be deleted on {1} if you do not work on it.

// ✅ In the view:
@messages("deletionWarningPage.warning", schemeName, deletionDate)
```

The dynamic values must also be added as parameters to the view's `@(...)` block so they
can be passed in from the controller:

```scala
// ✅ View signature includes dynamic values:
@(schemeName: String, deletionDate: String)(implicit messages: Messages)
```

**Rule 2 — Prototype placeholder convention**

If the prototype HTML contains values wrapped in double curly braces — `{{valueName}}` —
treat them as dynamic and use message parameters:

```html
<!-- Prototype contains: -->
<p>The update for {{schemeName}} will be deleted on {{deletionDate}}.</p>
```

```
// ✅ messages.en:
deletionWarningPage.warning = The update for {0} will be deleted on {1}.
```

**Rule 3 — When in doubt, stop and ask**

If content looks like it could be either static or dynamic — for example a date, a name,
or a reference number shown concretely in the prototype with no `{{}}` markers and no
instruction in the prompt — do not guess. Ask:

> "The prototype shows '[content]' — is this static copy or a dynamic runtime value?
> If dynamic, please tell me the value name and type so I can add it as a message parameter."

**Never substitute invented placeholder text** (e.g. `[name]`, `TBC`, `example value`)
for content you are unsure about. Either reproduce the prototype text exactly as static
copy, or stop and ask.

#### Dynamic values quick reference

| Signal | Action |
|---|---|
| Prompt says value is dynamic | Use `{0}`, `{1}` parameters in messages entry; add to view `@(...)` signature |
| Prototype uses `{{valueName}}` | Use `{0}`, `{1}` parameters in messages entry; add to view `@(...)` signature |
| Looks potentially dynamic, no signal | ⛔ Stop and ask |
| Clearly static copy | Reproduce exactly, externalise to messages as plain string |

### Step 6 — Work downward

Once the view is complete and verified, implement the remaining layers in order:
```
view → controller → service → connector
```
Follow all rules in `base-structuring-rules.md` for each layer.

---

### Prototype implementation quick reference

| Step | Action | If blocked |
|---|---|---|
| 1 | Inventory all components from prototype | — |
| 2 | Verify each in GOV.UK / HMRC frontend library | ⛔ Stop and ask |
| 3 | Find Twirl equivalent in play-frontend-hmrc | ⛔ Stop and ask |
| 4 | Implement view with verified components only | — |
| 5 | Reproduce prototype content exactly, externalise to messages; use `{0}` params for dynamic values | ⛔ Stop and ask if static vs dynamic is unclear |
| 6 | Work down: controller → service → connector | Follow base-structuring-rules.md |