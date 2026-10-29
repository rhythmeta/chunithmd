import assert from "node:assert/strict";
import { mkdtemp, mkdir, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { test } from "node:test";
import { measureJacketBytes } from "../src/static-bundle/build.js";

const withJackets = async (files: Record<string, number>, run: (directory: string) => Promise<void>) => {
	const directory = await mkdtemp(path.join(tmpdir(), "chunithmd-sizes-"));
	try {
		await mkdir(path.join(directory, "jackets"), { recursive: true });
		for (const [name, size] of Object.entries(files)) {
			await writeFile(path.join(directory, "jackets", name), Buffer.alloc(size, 1));
		}
		await run(directory);
	} finally {
		await rm(directory, { recursive: true, force: true });
	}
};

test("sums the on-disk size of every published jacket", async () => {
	await withJackets({ "a.png": 120, "b.png": 30 }, async (directory) => {
		assert.equal(await measureJacketBytes(directory, ["a.png", "b.png"]), 150);
	});
});

test("reports no total when a jacket is missing or empty", async () => {
	await withJackets({ "a.png": 120, "empty.png": 0 }, async (directory) => {
		assert.equal(await measureJacketBytes(directory, ["a.png", "missing.png"]), null);
		assert.equal(await measureJacketBytes(directory, ["a.png", "empty.png"]), null);
	});
});

test("a catalog without jackets still yields a measurable total", async () => {
	await withJackets({}, async (directory) => {
		assert.equal(await measureJacketBytes(directory, []), 0);
	});
});
