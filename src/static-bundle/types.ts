export type JsonObject = { [key: string]: JsonValue };
export type JsonValue = null | boolean | number | string | JsonValue[] | JsonObject;

export type PrimarySheet = {
	type: string;
	difficulty: string;
	level: string;
	levelValue: number;
	internalLevel?: string | null;
	internalLevelValue?: number | null;
	noteDesigner?: string | null;
	noteCounts?: JsonObject | null;
	regions?: Record<string, boolean> | null;
	isSpecial?: boolean;
};

export type PrimarySong = {
	songId: string;
	title: string;
	artist: string;
	category: string;
	bpm: number;
	imageName: string;
	version: string | null;
	releaseDate: string | null;
	sheets: PrimarySheet[];
};

export type PrimaryData = {
	categories: Array<Record<string, JsonValue>>;
	difficulties: Array<Record<string, JsonValue>>;
	regions: Array<Record<string, JsonValue>>;
	types: Array<Record<string, JsonValue>>;
	versions: Array<Record<string, JsonValue>>;
	songs: PrimarySong[];
	updateTime?: string;
};

export type LxnsDifficulty = {
	difficulty: number;
	level: string;
	level_value: number;
	note_designer?: string | null;
	version?: number;
	origin_id?: number;
	kanji?: string;
	star?: number;
	notes?: JsonObject | null;
};

export type LxnsSong = {
	id: number;
	title: string;
	artist?: string;
	genre?: string;
	bpm?: number;
	version?: number;
	difficulties: LxnsDifficulty[];
};

export type LxnsSongList = {
	songs: LxnsSong[];
	genres: Array<Record<string, JsonValue>>;
	versions: Array<Record<string, JsonValue>>;
};

export type LxnsAlias = { song_id: number; aliases: string[] };
export type LxnsAliasList = { aliases: LxnsAlias[] };

export type RegionOverrideChart = {
	available: boolean;
	level?: string;
	levelValue?: number;
};

export type RegionOverride = {
	available: boolean;
	charts: Record<string, RegionOverrideChart>;
};

export type BundleSong = PrimarySong & {
	regionOverrides: { cn: RegionOverride };
};

export type StaticBundlePayload = {
	schemaVersion: 1;
	catalog: {
	regions: string[];
		categories: Array<Record<string, JsonValue>>;
		difficulties: Array<Record<string, JsonValue>>;
		types: Array<Record<string, JsonValue>>;
		versions: Array<Record<string, JsonValue>>;
		songs: BundleSong[];
	};
	aliases: Record<string, string[]>;
};

export type MappingReport = {
	primarySongCount: number;
	lxnsSongCount: number;
	matchedSongCount: number;
	unmatchedSongCount: number;
	ambiguousSongCount: number;
	unmatchedPrimarySongs: Array<{ songId: string; title: string }>;
	ambiguousPrimarySongs: Array<{ songId: string; title: string; lxnsIds: number[] }>;
	unmatchedAliases: number;
	unmatchedAliasEntries: Array<{ songId: number; aliases: string[] }>;
	matchedAliasSongCount: number;
	cnChartMatchCount: number;
	cnChartMissingCount: number;
};

export type BuildManifest = {
	schemaVersion: 1;
	product: "chunithmd";
	version: string;
	sha256: string;
	bundle: string;
	createdAt: string;
	sources: Record<string, {
		url: string;
		contentSha256: string;
		updateTime?: string;
	}>;
	mapping: Omit<MappingReport, "unmatchedPrimarySongs" | "ambiguousPrimarySongs" | "unmatchedAliasEntries">;
	assets: { jacketBaseUrl: string };
};
