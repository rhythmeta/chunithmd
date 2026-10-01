import { createHash } from "node:crypto";
import { mkdir, readFile, rm, stat, writeFile } from "node:fs/promises";
import path from "node:path";
import { canonicalJson } from "./canonical-json.js";
import { mergeSources, validateLxnsAliasList, validateLxnsSongList, validatePrimaryData } from "./merge.js";
import type { BuildManifest, JsonValue, MappingReport } from "./types.js";

export const DEFAULT_PRIMARY_URL = "https://dp4p6x0xfi5o9.cloudfront.net/chunithm/data.json";
export const DEFAULT_LXNS_CATALOG_URL = "https://maimai.lxns.net/api/v0/chunithm/song/list?notes=true";
export const DEFAULT_LXNS_ALIAS_URL = "https://maimai.lxns.net/api/v0/chunithm/alias/list";
export const DEFAULT_IMAGE_BASE_URL = "https://dp4p6x0xfi5o9.cloudfront.net/chunithm/img/cover/";
export const DEFAULT_STATIC_BASE_URL = "https://chunithmd-assets.rhythmeta.org";

const sha256 = (bytes: Uint8Array) => createHash("sha256").update(bytes).digest("hex");

export const fetchJson = async (url: string, timeoutMs = 60_000): Promise<{ value: unknown; bytes: Uint8Array; hash: string }> => {
	const response = await fetch(url, { headers: { accept: "application/json", "user-agent": "chunithmd-static-bundle-builder/1" }, signal: AbortSignal.timeout(timeoutMs) });
	if (!response.ok) throw new Error(`${url} returned HTTP ${response.status}`);
	const bytes = new Uint8Array(await response.arrayBuffer());
	if (bytes.byteLength === 0) throw new Error(`${url} returned an empty response`);
	let value: unknown;
	try {
		value = JSON.parse(new TextDecoder().decode(bytes));
	} catch {
		throw new Error(`${url} returned invalid JSON`);
	}
	return { value, bytes, hash: sha256(bytes) };
};

const downloadImage = async (url: string, timeoutMs = 60_000): Promise<Uint8Array> => {
	const response = await fetch(url, { headers: { accept: "image/*", "user-agent": "chunithmd-static-bundle-builder/1" }, signal: AbortSignal.timeout(timeoutMs) });
	if (!response.ok) throw new Error(`${url} returned HTTP ${response.status}`);
	const contentType = response.headers.get("content-type")?.toLocaleLowerCase() ?? "";
	if (!contentType.startsWith("image/")) throw new Error(`${url} returned non-image content type ${contentType || "unknown"}`);
	const bytes = new Uint8Array(await response.arrayBuffer());
	if (bytes.byteLength === 0) throw new Error(`${url} returned an empty image`);
	return bytes;
};

export type BuildOptions = {
	outputDirectory: string;
	artifactDirectory: string;
	primaryUrl?: string;
	lxnsCatalogUrl?: string;
	lxnsAliasUrl?: string;
	imageBaseUrl?: string;
	staticBaseUrl?: string;
	createdAt?: string;
	downloadImages?: boolean;
};

