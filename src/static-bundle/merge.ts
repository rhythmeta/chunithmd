import { chartKey, lxnsDifficultyIndex, normalizeTitle, normalizeWorldEndName } from "./normalize.js";
import type {
	BundleSong,
	LxnsAliasList,
	LxnsSong,
	LxnsSongList,
	MappingReport,
	PrimaryData,
	RegionOverride,
	StaticBundlePayload,
} from "./types.js";

const selectLxnsMatch = (primarySong: PrimaryData["songs"][number], matches: LxnsSong[]): LxnsSong[] => {
	const exactTitle = matches.filter((song) => song.title === primarySong.title);
	if (exactTitle.length === 1) return exactTitle;
	const normalizedArtist = normalizeTitle(primarySong.artist);
	const exactArtist = matches.filter((song) => song.artist && normalizeTitle(song.artist) === normalizedArtist);
	if (exactArtist.length === 1) return exactArtist;
	const hasWorldEnd = primarySong.sheets.some((sheet) => sheet.type === "we");
	const hasStandard = primarySong.sheets.some((sheet) => sheet.type !== "we");
	const filtered = matches.filter((song) => {
		const songHasWorldEnd = song.difficulties.some((difficulty) => difficulty.difficulty === 5);
		const songHasStandard = song.difficulties.some((difficulty) => difficulty.difficulty !== 5);
		return (hasWorldEnd && !hasStandard && songHasWorldEnd) || (hasStandard && !hasWorldEnd && songHasStandard);
	});
	return filtered.length > 0 ? filtered : matches;
};

const asRecord = (value: unknown): Record<string, unknown> => {
	if (typeof value !== "object" || value === null || Array.isArray(value)) throw new Error("Expected an object.");
	return value as Record<string, unknown>;
};

export const validatePrimaryData = (value: unknown): PrimaryData => {
	const record = asRecord(value);
	if (
		!Array.isArray(record.songs) ||
		!Array.isArray(record.categories) ||
		!Array.isArray(record.difficulties) ||
		!Array.isArray(record.regions) ||
		!Array.isArray(record.types) ||
		!Array.isArray(record.versions)
	) {
		throw new Error("Primary data must contain categories, difficulties, regions, types, versions, and songs arrays.");
	}
	for (const song of record.songs) {
		const item = asRecord(song);
		if (
			typeof item.songId !== "string" ||
			typeof item.title !== "string" ||
			typeof item.artist !== "string" ||
			typeof item.category !== "string" ||
			typeof item.imageName !== "string" ||
			!Array.isArray(item.sheets)
		) {
			throw new Error("Primary data contains a song with invalid songId, title, or sheets.");
		}
		for (const sheet of item.sheets) {
			const sheetRecord = asRecord(sheet);
			if (typeof sheetRecord.type !== "string" || typeof sheetRecord.difficulty !== "string") {
				throw new Error("Primary data contains a sheet with invalid type or difficulty.");
			}
		}
	}
	return value as PrimaryData;
};

export const validateLxnsSongList = (value: unknown): LxnsSongList => {
	const record = asRecord(value);
	if (!Array.isArray(record.songs)) throw new Error("LXNS song data must contain a songs array.");
	for (const song of record.songs) {
		const item = asRecord(song);
		if (!Number.isSafeInteger(item.id) || typeof item.title !== "string" || !Array.isArray(item.difficulties)) {
			throw new Error("LXNS song data contains an invalid song.");
		}
	}
	return value as LxnsSongList;
};

export const validateLxnsAliasList = (value: unknown): LxnsAliasList => {
	const record = asRecord(value);
	if (!Array.isArray(record.aliases)) throw new Error("LXNS alias data must contain an aliases array.");
	for (const alias of record.aliases) {
		const item = asRecord(alias);
		if (!Number.isSafeInteger(item.song_id) || !Array.isArray(item.aliases) || item.aliases.some((entry) => typeof entry !== "string")) {
			throw new Error("LXNS alias data contains an invalid alias entry.");
		}
	}
	return value as LxnsAliasList;
};

