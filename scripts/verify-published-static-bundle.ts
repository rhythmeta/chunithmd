import { createHash } from "node:crypto";

const baseUrl = (process.env.CHUNITHMD_STATIC_BASE_URL ?? "https://chunithmd-assets.rhythmeta.org").replace(/\/+$/u, "");
const fetchRequired = async (url: string, accept: string) => {
	const response = await fetch(url, { headers: { accept, "user-agent": "chunithmd-static-bundle-verifier/1" } });
	if (!response.ok) throw new Error(`${url} returned HTTP ${response.status}`);
	return response;
};

const manifestResponse = await fetchRequired(`${baseUrl}/manifest.json`, "application/json");
const manifest = (await manifestResponse.json()) as {
	schemaVersion: number;
	product: string;
	sha256: string;
	bundle: string;
};
if (manifest.schemaVersion !== 1 || manifest.product !== "chunithmd") throw new Error("Published manifest has an invalid product or schema.");
const bundleResponse = await fetchRequired(new URL(manifest.bundle, `${baseUrl}/`).toString(), "application/json");
const bundleBytes = new Uint8Array(await bundleResponse.arrayBuffer());
const bundleHash = createHash("sha256").update(bundleBytes).digest("hex");
if (bundleHash !== manifest.sha256) throw new Error(`Published bundle hash mismatch: ${bundleHash} != ${manifest.sha256}`);
const bundle = JSON.parse(new TextDecoder().decode(bundleBytes)) as { schemaVersion: number; catalog?: { songs?: Array<{ imageName?: string }> } };
if (bundle.schemaVersion !== 1 || !bundle.catalog?.songs?.length) throw new Error("Published bundle has an invalid catalog.");
const imageNames = Array.from(new Set(bundle.catalog.songs.map((song) => song.imageName).filter((name): name is string => Boolean(name))));
if (imageNames.length === 0) throw new Error("Published bundle has no jacket image names.");
let cursor = 0;
const verifyWorker = async () => {
	while (cursor < imageNames.length) {
		const imageName = imageNames[cursor++];
		if (!imageName) continue;
		await fetchRequired(`${baseUrl}/jackets/${encodeURIComponent(imageName)}`, "image/*");
	}
};
await Promise.all(Array.from({ length: Math.min(16, imageNames.length) }, verifyWorker));
console.log(`[static-bundle] public manifest, bundle, and ${imageNames.length} jackets verified (${manifest.sha256})`);
