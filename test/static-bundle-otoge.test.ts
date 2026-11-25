import assert from "node:assert/strict";
import { test } from "node:test";
import { mergeSources } from "../src/static-bundle/merge.js";
import { enrichFromOtoge, validateOtogeSongs } from "../src/static-bundle/otoge.js";
import type { PrimaryData, PrimarySheet, PrimarySong } from "../src/static-bundle/types.js";

const master: PrimarySheet = {
	type: "std", difficulty: "master", level: "14", levelValue: 14,
	internalLevel: null, internalLevelValue: 14, regions: { jp: true, intl: false },
};
const ultima = { ...master, difficulty: "ultima" };
const we = { type: "we", difficulty: "【狂】", level: "☆☆☆", levelValue: 103, regions: { jp: false, intl: false } };
const song = (songId: string, ...sheets: PrimarySheet[]): PrimarySong => ({
	songId, title: "Ｔｅｓｔ", artist: "Artist", category: "VARIETY", bpm: null,
	imageName: "a.png", version: null, releaseDate: null, sheets,
});
const primary = (...songs: PrimarySong[]): PrimaryData => ({
	categories: [], difficulties: [], types: [], regions: [], versions: [{ version: "SUN PLUS", abbr: "SUN+" }], songs,
});
const row = { id: "42", title: "test", artist: "artist", we_kanji: "", we_star: "", intl: "1" };

test("fills shared metadata and explicit constants on existing charts while retaining CN levels and aliases", () => {
	const base = primary(song("original", master, ultima));
	const jp = { ...row, lev_mas: "14", lev_mas_i: "14.4", lev_ult: "14", date_added: "20240229", version: "SUN+" };
	const intl = { ...row, lev_mas: "14", lev_mas_i: "14.2", lev_mas_notes: "100", lev_mas_designer: "Designer", bpm: "200" };
	const enriched = enrichFromOtoge(base, [jp, { ...jp, id: "99", title: "Extra" }], [intl]);
	const result = mergeSources(enriched.primary, {
		genres: [], versions: [], songs: [{ id: 7, title: "Test", difficulties: [{ difficulty: 3, level: "13+", level_value: 13.8 }] }],
	}, { aliases: [{ song_id: 7, aliases: ["alias"] }] });
	const [merged] = result.payload.catalog.songs;
	assert.ok(merged);
	assert.equal(result.payload.catalog.songs.length, 1);
	assert.deepEqual(merged.sheets.map(s => s.difficulty), ["master", "ultima"]);
	assert.deepEqual([merged.songId, merged.bpm, merged.version, merged.releaseDate], ["original", 200, "SUN PLUS", "2024-02-29"]);
	assert.deepEqual(merged.sheets[0], { ...master, regions: { jp: true, intl: true }, internalLevel: "14.4", internalLevelValue: 14.4, noteDesigner: "Designer", noteCounts: { total: 100 } });
	assert.equal(merged.sheets[1]?.regions?.intl, false);
	assert.deepEqual(merged.regionOverrides.cn.charts["std:master"], { available: true, level: "13+", levelValue: 13.8 });
	assert.deepEqual(result.payload.aliases, { original: ["alias"] });
	assert.deepEqual(base.songs[0]?.sheets[0], master);
	assert.equal(enriched.report.intl.conflicts[0]?.field, "internalLevel");
});

test("regional availability is additive and independent of shared metadata and regional levels", () => {
	const oldMaster = { ...master, regions: { jp: false, intl: true } };
	const jp = { ...row, intl: "0", lev_mas: "14", lev_mas_designer: "shared", lev_ult: "14" };
	const result = enrichFromOtoge(primary(song("original", oldMaster, ultima)), [jp], [{ ...row, lev_ult: "13", lev_ult_i: "13.2" }]);
	const [standard, ult] = result.primary.songs[0]!.sheets;
	assert.equal(standard?.noteDesigner, "shared");
	assert.deepEqual(standard?.regions, { jp: false, intl: true });
	assert.equal(ult?.regions?.intl, true);
	assert.equal(ult?.internalLevel, null);
	assert.equal(result.report.intl.addedIntlCharts, 1);
});

test("matches WE kanji and encoded stars, skipping duplicates on either side", () => {
	const base = primary(song("std", master), song("we", we), song("we2", { ...we, level: "☆☆" }));
	const world = { ...row, we_kanji: "狂", we_star: "5", lev_we_notes: "1922", lev_we_designer: "WE designer" };
	const result = enrichFromOtoge(base, [world], []);
	assert.equal(result.report.jp.matchedWorldsEndCharts, 1);
	assert.deepEqual(result.primary.songs[1]?.sheets[0], { ...we, regions: { jp: false, intl: true }, noteCounts: { total: 1922 }, noteDesigner: "WE designer" });
	assert.deepEqual(result.primary.songs[2], base.songs[2]);
	assert.deepEqual(result.primary.songs[0], base.songs[0]);

	const duplicateSource = enrichFromOtoge(base, [world, world], []);
	assert.deepEqual(duplicateSource.primary, base);
	assert.equal(duplicateSource.report.jp.ambiguous.length, 1);
	const duplicateTargets = enrichFromOtoge(primary(song("one", we), song("two", we)), [], [world]);
	assert.deepEqual(duplicateTargets.report.intl.ambiguous[0]?.songIds, ["one", "two"]);
	assert.equal(duplicateTargets.report.intl.addedIntlCharts, 0);
});

test("retains explicit constants, zero note counts and existing data when note totals conflict", () => {
	const base = primary(song("original", { ...master, internalLevel: "14.0", noteCounts: { flick: 0 } }));
	const jp = { ...row, lev_mas: "14", lev_mas_i: "14.2", lev_mas_notes: "10", lev_mas_notes_flick: "1" };
	const intl = { ...jp, date_added: "20260230", lev_mas_notes_flick: "0", lev_mas_notes_tap: "12" };
	const result = enrichFromOtoge(base, [jp], [intl]);
	assert.deepEqual(result.primary.songs[0]?.sheets[0]?.noteCounts, { flick: 0 });
	assert.equal(result.primary.songs[0]?.sheets[0]?.internalLevelValue, 14);
	assert.equal(result.primary.songs[0]?.releaseDate, null);
	assert.deepEqual(result.report.jp.conflicts.map(c => c.field), ["internalLevel", "noteCounts.flick"]);
	assert.equal(result.report.intl.inconsistentNotes.length, 1);
});

test("validates source identity fields and ignores missing optional values", () => {
	assert.throws(() => validateOtogeSongs({ songs: [] }));
	assert.throws(() => validateOtogeSongs([{ ...row, we_kanji: null }]));
	assert.deepEqual(validateOtogeSongs([{ ...row, bpm: null }]), [{ ...row, bpm: null }]);
});