export const mergeSources = (primary: PrimaryData, lxns: LxnsSongList, aliasData: LxnsAliasList) => {
	const lxnsByTitle = new Map<string, LxnsSong[]>();
	for (const song of lxns.songs) {
		const key = normalizeTitle(song.title);
		const values = lxnsByTitle.get(key) ?? [];
		values.push(song);
		lxnsByTitle.set(key, values);
	}

	const matchedLxnsByPrimaryId = new Map<string, LxnsSong>();
	const ambiguousPrimarySongs: MappingReport["ambiguousPrimarySongs"] = [];
	const unmatchedPrimarySongs: MappingReport["unmatchedPrimarySongs"] = [];

	for (const song of primary.songs) {
		const matches = selectLxnsMatch(song, lxnsByTitle.get(normalizeTitle(song.title)) ?? []);
		if (matches.length === 1) matchedLxnsByPrimaryId.set(song.songId, matches[0] as LxnsSong);
		else if (matches.length === 0) unmatchedPrimarySongs.push({ songId: song.songId, title: song.title });
		else {
			ambiguousPrimarySongs.push({ songId: song.songId, title: song.title, lxnsIds: matches.map((item) => item.id) });
		}
	}
	if (ambiguousPrimarySongs.length > 0) {
		throw new Error(`Ambiguous LXNS title matches: ${ambiguousPrimarySongs.map((item) => item.title).join(", ")}`);
	}

	const primaryIdByLxnsId = new Map<number, string>();
	for (const [songId, lxnsSong] of matchedLxnsByPrimaryId) primaryIdByLxnsId.set(lxnsSong.id, songId);
	const worldEndByOrigin = new Map<number, LxnsSong[]>();
	for (const song of lxns.songs) {
		for (const difficulty of song.difficulties) {
			if (difficulty.difficulty !== 5 || difficulty.origin_id === undefined) continue;
			const values = worldEndByOrigin.get(difficulty.origin_id) ?? [];
			values.push(song);
			worldEndByOrigin.set(difficulty.origin_id, values);
		}
	}

	let cnChartMatchCount = 0;
	let cnChartMissingCount = 0;
	const songs: BundleSong[] = primary.songs.map((song) => {
		const matched = matchedLxnsByPrimaryId.get(song.songId);
		const charts: RegionOverride["charts"] = {};
		for (const sheet of song.sheets) {
			const key = chartKey(sheet.type, sheet.difficulty);
			let lxnsDifficulty = matched?.difficulties.find(
				(item) => item.difficulty === lxnsDifficultyIndex(sheet.type, sheet.difficulty),
			);
			if (sheet.type === "we" && matched) {
				const directWorldEnd = matched.difficulties.some((item) => item.difficulty === 5) ? [matched] : [];
				const candidates = directWorldEnd.length > 0 ? directWorldEnd : (worldEndByOrigin.get(matched.id) ?? []);
				lxnsDifficulty = candidates
					.flatMap((item) => item.difficulties)
					.find((item) => item.difficulty === 5 && item.kanji && normalizeWorldEndName(item.kanji) === normalizeWorldEndName(sheet.difficulty));
			}
			if (lxnsDifficulty) {
				cnChartMatchCount += 1;
				charts[key] = {
					available: true,
					level: lxnsDifficulty.level,
					levelValue: lxnsDifficulty.level_value,
				};
			} else {
				cnChartMissingCount += 1;
				charts[key] = { available: false };
			}
		}
		const available = Object.values(charts).some((chart) => chart.available);
		return { ...song, regionOverrides: { cn: { available, charts } } };
	});

	const aliases: Record<string, string[]> = {};
	let unmatchedAliases = 0;
	const unmatchedAliasEntries: MappingReport["unmatchedAliasEntries"] = [];
	let matchedAliasSongCount = 0;
	for (const entry of aliasData.aliases) {
		const songId = primaryIdByLxnsId.get(entry.song_id);
		if (!songId) {
			unmatchedAliases += 1;
			unmatchedAliasEntries.push({ songId: entry.song_id, aliases: entry.aliases });
			continue;
		}
		const values = Array.from(new Set(entry.aliases.map((alias) => alias.trim()).filter(Boolean))).sort((a, b) => a.localeCompare(b));
		if (values.length > 0) {
			aliases[songId] = values;
			matchedAliasSongCount += 1;
		}
	}

	const report: MappingReport = {
		primarySongCount: primary.songs.length,
		lxnsSongCount: lxns.songs.length,
		matchedSongCount: matchedLxnsByPrimaryId.size,
		unmatchedSongCount: unmatchedPrimarySongs.length,
		ambiguousSongCount: ambiguousPrimarySongs.length,
		unmatchedPrimarySongs,
		ambiguousPrimarySongs,
		unmatchedAliases,
		unmatchedAliasEntries,
		matchedAliasSongCount,
		cnChartMatchCount,
		cnChartMissingCount,
	};

	const payload: StaticBundlePayload = {
		schemaVersion: 1,
		catalog: {
			regions: ["jp", "intl", "cn"],
			categories: primary.categories,
			difficulties: primary.difficulties,
			types: primary.types,
			versions: primary.versions,
			songs,
		},
		aliases: Object.fromEntries(Object.entries(aliases).sort(([left], [right]) => left.localeCompare(right))),
	};
	return { payload, report };
};
