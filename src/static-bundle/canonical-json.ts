import type { JsonValue } from "./types.js";

const sortValue = (value: JsonValue): JsonValue => {
	if (Array.isArray(value)) return value.map((item) => sortValue(item));
	if (value !== null && typeof value === "object") {
		return Object.fromEntries(
			Object.keys(value)
				.sort()
				.map((key) => [key, sortValue(value[key] as JsonValue)]),
		) as JsonValue;
	}
	return value;
};

export const canonicalJson = (value: JsonValue): string => JSON.stringify(sortValue(value));
