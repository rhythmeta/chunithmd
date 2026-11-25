import { chartKey, normalizeTitle, normalizeWorldEndName } from "./normalize.js";
import type { JsonObject, PrimaryData, PrimarySheet, PrimarySong } from "./types.js";

type OtogeRow = Record<string, string | null | undefined> & { id: string; title: string; artist: string; we_kanji: string; we_star: string };

export function validateOtogeSongs(value: unknown): OtogeRow[] {
	if (!Array.isArray(value) || !value.length) throw new Error("OTOGE music-ex must be a nonempty array.");
	for (const [index, row] of value.entries()) {
		if (!row || typeof row !== "object" || !["id", "title", "artist", "we_kanji", "we_star"].every(key => typeof row[key] === "string")
			|| !row.id.trim() || !row.title.trim() || !Object.values(row).every(v => v == null || typeof v === "string")) {
			throw new Error(`Invalid OTOGE song at index ${index}.`);
		}
	}
	return value;
}

const standardPrefixes: Record<string, string> = { basic: "lev_bas", advanced: "lev_adv", expert: "lev_exp", master: "lev_mas", ultima: "lev_ult" };
const stars: Record<string, string> = { "1": "☆", "3": "☆☆", "5": "☆☆☆", "7": "☆☆☆☆", "9": "☆☆☆☆☆" };
const versionNames: Record<string, string> = { "無印": "CHUNITHM", PLUS: "CHUNITHM PLUS", "PARADISE×": "PARADISE LOST" };

type Target = { song: PrimarySong; sheets: PrimarySheet[] };
type Conflict = { songId: string; otogeId: string; chart?: string; field: string; existing: string | number; otoge: string | number };
type OtogeReport = {
	matchedStandardSongs: number;
	matchedWorldsEndCharts: number;
	addedIntlCharts: number;
	filled: { song: Record<string, number>; std: Record<string, number>; we: Record<string, number> };
	ambiguous: Array<{ songIds: string[]; otogeIds: string[] }>;
	conflicts: Conflict[];
	inconsistentNotes: Array<{ songId: string; otogeId: string; chart: string; counts: JsonObject }>;
};

function matchKey(song: { title: string; artist: string }, ...chart: string[]) {
	return JSON.stringify([normalizeTitle(song.title), normalizeTitle(song.artist), ...chart]);
}

function addToIndex<T>(index: Map<string, T[]>, key: string, entry: T) {
	const existing = index.get(key);
	if (existing) existing.push(entry);
	else index.set(key, [entry]);
}

function numeric(value: string | null | undefined): number | null {
	if (!value || !/^\d+(?:\.\d+)?$/u.test(value)) return null;
	const number = Number(value);
	return Number.isFinite(number) ? number : null;
}

function chartConstant(raw: string | null | undefined, level: string): number | null {
	if (!raw || !/^\d+(?:\.\d)?$/u.test(raw)) return null;
	const value = Number(raw);
	const displayed = `${Math.floor(value)}${value % 1 >= 0.5 ? "+" : ""}`;
	return value > 0 && displayed === level ? value : null;
}

function releaseDate(raw: string | null | undefined): string | null {
	if (!raw || !/^\d{8}$/u.test(raw)) return null;
	const iso = `${raw.slice(0, 4)}-${raw.slice(4, 6)}-${raw.slice(6)}`;
	const date = new Date(iso);
	return Number.isFinite(date.getTime()) && date.toISOString().startsWith(iso) ? iso : null;
}

type Fill = (field: string, current: string | number | null | undefined, candidate: string | number | null | undefined) => boolean;

function fieldMerger(report: OtogeReport, song: PrimarySong, row: OtogeRow, sheet?: PrimarySheet): Fill {
	const fills = sheet ? report.filled[sheet.type === "we" ? "we" : "std"] : report.filled.song;
	return (field, current, candidate) => {
		if (candidate == null || candidate === "" || candidate === "-") return false;
		if (current == null || current === "" || current === "-") {
			fills[field] = (fills[field] ?? 0) + 1;
			return true;
		}
		if (normalizeTitle(String(current)) !== normalizeTitle(String(candidate))) {
			report.conflicts.push({
				songId: song.songId, otogeId: row.id, ...(sheet && { chart: chartKey(sheet.type, sheet.difficulty) }),
				field, existing: current, otoge: candidate,
			});
		}
		return false;
	};
}

const noteTypes = ["tap", "hold", "slide", "air", "flick"] as const;

function mergeNotes(song: PrimarySong, sheet: PrimarySheet, row: OtogeRow, prefix: string, report: OtogeReport) {
	const current = sheet.noteCounts ?? {};
	const proposed = { ...current };
	const missing: string[] = [];
	let conflict = false;
	const chart = chartKey(sheet.type, sheet.difficulty);
	for (const key of [...noteTypes, "total"]) {
		const value = numeric(row[key === "total" ? prefix : `${prefix}_${key}`]);
		if (value === null || !Number.isInteger(value) || value > 2_147_483_647) continue;
		if (current[key] == null) {
			proposed[key] = value;
			missing.push(key);
		} else if (current[key] !== value) {
			report.conflicts.push({ songId: song.songId, otogeId: row.id, chart, field: `noteCounts.${key}`, existing: String(current[key]), otoge: value });
			conflict = true;
		}
	}
	if (conflict || !missing.length) return;
	const parts = noteTypes.map(key => proposed[key]).filter((v): v is number => typeof v === "number");
	const sum = parts.reduce((acc, n) => acc + n, 0);
	if (typeof proposed.total === "number" && (sum > proposed.total || (parts.length === noteTypes.length && sum !== proposed.total))) {
		report.inconsistentNotes.push({ songId: song.songId, otogeId: row.id, chart, counts: proposed });
		return;
	}
	sheet.noteCounts = proposed;
	const fills = sheet.type === "we" ? report.filled.we : report.filled.std;
	for (const key of missing) {
		const field = `noteCounts.${key}`;
		fills[field] = (fills[field] ?? 0) + 1;
	}
}

