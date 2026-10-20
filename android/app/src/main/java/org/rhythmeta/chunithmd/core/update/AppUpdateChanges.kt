package org.rhythmeta.chunithmd.core.update

data class AppUpdateChanges(val commits: List<AppUpdateCommit> = emptyList(), val incomplete: Boolean = false)
