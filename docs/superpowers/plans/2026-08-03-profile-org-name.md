# Profile org name Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show the user's organization name on the Profile screen via `GET /me` → `userInfo.orgName`.

**Architecture:** feature-service extracts Zitadel claim `urn:zitadel:iam:user:resourceowner:name` into `/me`; client maps it into `SessionUser` and renders «Организация» on Profile. No gateway changes; never show org id in UI.

**Tech Stack:** Kotlin, Spring (feature-service), KMP Compose (client-app), kotlinx.serialization

**Spec:** `client-app/docs/superpowers/specs/2026-08-03-profile-org-name-design.md`

## Global Constraints

- UI: names only — never show org UUID in Profile (workspace rule `ui-names-not-ids`)
- Optional `orgName`: blank/missing → omit Profile row
- Do not add `orgId` to `/me` in this change
- Commit/push each repo after its task; no heavy local full builds — targeted unit tests only; CI builds on GitHub
- After both tasks ship green: `/smoke-test` on Fixaverse Smoke Profile screen

---

## File map

| Repo | File | Role |
|------|------|------|
| feature-service | `JwtUserExtractor.kt` | read org name claim |
| feature-service | `JwtUserClaims` / `UserInfoDto` / `MeService` | plumb `orgName` |
| feature-service | `JwtUserExtractorTest` / `MeControllerTest` | cover claim present/absent |
| client-app | `auth/.../AuthModels.kt` | `UserInfoDto.orgName` |
| client-app | `session/ClientSession.kt` | map to `SessionUser.orgName` |
| client-app | `ui/screens/ProfileScreen.kt` | ProfileRow «Организация» |
| client-app | tests: Me decode, fromMe, Profile if any | coverage |

---

### Task 1: feature-service — `orgName` on `/me`

**Repo:** `/Users/antonbutov/Documents/MYPROJECTS/Fixaverse/feature-service` (branch `master` or `main`)

**Steps:**

- [ ] Read existing `JwtUserExtractor`, `MeService`, `MeControllerTest`, `JwtUserExtractorTest`
- [ ] Add failing tests: claim `urn:zitadel:iam:user:resourceowner:name` → `userInfo.orgName`; absent/blank → null
- [ ] Implement extractor + DTO + MeService mapping
- [ ] Run targeted tests (e.g. `./gradlew test --tests '*JwtUserExtractor*' --tests '*MeController*'` — if CI policy forbids local gradle for this repo, commit and let Actions run; prefer small unit tests locally if already used in repo)
- [ ] Commit + push; watch Actions to green

**Done when:** `/me` JSON can include `"orgName":"Fixaverse Demo"` from JWT; tests green on CI.

---

### Task 2: client-app — Profile shows organization

**Repo:** `/Users/antonbutov/Documents/MYPROJECTS/Fixaverse/client-app`  
**Depends on:** Task 1 deployed or at least API contract stable (optional field — client can ship in parallel)

**Steps:**

- [ ] Add `orgName` to `UserInfoDto` + `SessionUser`; map in `ClientSession.fromMe`
- [ ] Failing/updated tests: decode Me with orgName; fromMe maps it; blank omitted
- [ ] `ProfileScreen`: show `Организация` when `user.orgName` non-null
- [ ] Commit + push; watch CI/deploy green

**Done when:** Profile shows org name when `/me` returns it.

---

### Task 3: Smoke

- [ ] After client-app deploy success: `/smoke-test` on `https://app.fixaverse.ru/` tenant **Fixaverse Smoke** — Profile shows «Организация: Fixaverse Smoke» (or current Smoke org display name). Screenshot + Read.
