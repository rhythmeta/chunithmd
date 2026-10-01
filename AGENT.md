# chunithmd Agent Memory

## Project direction

- `chunithmd` is the CHUNITHM counterpart of `maimaid` under the Rhythmeta ecosystem.
- The project is ready for UI and client implementation. The authoritative data sources are considered sufficiently settled for current feature work.
- Agents may freely create new Android UI files, including screens, components, navigation shells, and supporting presentation code, when implementing or restructuring the Android app.

## Client architecture

* Use Kotlin Multiplatform for shared business logic and platform-native UI.
* iOS UI is SwiftUI.
* Android UI is MIUIX.
* Shared code should contain domain models, business rules, calculations, validation, serialization, API contracts, use cases, and repository-facing logic where platform support is appropriate.
* Platform UI and genuinely platform-specific integrations remain native.

### Shared logic boundary

* Business logic must live in `commonMain` whenever it is platform-independent or can be expressed without platform-specific APIs.
* Android and iOS UI layers must not duplicate business rules that can be shared.
* Do not introduce business rules, domain calculations, validation rules, permission decisions, API request construction, or repository logic directly inside Android UI or iOS UI merely for convenience.
* When implementing an Android feature, first check whether the logic being added is reusable by iOS or other Android screens. If so, place it in shared code rather than the Android UI layer.
* Prefer shared use cases, domain services, and state holders over embedding reusable logic in platform UI.

### Native UI boundary

* Android UI is responsible for rendering state, collecting user input, navigation, Android-specific presentation behavior, and platform-specific integrations.
* iOS UI is responsible for rendering state, collecting user input, navigation, iOS-specific presentation behavior, and platform-specific integrations.
* UI-only transformations are allowed when they have no domain meaning and are not expected to be reused.
* Avoid duplicating state machines or business decisions in platform-specific UI.

### State and data flow

* Prefer unidirectional data flow:
  UI -> intent/action -> shared logic -> updated state -> UI.
* Shared logic should expose platform-independent state and operations where practical.
* Platform UI should observe shared state and translate user interaction into intents/actions rather than implementing business decisions locally.

### Platform-specific code

* Use `androidMain` for Android-specific implementations.
* Use `iosMain` for iOS-specific implementations.
* Use `expect`/`actual` only when a genuinely platform-specific capability is required.
* Do not move business logic into `androidMain` or `iosMain` solely because the current feature is being implemented on one platform.

### Android-focused implementation rule

* Even when working exclusively on the Android application, preserve the shared/native boundary.
* Before adding non-trivial logic to Android UI, determine whether it belongs in `commonMain`, `androidMain`, or the UI layer.
* If a piece of logic could reasonably be consumed by both Android and iOS, implement it in shared code.
* Do not assume that "Android-only task" means "Android-only business logic."

### Before implementation

Before implementation, explicitly choose:

* KMP targets
* Kotlin/Gradle baseline
* Supported iOS and Android versions
* Persistence strategy
* Networking and serialization libraries
* Shared state strategy
* Boundary between shared and native code

When modifying an existing feature, preserve these architectural boundaries unless there is a documented reason to change them.

## Online scope and deployment

- Rhythmeta accounts, community aliases and manual personal-data backups are approved for both games.
- The shared backend is https://github.com/rhythmeta/gekichumai-backend: Hono on Cloudflare Workers with native D1 SQL and the existing public R2 bucket. This supersedes the earlier Elysia proposal.
- The dashboard is https://github.com/rhythmeta/gekichumai-dashboard, deployed at https://dash.rhythmeta.org.
- APIs: https://api.rhythmeta.org/auth/v1 for accounts; `/maimaid/v1` and `/chunithmd/v1` for game-scoped resources. Legacy `/v1/*` returns 410.
- Existing account IDs, password credentials, MFA and passkeys are preserved. Sessions were invalidated at migration. Native login uses state + PKCE S256 and exact registered callbacks.
- Manual backups use shared `backup.proto`, gzip, SHA-256, 64 MiB compressed/512 MiB raw limits and the last three snapshots per user/game. Restore replaces personal data and uses a durable rollback journal. Static data and credentials are excluded.
- KMP `commonMain` owns authentication, network requests, serialization, validation and backup/recovery coordination. Android owns Room/DataStore/Keystore adapters; iOS owns Keychain/protected file adapters and SwiftUI. The current iOS catalog shell preserves complete imported snapshots until personal-data UI is added.
- Each game repository owns static catalog publication and emits a public `community-index.json`; backend services do not build catalogs.
- Independent backend/dashboard validation and deployment workflows own their releases. Cloudflare deployment credentials are organization Actions secrets.

## UI file creation rule

- When implementing an Android UI feature, create new files such as `Screen.kt`, `Components.kt`, or feature-specific component files when that produces a clearer structure. Do not avoid creating a new file merely because an existing file could technically contain the code.
- Keep Android presentation code in the UI layer and reusable business logic in the appropriate shared/native layer according to the boundaries above.

## Validation workflow

- After Android navigation or animation changes, Codex should compile the Android app and stop. The user performs device installation, interaction testing, and visual acceptance.

## Android navigation lessons

- Root tabs are peers, so they stay inside one root navigation entry. Switching tabs is a horizontal translation between two adjacent pages; root tabs should not be modeled as stacked `NavDisplay` destinations.
- Child screens use the navigation stack and the normal Miuix page transition. Keep root back behavior separate: a non-home root returns to home, while home falls through to the system exit behavior.
- Predictive back must keep the source and destination pages adjacent. The destination layer starts outside the viewport and moves in with the gesture; do not leave a full destination page at `x = 0` underneath the source page.
- Do not keep a hidden full root `Scaffold` alive while a child destination is transitioning. It adds another top bar, backdrop, and layout pass to an already expensive two-entry transition.
- Backdrop/offscreen layers must not be nested into a render tree that also consumes the same backdrop. That creates recursive `RenderNode` preparation on some Android devices and can crash native HWUI. Keep the root backdrop at the shell boundary and avoid wrapping child entry content with it again.

## Android UI title behavior

- Every Android page that uses a large title must also collapse it to the small title when the page content scrolls. Use Miuix `TopAppBar` with both `title` and `largeTitle`, create a `MiuixScrollBehavior`, pass it to the app bar, and attach the same `scrollBehavior.nestedScrollConnection` to the page's scrollable content (for example, `LazyColumn`). Follow the local `miuix-doc` `TopAppBar` and `Scaffold` examples.

## Android top bar blur

- Every Android top bar must apply the configured blur when the theme's blur setting is enabled and the platform supports it. Keep the top bar connected to that page's backdrop, and use the opaque page surface when blur is disabled or unavailable.
