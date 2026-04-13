# Memory Bank Maintenance Rules

## ⚠️ Read This First

This project uses a **lightweight memory bank model** to avoid proxy timeout issues caused by excessive file reads and writes during tasks. The rules below override any general Cline memory bank guidance.

---

## The Core Rule

**Do not read or write memory bank files automatically.**

Memory bank activity is triggered explicitly by the user, or at task start only as defined below.

---

## What to Read at Task Start

| File | When to read |
|------|-------------|
| `projectbrief.md` | Every task — always |
| `systemPatterns.md` | Every task — always |
| `techContext.md` | Only if the task involves config, dependencies, or tooling |
| `[feature]-implementation-plan.md` | Only if one exists for the current feature |
| `progress.md` | Only if the user asks you to check it |

**Maximum files read at task start: 2 required + up to 2 optional.**

---

## What NOT to Do

❌ Do not read all memory bank files automatically at task start
❌ Do not write to any memory bank file mid-task
❌ Do not update `progress.md` automatically at task completion
❌ Do not update memory bank files as part of `attempt_completion`

---

## When to Update Files

### User says `**update memory bank**`

Update these files only:
1. `progress.md` — record what was completed
2. `systemPatterns.md` — only if a new pattern was established this session
3. The active feature implementation plan — only if phases or scope changed

Report which files were updated and what changed.

### User asks to update a specific file

Do exactly that and nothing else.

### User asks to create an implementation plan

Create `memory-bank/[feature]-implementation-plan.md` with:
- Overview and key technical decisions
- Complete page/component inventory
- Folder structure
- Phased implementation checklist
- Data models if relevant
- Routes

This file is the primary context for all future sessions working on that feature.

---

## The Lightweight Workflow

### Planning a new feature
```
User: "Plan the [feature] journey"
Cline: Creates [feature]-implementation-plan.md in memory-bank/
       Presents the plan for review
       Does NOT update any other memory bank files
```

### Working on a feature
```
User: "Work on phase 2 of the returns journey"
Cline: Reads projectbrief.md + systemPatterns.md + returns-implementation-plan.md
       Completes the task
       Does NOT write to any file unless asked
```

### Recording progress
```
User: "**update memory bank**"
Cline: Updates progress.md and/or the implementation plan checklist
       Reports what was changed
       Does NOT rewrite other files
```

---

## File Responsibilities

| File | Owner | Purpose |
|------|-------|---------|
| `projectbrief.md` | User (set up once) | Project scope and stack — rarely changes |
| `systemPatterns.md` | User + Cline (when asked) | Architecture reference |
| `techContext.md` | User (set up once) | Tech stack reference — rarely changes |
| `[feature]-plan.md` | Cline (on request) | Active feature implementation guide |
| `progress.md` | Cline (when asked) | Completed work log |

---

## Integration with Other Rules

- **`.clinerules/` rule files** — read as normal when their topic is relevant to the task
- **Implementation plans** — the primary replacement for `activeContext.md`
- All other `.clinerules` files are unchanged and should be followed as written