package org.rhythmeta.chunithmd.core.update

import kotlinx.serialization.Serializable

@Serializable
data class AppUpdateHistory(val schemaVersion: Int, val commits: List<AppUpdateCommit>)
