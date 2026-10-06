package org.rhythmeta.chunithmd.shared

import org.rhythmeta.chunithmd.shared.localization.tr
import org.rhythmeta.chunithmd.shared.backup.*
import kotlin.time.Clock
import org.rhythmeta.chunithmd.collection.*
import org.rhythmeta.chunithmd.shared.importing.*
import org.rhythmeta.chunithmd.shared.community.mergeCommunityAliases

/** Native clients edit the same portable store used by backup/restore. Every edit rereads
 * the current snapshot so a completed cloud restore cannot be overwritten by stale UI. */
class PersonalDataBridge(private val files: SnapshotFiles) {
    private val codec = CatalogJson.codec

    @Throws(Exception::class)
    fun snapshotJson(): String = codec.encodeToString(read())

    private fun read(): BackupSnapshot {
        files.read("personal.pb.gz").bytes?.let { return BackupCodec.decode(it) }
        val now = Clock.System.now().toEpochMilliseconds()
        val initial = BackupSnapshot(magic = "RHYTHMETA_BACKUP", formatVersion = 1,
            game = "chunithmd", createdAt = now, clientVersion = "1",
            profiles = listOf(BackupProfile(id = uuid(), name = tr("我的档案"), server = "jp", active = true, createdAt = now)))
        write(initial)
        return initial
    }

    private fun write(snapshot: BackupSnapshot) {
        check(files.read("restore-pending.pb.gz").bytes == null) { tr("正在恢复备份，请稍后再试") }
        files.write("personal.pb.gz", BackupCodec.encode(snapshot))
    }

    @Throws(Exception::class)
    fun saveProfile(id: String?, name: String, server: String, title: String): String {
        val draft = ProfileDraft(name, ProfileServer.fromWire(server), title).normalized()
        require(draft.isValid()) { tr("请输入档案名称") }
        val state = read()
        val existing = state.profiles.firstOrNull { it.id == id }
        val profile = (existing ?: BackupProfile(id = uuid(), createdAt = Clock.System.now().toEpochMilliseconds()))
            .copy(name = draft.name, server = draft.server.wireValue, title = draft.title)
        write(state.copy(profiles = if (existing == null) state.profiles + profile else state.profiles.map { if (it.id == id) profile else it }))
        return profile.id
    }

    @Throws(Exception::class)
    fun setAvatar(profileId: String, base64: String) {
        val state = read()
        val bytes = kotlin.io.encoding.Base64.Default.decode(base64)
        require(bytes.size <= 16 * 1024 * 1024)
        require(state.profiles.any { it.id == profileId })
        write(state.copy(profiles = state.profiles.map { if (it.id == profileId) it.copy(avatar = bytes) else it }))
    }

    @Throws(Exception::class)
    fun activateProfile(id: String) {
        val state = read()
        require(state.profiles.any { it.id == id })
        write(state.copy(profiles = state.profiles.map { it.copy(active = it.id == id) }))
    }

    @Throws(Exception::class)
    fun deleteProfile(id: String) {
        val state = read()
        require(state.profiles.none { it.id == id && it.active }) { tr("不能删除当前档案") }
        write(state.copy(profiles = state.profiles.filterNot { it.id == id },
            scores = state.scores.filterNot { it.profileId == id }, playRecords = state.playRecords.filterNot { it.result.profileId == id }))
    }

    @Throws(Exception::class)
    fun saveScore(songId: String, type: String, difficulty: String, score: Int, clear: String, combo: String, chain: String) {
        require(clear.isEmpty() || ClearType.fromWire(clear) != null)
        require(combo.isEmpty() || FullComboType.fromWire(combo) != null)
        require(chain.isEmpty() || FullChainType.fromWire(chain) != null)
        require(ChunithmScoreRules.isValid(score)) { tr("分数需在 0–1,010,000 之间") }
        val state = read()
        val profile = state.profiles.first { it.active }
        val result = BackupScore(profileId = profile.id, chartKey = "$songId:$type:$difficulty", songId = songId,
            score = score, rank = ChunithmScoreRules.rank(score), achievedAt = Clock.System.now().toEpochMilliseconds(),
            clear = clear, fc = combo, fs = chain)
        write(state.copy(playRecords = state.playRecords + BackupPlayRecord(uuid(), result)))
    }

    @Throws(Exception::class)
    fun deleteRecord(id: String) {
        val state = read()
        write(state.copy(playRecords = state.playRecords.filterNot { it.id == id }))
    }