function enrichSheet(song: PrimarySong, sheet: PrimarySheet, row: OtogeRow, region: "jp" | "intl", report: OtogeReport) {
	const fill = fieldMerger(report, song, row, sheet);
	const prefix = sheet.type === "we" ? "lev_we" : standardPrefixes[sheet.difficulty];
	if (!prefix) return;
	// ULTIMA can debut later in INTL than the four standard difficulties.
	const listedInIntl = sheet.type === "we" || (row[prefix] && (sheet.difficulty !== "ultima" || region === "intl"));
	if (row.intl === "1" && listedInIntl && !sheet.regions?.intl) {
		sheet.regions = { ...sheet.regions, intl: true };
		report.addedIntlCharts++;
	}
	if (sheet.type === "std") {
		const level = row[prefix];
		if (!level) return;
		if (sheet.level !== level) {
			report.conflicts.push({ songId: song.songId, otogeId: row.id, chart: chartKey(sheet.type, sheet.difficulty), field: "level", existing: sheet.level, otoge: level });
			return;
		}
		const constant = chartConstant(row[`${prefix}_i`], level);
		// When the explicit constant is missing, the main source uses the displayed level as a numeric fallback.
		const explicit = numeric(sheet.internalLevel) ?? sheet.internalLevel;
		if (constant !== null && fill("internalLevel", explicit, constant)) {
			sheet.internalLevel = constant.toFixed(1);
			sheet.internalLevelValue = constant;
		}
	}
	const designer = row[`${prefix}_designer`]?.trim();
	if (designer && fill("noteDesigner", sheet.noteDesigner, designer)) sheet.noteDesigner = designer;
	mergeNotes(song, sheet, row, `${prefix}_notes`, report);
}

export function enrichFromOtoge(primary: PrimaryData, jp: OtogeRow[], intl: OtogeRow[]) {
	const songs = structuredClone(primary.songs);
	const targets = new Map<string, Target[]>();
	for (const song of songs) {
		const standard = song.sheets.filter(sheet => sheet.type === "std");
		if (standard.length) addToIndex(targets, matchKey(song, "std"), { song, sheets: standard });
		for (const sheet of song.sheets.filter(sheet => sheet.type === "we")) {
			addToIndex(targets, matchKey(song, "we", normalizeWorldEndName(sheet.difficulty), sheet.level), { song, sheets: [sheet] });
		}
	}
	const versions = new Map<string, string>();
	for (const { version, abbr } of primary.versions) {
		if (typeof version !== "string") continue;
		versions.set(version, version);
		if (typeof abbr === "string") versions.set(abbr, version);
	}
	const report = {
		jp: applyRows(targets, versions, jp, "jp"),
		intl: applyRows(targets, versions, intl, "intl"),
	};
	return { primary: { ...primary, songs }, report };
}

function applyRows(targets: Map<string, Target[]>, versions: Map<string, string>, rows: OtogeRow[], region: "jp" | "intl") {
	const bySong = new Map<string, OtogeRow[]>();
	for (const row of rows) {
		if (row.we_kanji) {
			const level = stars[row.we_star];
			if (level) addToIndex(bySong, matchKey(row, "we", normalizeWorldEndName(row.we_kanji), level), row);
		} else {
			addToIndex(bySong, matchKey(row, "std"), row);
		}
	}
	const report: OtogeReport = {
		matchedStandardSongs: 0, matchedWorldsEndCharts: 0, addedIntlCharts: 0,
		filled: { song: {}, std: {}, we: {} }, ambiguous: [], conflicts: [], inconsistentNotes: [],
	};
	for (const [key, matches] of targets) {
		const candidates = bySong.get(key);
		if (!candidates) continue;
		if (matches.length !== 1 || candidates.length !== 1) {
			report.ambiguous.push({ songIds: matches.map(m => m.song.songId), otogeIds: candidates.map(r => r.id) });
			continue;
		}
		const { song, sheets } = matches[0]!;
		const row = candidates[0]!;
		if (row.we_kanji) report.matchedWorldsEndCharts++;
		else report.matchedStandardSongs++;
		// A WE row describes that chart's debut. Only use it for song metadata on a single-chart WE entry.
		if (!row.we_kanji || song.sheets.length === 1) {
			const fill = fieldMerger(report, song, row);
			const bpm = numeric(row.bpm);
			if (bpm !== null && bpm > 0 && fill("bpm", song.bpm, bpm)) song.bpm = bpm;
			const date = releaseDate(row.date_added);
			if (date && fill("releaseDate", song.releaseDate, date)) song.releaseDate = date;
			const name = row.version ?? "";
			const version = versions.get(versionNames[name] ?? name);
			if (version && fill("version", song.version, version)) song.version = version;
		}
		for (const sheet of sheets) enrichSheet(song, sheet, row, region, report);
	}
	return report;
}