export const buildStaticBundle = async (options: BuildOptions) => {
	const primaryUrl = options.primaryUrl ?? DEFAULT_PRIMARY_URL;
	const lxnsCatalogUrl = options.lxnsCatalogUrl ?? DEFAULT_LXNS_CATALOG_URL;
	const lxnsAliasUrl = options.lxnsAliasUrl ?? DEFAULT_LXNS_ALIAS_URL;
	const imageBaseUrl = (options.imageBaseUrl ?? DEFAULT_IMAGE_BASE_URL).replace(/\/+$/u, "/");
	const staticBaseUrl = (options.staticBaseUrl ?? DEFAULT_STATIC_BASE_URL).replace(/\/+$/u, "");
	const [primary, lxnsCatalog, lxnsAliases] = await Promise.all([
		fetchJson(primaryUrl),
		fetchJson(lxnsCatalogUrl),
		fetchJson(lxnsAliasUrl),
	]);
	const primaryValue = validatePrimaryData(primary.value);
	const lxnsCatalogValue = validateLxnsSongList(lxnsCatalog.value);
	const lxnsAliasesValue = validateLxnsAliasList(lxnsAliases.value);
	const { payload, report } = mergeSources(primaryValue, lxnsCatalogValue, lxnsAliasesValue);
	if (report.ambiguousSongCount > 0) throw new Error("Bundle contains ambiguous song matches.");

	const bundleBytes = new TextEncoder().encode(canonicalJson(payload as unknown as JsonValue));
	const bundleHash = sha256(bundleBytes);
	const bundleVersion = `bundle-${bundleHash.slice(0, 16)}`;
	const bundlePath = `/bundles/${bundleHash}.json`;
	const createdAt = options.createdAt ?? new Date().toISOString();
	const manifest: BuildManifest = {
		schemaVersion: 1,
		product: "chunithmd",
		version: bundleVersion,
		sha256: bundleHash,
		bundle: bundlePath,
		createdAt,
		sources: {
			primary: { url: primaryUrl, contentSha256: primary.hash, ...(primaryValue.updateTime ? { updateTime: primaryValue.updateTime } : {}) },
			lxnsCatalog: { url: lxnsCatalogUrl, contentSha256: lxnsCatalog.hash },
			lxnsAliases: { url: lxnsAliasUrl, contentSha256: lxnsAliases.hash },
		},
		mapping: {
			primarySongCount: report.primarySongCount,
			lxnsSongCount: report.lxnsSongCount,
			matchedSongCount: report.matchedSongCount,
			unmatchedSongCount: report.unmatchedSongCount,
			ambiguousSongCount: report.ambiguousSongCount,
			unmatchedAliases: report.unmatchedAliases,
			matchedAliasSongCount: report.matchedAliasSongCount,
			cnChartMatchCount: report.cnChartMatchCount,
			cnChartMissingCount: report.cnChartMissingCount,
		},
		assets: { jacketBaseUrl: `${staticBaseUrl}/jackets/` },
	};

	await rm(options.outputDirectory, { recursive: true, force: true });
	await mkdir(path.join(options.outputDirectory, "bundles"), { recursive: true });
	await mkdir(path.join(options.outputDirectory, "jackets"), { recursive: true });
	await writeFile(path.join(options.outputDirectory, "manifest.json"), `${JSON.stringify(manifest)}\n`);
	await writeFile(path.join(options.outputDirectory, bundlePath.slice(1)), bundleBytes);
 await writeFile(path.join(options.outputDirectory, "community-index.json"), JSON.stringify({
  schemaVersion: 1, game: "chunithmd",
  songs: Object.fromEntries(payload.catalog.songs.map(song => [song.songId, payload.aliases[song.songId] ?? []])),
  catalog: payload.catalog.songs.map(song => ({songIdentifier: song.songId, title: song.title, artist: song.artist, coverUrl: `${staticBaseUrl}/jackets/${encodeURIComponent(song.imageName)}`})),
 }));
 await writeFile(path.join(options.outputDirectory, "_headers"), "/*\n  Access-Control-Allow-Origin: *\n/community-index.json\n  Cache-Control: public, max-age=300\n");

	if (options.downloadImages !== false) {
		const imageNames = Array.from(new Set(primaryValue.songs.map((song) => song.imageName))).filter(Boolean);
		let cursor = 0;
		const worker = async () => {
			while (cursor < imageNames.length) {
				const imageName = imageNames[cursor++];
				if (!imageName) continue;
				const bytes = await downloadImage(`${imageBaseUrl}${encodeURIComponent(imageName)}`);
				await writeFile(path.join(options.outputDirectory, "jackets", imageName), bytes);
			}
		};
		await Promise.all(Array.from({ length: Math.min(12, imageNames.length) }, worker));
	}

	const fullReport = {
		...report,
		bundle: { version: bundleVersion, sha256: bundleHash, byteLength: bundleBytes.byteLength },
		manifest,
		generatedAt: createdAt,
	};
	await mkdir(options.artifactDirectory, { recursive: true });
	await writeFile(path.join(options.artifactDirectory, "static-bundle-report.json"), `${JSON.stringify(fullReport, null, 2)}\n`);
	return { manifest, report, bundleBytes };
};

export const verifyOutput = async (outputDirectory: string) => {
	const manifest = JSON.parse(await readFile(path.join(outputDirectory, "manifest.json"), "utf8")) as BuildManifest;
	const bundle = await readFile(path.join(outputDirectory, manifest.bundle.slice(1)));
	if (sha256(bundle) !== manifest.sha256) throw new Error("Published bundle hash does not match manifest.");
	const imageBasePath = path.join(outputDirectory, "jackets");
	const imageDirectory = await stat(imageBasePath);
	if (!imageDirectory.isDirectory()) throw new Error("Jacket asset directory is missing.");
	const payload = JSON.parse(new TextDecoder().decode(bundle)) as { catalog?: { songs?: Array<{ imageName?: string }> } };
	const imageNames = Array.from(new Set(
		(payload.catalog?.songs?.map((song) => song.imageName) ?? []).filter((imageName): imageName is string => Boolean(imageName)),
	));
	for (const imageName of imageNames) {
		const image = await stat(path.join(imageBasePath, imageName));
		if (!image.isFile() || image.size === 0) throw new Error(`Jacket asset is missing or empty: ${imageName}`);
	}
	return manifest;
};
