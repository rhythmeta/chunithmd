package org.rhythmeta.chunithmd.shared.diagnostics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticLogRedactorTest {
    @Test fun removesHeaderJsonAndCallbackSecrets() {
        val log = """
            Authorization: Bearer header-secret
            {"accessToken":"access-secret","refresh_token":"refresh-secret","password":"pass with spaces"}
            chunithmd://auth/callback?sessionCode=callback-secret&result=success
            code_verifier=verifier-secret
            IllegalStateException: useful diagnostic
        """.trimIndent()
        val result = DiagnosticLogRedactor.redact(log)
        listOf("header-secret", "access-secret", "refresh-secret", "pass with spaces", "callback-secret", "verifier-secret")
            .forEach { assertFalse(result.contains(it), it) }
        assertTrue(result.contains("&result=success"))
        assertTrue(result.contains("IllegalStateException: useful diagnostic"))
    }

    @Test fun preservesOrdinaryDiagnosticsAndIsIdempotent() {
        val log = "App version: 1.0 (42)\nThread: main\njava.lang.IllegalStateException: failed"
        assertEquals(log, DiagnosticLogRedactor.redact(log))
        val redacted = DiagnosticLogRedactor.redact("password=secret")
        assertEquals(redacted, DiagnosticLogRedactor.redact(redacted))
    }
}
