package org.rhythmeta.chunithmd

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.rhythmeta.chunithmd.shared.PlateType
import org.rhythmeta.chunithmd.ui.plate.PlateProgressPreferences
import org.rhythmeta.chunithmd.ui.plate.PlateProgressPreferencesRepository

class PlateProgressPreferencesRepositoryTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun selectionsSurviveClosingAndReopeningTheStore() = runBlocking {
        val file = File(temporaryFolder.root, "plate.preferences_pb")
        val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = repository(file, writerScope)
            assertEquals(PlateProgressPreferences(), repository.preferences.first())
            repository.setVersion("CHUNITHM AIR PLUS")
            repository.setPlateType(PlateType.Legend)
            repository.setDifficulty("expert")
            repository.setRemainingOnly(true)
        } finally {
            writerScope.coroutineContext[Job]!!.cancelAndJoin()
        }
        val readerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            assertEquals(
                PlateProgressPreferences("CHUNITHM AIR PLUS", PlateType.Legend, "expert", true),
                repository(file, readerScope).preferences.first(),
            )
        } finally {
            readerScope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }

    @Test
    fun selectingAllDifficultiesAndDisablingFilterPersistWithoutResettingOtherSelections() = runBlocking {
        val file = File(temporaryFolder.root, "plate.preferences_pb")
        val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repository = repository(file, writerScope)
            repository.setVersion("CHUNITHM PLUS")
            repository.setPlateType(PlateType.Tribute)
            repository.setDifficulty("master")
            repository.setRemainingOnly(true)
            repository.setDifficulty(null)
            repository.setRemainingOnly(false)
        } finally {
            writerScope.coroutineContext[Job]!!.cancelAndJoin()
        }
        val readerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            assertEquals(
                PlateProgressPreferences("CHUNITHM PLUS", PlateType.Tribute, null, false),
                repository(file, readerScope).preferences.first(),
            )
        } finally {
            readerScope.coroutineContext[Job]!!.cancelAndJoin()
        }
    }

    private fun repository(file: File, scope: CoroutineScope) = PlateProgressPreferencesRepository(
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }),
    )
}
