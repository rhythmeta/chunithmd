import path from "node:path";
import { fileURLToPath } from "node:url";
import { buildStaticBundle, verifyOutput } from "../src/static-bundle/build.js";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const outputDirectory = process.env.STATIC_BUNDLE_OUTPUT ?? path.join(root, "static-worker", "public");
const artifactDirectory = process.env.STATIC_BUNDLE_ARTIFACTS ?? path.join(root, "artifacts");

const buildOptions = {
	outputDirectory,
	artifactDirectory,
	...(process.env.CHUNITHMD_PRIMARY_URL ? { primaryUrl: process.env.CHUNITHMD_PRIMARY_URL } : {}),
	...(process.env.CHUNITHMD_LXNS_CATALOG_URL ? { lxnsCatalogUrl: process.env.CHUNITHMD_LXNS_CATALOG_URL } : {}),
	...(process.env.CHUNITHMD_LXNS_ALIAS_URL ? { lxnsAliasUrl: process.env.CHUNITHMD_LXNS_ALIAS_URL } : {}),
	...(process.env.CHUNITHMD_IMAGE_BASE_URL ? { imageBaseUrl: process.env.CHUNITHMD_IMAGE_BASE_URL } : {}),
	...(process.env.CHUNITHMD_STATIC_BASE_URL ? { staticBaseUrl: process.env.CHUNITHMD_STATIC_BASE_URL } : {}),
};

const result = await buildStaticBundle(buildOptions);
await verifyOutput(outputDirectory);
console.log(`[static-bundle] wrote ${result.manifest.bundle} (${result.manifest.sha256})`);
console.log(`[static-bundle] matched ${result.report.matchedSongCount}/${result.report.primarySongCount} songs`);
