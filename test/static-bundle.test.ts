import assert from "node:assert/strict";
import { test } from "node:test";
import { canonicalJson } from "../src/static-bundle/canonical-json.js";
import { mergeSources } from "../src/static-bundle/merge.js";
import type { LxnsAliasList, LxnsSongList, PrimaryData } from "../src/static-bundle/types.js";

const primary = (overrides: Partial<PrimaryData> = {}): PrimaryData => ({
	categories: [], difficulties: [], regions: [{ region: "jp" }, { region: "intl" }], types: [], versions: [],
	songs: [{ songId: "song-a", title: "  Ｔｅｓｔ  Song ", artist: "artist", category: "VARIETY", bpm: 120, imageName: "a.png", version: "CHUNITHM", releaseDate: null, sheets: [
		{ type: "std", difficulty: "master", level: "12", levelValue: 12, regions: { jp: true, intl: true } },
		{ type: "we", difficulty: "【狂】", level: "☆", levelValue: 101, regions: { jp: true, intl: true } },
	] }], ...overrides,
});

const lxns: LxnsSongList = {
	genres: [], versions: [], songs: [
		{ id: 10, title: "Test Song", artist: "artist", difficulties: [{ difficulty: 3, level: "13", level_value: 13.4 }] },
		{ id: 20, title: "Test Song WE", artist: "artist", difficulties: [{ difficulty: 5, level: "0", level_value: 0, origin_id: 10, kanji: "狂" }] },
	],
};

test("canonical JSON sorts object keys without changing array order", () => {
	assert.equal(canonicalJson({ z: 1, a: { y: 2, x: 3 }, list: [2, 1] }), '{"a":{"x":3,"y":2},"list":[2,1],"z":1}');
});

test("merges normalized titles, CN constants, and WORLD'S END origin records", () => {
	const result = mergeSources(primary(), lxns, { aliases: [{ song_id: 10, aliases: [" test", "test"] }] });
	const song = result.payload.catalog.songs[0];
	assert.ok(song);
	assert.equal(result.report.matchedSongCount, 1);
	assert.equal(song.regionOverrides.cn.available, true);
	assert.deepEqual(song.regionOverrides.cn.charts["std:master"], { available: true, level: "13", levelValue: 13.4 });
	assert.deepEqual(song.regionOverrides.cn.charts["we:【狂】"], { available: true, level: "0", levelValue: 0 });
	assert.deepEqual(result.payload.aliases["song-a"], ["test"]);
});

test("unmatched primary songs remain available in the base catalog but unavailable in CN", () => {
	const baseSong = primary().songs[0];
	assert.ok(baseSong);
	const result = mergeSources(primary({ songs: [{ ...baseSong, title: "Missing" }] }), { ...lxns, songs: [] }, { aliases: [] } satisfies LxnsAliasList);
	assert.equal(result.report.unmatchedSongCount, 1);
	assert.equal(result.payload.catalog.songs[0]?.regionOverrides.cn.available, false);
});

test("uses original title and artist to disambiguate case-folded titles", () => {
	const base = primary();
	const result = mergeSources(
		primary({ songs: [{ ...base.songs[0]!, title: "FLOWER", artist: "DJ YOSHITAKA" }] }),
		{
			genres: [],
			versions: [],
			songs: [
				{ id: 1, title: "FLOWER", artist: "DJ YOSHITAKA", difficulties: [{ difficulty: 3, level: "13", level_value: 13 }] },
				{ id: 2, title: "Flower", artist: "Other Artist", difficulties: [{ difficulty: 3, level: "12", level_value: 12 }] },
			],
		},
		{ aliases: [] },
	);
	assert.equal(result.report.matchedSongCount, 1);
});
