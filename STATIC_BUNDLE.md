# CHUNITHMD Static Bundle

The builder is independent of the Elysia service. It fetches the CloudFront
catalog, the LXNS CHUNITHM catalog, and the LXNS alias list, then writes an
atomic static tree under `static-worker/public`.

```bash
pnpm install
pnpm test
pnpm run typecheck
pnpm run build:static-bundle
pnpm run verify:static-bundle
```

The build requires network access to the three configured sources. Override
the defaults with `CHUNITHMD_PRIMARY_URL`, `CHUNITHMD_LXNS_CATALOG_URL`,
`CHUNITHMD_LXNS_ALIAS_URL`, `CHUNITHMD_IMAGE_BASE_URL`, and
`CHUNITHMD_STATIC_BASE_URL`. The output contains `manifest.json`, a
content-addressed bundle under `bundles/`, and mirrored jacket images under
`jackets/`. The report is written to `artifacts/static-bundle-report.json`.

The primary `songId` is the stable client identifier. LXNS title matching is
Unicode NFKC, trimmed, case-folded, and whitespace-collapsed. Ambiguous matches
are first disambiguated by original title, artist, and chart type; any remaining
ambiguity fails the build. Unmatched songs remain in the base catalog but are
unavailable in the CN region. CN levels and availability are stored in
`regionOverrides.cn`; JP and international values remain from the primary
source. The bundle has a top-level `schemaVersion` of `1`.

The verification command reads the deployed manifest and bundle, recomputes
the bundle SHA-256, validates the catalog schema, and fetches one jacket asset.
