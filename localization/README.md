# Shared app localization

`strings.json` is the source for Simplified Chinese (`zh-Hans`), Traditional
Chinese (`zh-Hant`), English (`en`), and Japanese (`ja`). Keys are stable source
phrases; values are the four translations. Android uses the KMP `tr(key, args…)`
function for native UI, accessibility labels, validation messages, and exports.
Numbered placeholders such as `{0}` allow translators to reorder arguments.

Run `python3 scripts/generate-localization.py` after editing the catalog. Commit
the generated `Translations.kt` so builds need no Python or resource plugin.
`python3 scripts/generate-localization.py --check` checks the generated file,
translation completeness, placeholders, and literal `tr` call sites.

Android resolves its configured system locales before composing the UI. Chinese
script subtags take precedence over region, including `zh-Hans-TW`; traditional
Chinese regions include TW, HK, and MO. The first supported system language wins,
with English as the fallback. There is no in-app language selector. Android 13+
also recognizes the four languages in its system app-language settings.

Keep song titles, aliases, profile/collection names, server keys, and stored
status/filter values unchanged. Translate status labels only when rendering them.
The requested scanner message stays `Working in progress` in all four languages.
iOS has not been changed; it can reuse this catalog in a later integration.

Traditional Chinese uses shared written UI terminology for Taiwan, Hong Kong,
and Macau, rather than mechanical character conversion. Keep these terms
consistent: 設定, 登入／登出, 帳號, 資料, 儲存, 匯入／匯出, 剪貼簿,
連結, 伺服器, 社群, 個人檔案, and 收藏集. Use 最愛 for favorite songs
to distinguish them from collections, and 檔案 for an actual file. Preserve
game terms such as 譜面, 定數, Rating, FC, AJ, and AJC. Edit full sentences
when needed; do not regenerate `zh-Hant` by converting `zh-Hans` characters.
