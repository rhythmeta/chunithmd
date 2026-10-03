package org.rhythmeta.chunithmd.shared.diagnostics

object DiagnosticLogRedactor {
    private val authorization = Regex("""(?i)(authorization["']?\s*[:=]\s*["']?)(?:bearer|basic)\s+[^\s"',;]+""")
    private val secrets = Regex("""(?i)((?:access[_-]?token|refresh[_-]?token|password|sessionCode|code_verifier|codeVerifier)["']?\s*[:=]\s*)("[^"\r\n]*"|'[^'\r\n]*'|[^\s&,;}]+)""")

    fun redact(value: String): String = value
        .replace(authorization) { "${it.groupValues[1]}[REDACTED]" }
        .replace(secrets) { "${it.groupValues[1]}[REDACTED]" }
}
