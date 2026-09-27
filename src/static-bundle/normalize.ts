export const normalizeTitle = (value: string): string =>
	value.normalize("NFKC").trim().toLocaleLowerCase().replace(/\s+/gu, " ");

export const chartKey = (type: string, difficulty: string): string => `${type}:${difficulty}`;

export const normalizeWorldEndName = (value: string): string => value.replace(/[【】]/gu, "").trim();

export const lxnsDifficultyIndex = (type: string, difficulty: string): number | null => {
	if (type === "we") return 5;
	return {
		basic: 0,
		advanced: 1,
		expert: 2,
		master: 3,
		ultima: 4,
	}[difficulty] ?? null;
};