    @Throws(Exception::class)
    fun toggleFavorite(songId: String) {
        val state = read()
        val favorites = state.favoriteSongIds.toMutableSet()
        if (!favorites.add(songId)) favorites.remove(songId)
        write(state.copy(favoriteSongIds = favorites.toList()))
    }

    @Throws(Exception::class)
    fun saveCollection(id: String?, name: String) {
        require(name.isNotBlank()) { tr("请输入收藏夹名称") }
        val state = read()
        val now = Clock.System.now().toEpochMilliseconds()
        val old = state.collections.firstOrNull { it.id == id }
        val item = (old ?: BackupCollection(id = uuid(), sortIndex = state.collections.size, createdAt = now))
            .copy(name = name.trim(), updatedAt = now)
        write(state.copy(collections = if (old == null) state.collections + item else state.collections.map { if (it.id == id) item else it }))
    }

    @Throws(Exception::class)
    fun deleteCollection(id: String) {
        val state = read()
        write(state.copy(collections = state.collections.filterNot { it.id == id }, collectionItems = state.collectionItems.filterNot { it.collectionId == id }))
    }

    @Throws(Exception::class)
    fun toggleCollectionSong(collectionId: String, songId: String, type: String, difficulty: String) {
        val state = read()
        require(state.collections.any { it.id == collectionId })
        val exists = state.collectionItems.any { it.collectionId == collectionId && it.songId == songId && it.chartType == type && it.difficulty == difficulty }
        val now = Clock.System.now().toEpochMilliseconds()
        val items = if (exists) state.collectionItems.filterNot { it.collectionId == collectionId && it.songId == songId && it.chartType == type && it.difficulty == difficulty }
            else state.collectionItems + BackupCollectionItem(id = uuid(), collectionId = collectionId, songId = songId, chartType = type, difficulty = difficulty,
                position = state.collectionItems.count { it.collectionId == collectionId }, createdAt = now, updatedAt = now)
        write(state.copy(collectionItems = items))
    }

    @Throws(Exception::class)
    fun collectionLink(id: String): String {
        val state = read()
        val collection = state.collections.first { it.id == id }
        return PortableCollectionCodec.encode(CollectionExport(collection.name, state.collectionItems.filter { it.collectionId == id }
            .map { CollectionEntry(it.songId, it.chartType, it.difficulty) }))
    }
    @Throws(Exception::class)
    fun importCollection(text: String): String {
        val value = PortableCollectionCodec.decode(text)
        val state = read()
        val now = Clock.System.now().toEpochMilliseconds()
        val id = uuid()
        write(state.copy(collections = state.collections + BackupCollection(id, value.name, state.collections.size, now, now),
            collectionItems = state.collectionItems + value.entries.mapIndexed { index, entry ->
                BackupCollectionItem(uuid(), id, entry.songId, entry.chartType, entry.difficulty, index, now, now)
            }))
        return value.name
    }

    fun importDivingFish(bundle: CatalogBundle, profileId: String, payload: DivingFishPayload): ScoreImportResult {
        val state = readForImport(profileId)
        val plan = DivingFishImportPolicy.plan(payload, bundle, profileId, records(state), Clock.System.now().toEpochMilliseconds())
        write(state.copy(playRecords = state.playRecords + plan.records.map { it.toBackup() }))
        return plan.result
    }
    fun importLxns(bundle: CatalogBundle, profileId: String, payload: LxnsPayload): ScoreImportResult {
        val state = readForImport(profileId)
        val plan = LxnsImportPolicy.plan(payload, bundle, profileId, records(state), Clock.System.now().toEpochMilliseconds())
        write(state.copy(playRecords = state.playRecords + plan.records.map { it.toBackup() }))
        return plan.result
    }
    fun importOtogame(bundle: CatalogBundle, profileId: String, payload: OtogamePayload): ScoreImportResult {
        val state = readForImport(profileId)
        require(server(state) == ProfileServer.Jp) { tr("需要启用一个日服档案") }
        val plan = OtogameImportPolicy.plan(payload, bundle, profileId, records(state))
        write(state.copy(playRecords = state.playRecords + plan.records.map { it.toBackup() }))
        return plan.result
    }
    private fun readForImport(profileId: String): BackupSnapshot = read().also {
        if (it.profiles.firstOrNull { profile -> profile.active }?.id != profileId) throw DivingFishException("profile_changed")
    }

    private fun records(state: BackupSnapshot): List<ScoreRecord> {
        val profile = state.profiles.firstOrNull { it.active }?.id
        val history = state.playRecords.map { it.toRecord() }
        val best = state.scores.mapIndexed { index, score -> BackupPlayRecord("best:$index", score).toRecord() }
        return (history + best).filter { it.profileId == profile }
    }
    private fun server(state: BackupSnapshot) = ProfileServer.fromWire(state.profiles.firstOrNull { it.active }?.server)

