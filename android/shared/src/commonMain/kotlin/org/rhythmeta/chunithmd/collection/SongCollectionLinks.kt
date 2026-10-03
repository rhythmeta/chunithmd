package org.rhythmeta.chunithmd.collection

/** The game prefix keeps maimaid and CHUNITHM song identifiers separate. */
object SongCollectionLinks {
    const val Prefix = "CHMD1."
    const val WebBaseUrl = "https://dash.rhythmeta.org/collection/"
    const val MaxCodeSize = 200_000
    private const val MaxInputSize = MaxCodeSize + 2_048
    private val codePattern = Regex("CHMD1\\.[A-Za-z0-9_-]+={0,2}")
    private val linkPattern = Regex(
        "(?:https://dash\\.rhythmeta\\.org/collection/|chunithmd://collection/)([^/?#]+)(?:[?#].*)?",
        RegexOption.IGNORE_CASE,
    )

    fun extractCode(value: String): String {
        require(value.length <= MaxInputSize) { "收藏夹链接或分享码过长" }
        val input = value.trim()
        val code = if (input.startsWith(Prefix)) input else {
            linkPattern.matchEntire(input)?.groupValues?.get(1)
                ?: throw IllegalArgumentException("请输入 chunithmd 收藏夹链接或 CHMD1 分享码")
        }
        require(code.length <= MaxCodeSize) { "分享码过长" }
        require(codePattern.matches(code)) { "请输入有效的 chunithmd 收藏夹链接或 CHMD1 分享码" }
        return code
    }

    fun webUrl(code: String): String = WebBaseUrl + extractCode(code)
}
