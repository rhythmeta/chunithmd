# chunithmd Agent Memory

## Project direction

- `chunithmd` is the CHUNITHM counterpart of `maimaid` under the Rhythmeta ecosystem.
- The project is currently in the planning and data-source phase. Do not start a first-version implementation until the data-source and static-bundle design has been reviewed and confirmed.
- The immediate next planning topic is the `chunithmd` static bundle build and publication flow.

## Client architecture

- Use Kotlin Multiplatform for shared business logic and platform-native UI.
- iOS UI is SwiftUI.
- Android UI is MIUIX.
- Shared code should contain domain models, rules/calculations, serialization, API contracts, and repository-facing logic where platform support is appropriate. Platform UI and platform-specific integrations remain native.
- Before implementation, explicitly choose the KMP targets, Kotlin/Gradle baseline, supported iOS and Android versions, persistence strategy, networking/serialization libraries, and the boundary between shared and native code.

## Online scope

- The first `chunithmd` online scope is limited to the account system and community aliases. Other online features are out of scope until separately approved.
- Community aliases should follow the existing `maimaid` product and data-model direction, while meeting a serverless deployment requirement.
- “Serverless” is a deployment/runtime constraint to resolve in the service design. It must include a concrete choice of runtime, database/storage, migrations, scheduled jobs, rate limiting, and consistency behavior; it is not satisfied by merely placing a conventional stateful server behind a function URL.

## Rhythmeta account continuity

- Existing `maimaid` accounts should be upgraded into Rhythmeta accounts without requiring users to create a second account.
- Account identity must remain stable across the upgrade. Preserve the canonical user identifier and existing credentials or provide an explicit, secure migration path.
- The shared authentication contract must define token issuer/audience, signing keys and rotation, refresh-token/session invalidation, email verification, MFA/passkeys, username/handle uniqueness, and rollback/compatibility behavior before either client is implemented.
- A hostname or route change alone is not an account migration. Do not fork the user table or create a second independent identity namespace for `chunithmd`.

## API namespace

- Public API base paths are unified under the Rhythmeta host:
  - `https://api.rhythmeta.org/maimaid/v1`
  - `https://api.rhythmeta.org/chunithmd/v1`
- The exact behavior of shared authentication routes, health checks, documentation, internal jobs, redirects, and legacy paths remains a design item. Versioned resource routes should not silently mix the two product namespaces.
- API schemas, error envelopes, pagination, auth headers, and compatibility rules should be shared where the contract is common and product-scoped where data is product-specific.

## `chunithmd` backend direction

- Use ElysiaJS 2 for the `chunithmd` service, with generated API documentation as a primary benefit.
- Confirm the actual Elysia 2 release, Bun/runtime support, OpenAPI plugin/version, validation/schema library, and serverless adapter before scaffolding the backend.
- Generated documentation must be derived from the registered route schemas and checked in CI; it must not become a second hand-maintained API contract.

## Review of the current plan

The plan is coherent as a direction, but these points need an explicit decision before implementation:

1. **KMP boundary:** “one logic, native UI” does not by itself decide what is shared. Define shared modules and native escape hatches, especially for secure credential storage, camera/OCR, background work, database access, and deep links.
2. **Account migration:** define whether `maimaid` and `chunithmd` call one shared auth service or two services that validate the same Rhythmeta issuer. Plan a staged migration and legacy endpoint/token compatibility window.
3. **Serverless aliases:** the current `maimaid` backend uses Bun/Hono, Prisma, PostgreSQL, and stateful community-alias workflows. Reusing its data model is reasonable, but its runtime and job/storage assumptions do not automatically satisfy serverless. Decide what is shared and what is reimplemented at the edge.
4. **API path migration:** changing to `/maimaid/v1` and `/chunithmd/v1` affects clients, CORS, docs, reverse-proxy rules, monitoring, and every generated URL. Keep an explicit compatibility or redirect plan for existing `maimaid` clients.
5. **Product scope wording:** the first online scope should be recorded as “accounts plus community aliases”; all other online features remain deferred. Local/offline catalog and static data are separate from this online-scope statement.
6. **Data-source gate:** do not select schemas, identifiers, bundle formats, or synchronization behavior for `chunithmd` until the authoritative data sources, licensing/attribution requirements, update cadence, and reproducible build inputs are confirmed.

## Deferred decisions

- Authoritative CHUNITHM song/chart data sources and source precedence.
- Static bundle format, manifest/versioning, compression, signatures/checksums, delta updates, and rollback.
- Static asset hosting and whether the bundle is embedded, downloaded, or both.
- Serverless provider/runtime and its database, object storage, queue/cron, and observability choices.
- Shared Rhythmeta auth service topology and the exact `maimaid` migration procedure.
- KMP dependency versions and minimum OS/SDK support.

## Validation workflow

- After Android navigation or animation changes, Codex should compile the Android app and stop. The user performs device installation, interaction testing, and visual acceptance.

## Android navigation lessons

- Root tabs are peers, so they stay inside one root navigation entry. Switching tabs is a horizontal translation between two adjacent pages; root tabs should not be modeled as stacked `NavDisplay` destinations.
- Child screens use the navigation stack and the normal Miuix page transition. Keep root back behavior separate: a non-home root returns to home, while home falls through to the system exit behavior.
- Predictive back must keep the source and destination pages adjacent. The destination layer starts outside the viewport and moves in with the gesture; do not leave a full destination page at `x = 0` underneath the source page.
- Do not keep a hidden full root `Scaffold` alive while a child destination is transitioning. It adds another top bar, backdrop, and layout pass to an already expensive two-entry transition.
- Backdrop/offscreen layers must not be nested into a render tree that also consumes the same backdrop. That creates recursive `RenderNode` preparation on some Android devices and can crash native HWUI. Keep the root backdrop at the shell boundary and avoid wrapping child entry content with it again.