    @Throws(Exception::class)
    fun bestEntries(bundle: CatalogBundle): List<BestTableEntry> = read().let { buildBestTableEntries(bundle, records(it), server(it)) }

    @Throws(Exception::class)
    fun rating(bundle: CatalogBundle): PlayerRatingSummary = calculatePlayerRating(bestEntries(bundle).map { RatingChartEntry(it.chartId, it.songId, it.rating, it.isNew) })

    @Throws(Exception::class)
    fun bestTable(bundle: CatalogBundle, version: String?): BestTableResponse = read().let { state ->
        buildBestTableResponse(bundle, records(state), server(state), bestTablePreferences(state).copy(selectedVersion = version))
    }

    @Throws(Exception::class)
    fun setBestTableCapacity(bestCount: Int, newCount: Int) {
        val state = read()
        val preferences = BestTablePreferences(bestCount, newCount).normalized()
        val values = listOf(
            BackupSetting(key = "android.chunithmd.best.best_count", kind = "int", integerValue = preferences.bestCount.toLong()),
            BackupSetting(key = "android.chunithmd.best.new_count", kind = "int", integerValue = preferences.newCount.toLong()),
        )
        val keys = values.map { it.key }.toSet()
        write(state.copy(settings = state.settings.filterNot { it.key in keys } + values))
    }

    private fun bestTablePreferences(state: BackupSnapshot): BestTablePreferences {
        fun count(key: String, fallback: Int): Int = state.settings
            .firstOrNull { it.key == "android.chunithmd.best.$key" && it.kind == "int" }
            ?.integerValue?.toInt() ?: fallback
        return BestTablePreferences(
            count("best_count", BestTablePreferences.DEFAULT_BEST_COUNT),
            count("new_count", BestTablePreferences.DEFAULT_NEW_COUNT),
        ).normalized()
    }

    @Throws(Exception::class)
    fun constants(bundle: CatalogBundle): ConstantTableResponse = read().let { buildConstantTableResponse(bundle, records(it), server(it), it.favoriteSongIds.toSet()) }

    @Throws(Exception::class)
    fun recommendations(bundle: CatalogBundle): RecommendationResponse = read().let { RecommendationCalculator.calculate(bundle, records(it), server(it)) }

    @Throws(Exception::class)
    fun scoreQuery(bundle: CatalogBundle, aliases: Map<String, List<String>>): ScoreQueryResponse = read().let {
        buildScoreQueryResponse(bundle.copy(aliases = mergeCommunityAliases(bundle.aliases, aliases)), records(it), server(it))
    }

    @Throws(Exception::class)
    fun plate(bundle: CatalogBundle, version: String?, kind: String): PlateProgressResponse = read().let {
        PlateProgressCalculator.calculate(bundle, records(it), server(it), version, PlateType.entries.firstOrNull { it.name == kind } ?: PlateType.Spirit)
    }

    @Throws(Exception::class)
    fun playHistory(): List<ScoreRecord> = read().let { state ->
        val profileId = state.profiles.firstOrNull { it.active }?.id
        state.playRecords.filter { it.result.profileId == profileId }.map { it.toRecord() }
    }

    fun chartHistory(records: List<ScoreRecord>, songId: String, sheetId: String, sort: ScoreHistorySort): List<ScoreRecord> =
        records.filter { it.songId == songId && it.sheetKey == "$songId:$sheetId" }.sortForHistory(sort)

    fun bestHistoryRecordId(records: List<ScoreRecord>): String? = records.bestScore()?.id

    fun ratingTable(constant: Double): List<RatingTableRow> = buildRatingTable(constant)
    fun randomSongs(songs: List<CatalogSong>, count: Int): List<CatalogSong> = RandomSongQuery.draw(songs, count)

    @Throws(Exception::class)
    fun chartProgress(): Map<String, Float> = records(read()).groupBy { it.sheetKey }
        .mapValues { (_, records) -> scoreProgress(records.bestScore()?.score) }

    private fun uuid(): String {
        val bytes = secureRandomBytes(16)
        bytes[6] = ((bytes[6].toInt() and 15) or 64).toByte()
        bytes[8] = ((bytes[8].toInt() and 63) or 128).toByte()
        val hex = bytes.joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
        return "${hex.take(8)}-${hex.substring(8,12)}-${hex.substring(12,16)}-${hex.substring(16,20)}-${hex.substring(20)}"
    }
}
