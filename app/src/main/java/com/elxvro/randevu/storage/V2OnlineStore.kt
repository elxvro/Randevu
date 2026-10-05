package com.elxvro.randevu.storage

import android.content.Context
import com.elxvro.randevu.online.OnlineEntityType
import com.elxvro.randevu.online.OnlineMutation
import com.elxvro.randevu.online.OnlineSnapshot
import com.elxvro.randevu.online.OnlineSyncQueue
import com.elxvro.randevu.online.SyncConflict
import com.elxvro.randevu.online.V2SnapshotProjector
import com.elxvro.randevu.online.V2SyncPersistence

class V2OnlineStore(
    context: Context,
    private val legacyStore: LiveSyncStore,
    private val ownerName: String
) : V2SyncPersistence {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun pending(): List<OnlineMutation> =
        V2OnlineCodec.decodePending(prefs.getString(KEY_PENDING, null))

    override fun savePending(value: List<OnlineMutation>) {
        prefs.edit().putString(KEY_PENDING, V2OnlineCodec.encodePending(value)).apply()
    }

    fun enqueue(value: OnlineMutation) {
        savePending(OnlineSyncQueue.enqueue(pending(), value))
    }

    fun enqueueAll(values: List<OnlineMutation>) {
        var queue = pending()
        values.forEach { queue = OnlineSyncQueue.enqueue(queue, it) }
        savePending(queue)
    }

    override fun replaceSnapshot(value: OnlineSnapshot) {
        prefs.edit().putString(KEY_SNAPSHOT, V2OnlineCodec.encodeSnapshot(value)).apply()
        val projected = V2SnapshotProjector.project(value, ownerName)
        projected.profile?.let(legacyStore::saveBusinessProfile)
        legacyStore.saveServices(projected.services)
        legacyStore.saveStaff(projected.staff)
        legacyStore.saveStaffLeaves(projected.leaves)
        legacyStore.saveAppointments(projected.appointments)
        saveVersions(projected.versions)
    }

    fun snapshot(): OnlineSnapshot? =
        V2OnlineCodec.decodeSnapshot(prefs.getString(KEY_SNAPSHOT, null))

    override fun setVersion(entity: OnlineEntityType, id: String, version: Int) {
        if (version < 1) return
        val next = versions().toMutableMap()
        next[versionKey(entity, id)] = version
        saveVersions(next)
    }

    fun versions(): Map<String, Int> =
        V2OnlineCodec.decodeVersions(prefs.getString(KEY_VERSIONS, null))

    private fun saveVersions(value: Map<String, Int>) {
        prefs.edit().putString(KEY_VERSIONS, V2OnlineCodec.encodeVersions(value)).apply()
    }

    override fun saveConflict(value: SyncConflict?) {
        val encoded = V2OnlineCodec.encodeConflict(value)
        val editor = prefs.edit()
        if (encoded == null) editor.remove(KEY_CONFLICT) else editor.putString(KEY_CONFLICT, encoded)
        editor.apply()
    }

    fun conflict(): SyncConflict? =
        V2OnlineCodec.decodeConflict(prefs.getString(KEY_CONFLICT, null))

    override fun markBootstrap(serverTime: String) {
        prefs.edit().putString(KEY_LAST_BOOTSTRAP, serverTime).apply()
    }

    fun lastBootstrap(): String = prefs.getString(KEY_LAST_BOOTSTRAP, "").orEmpty()

    override fun markImported(entity: OnlineEntityType, id: String) {
        val next = importedIds().toMutableSet()
        next += "${entity.name}:$id"
        prefs.edit().putString(KEY_IMPORTED, V2OnlineCodec.encodeImported(next)).apply()
    }

    fun importedIds(): Set<String> =
        V2OnlineCodec.decodeImported(prefs.getString(KEY_IMPORTED, null))

    fun importHandled(): Boolean = prefs.getBoolean(KEY_IMPORT_HANDLED, false)

    fun markImportHandled() {
        prefs.edit().putBoolean(KEY_IMPORT_HANDLED, true).apply()
    }

    fun clearCloudCache() {
        prefs.edit()
            .remove(KEY_PENDING)
            .remove(KEY_SNAPSHOT)
            .remove(KEY_VERSIONS)
            .remove(KEY_CONFLICT)
            .remove(KEY_LAST_BOOTSTRAP)
            .remove(KEY_IMPORTED)
            .remove(KEY_IMPORT_HANDLED)
            .apply()
    }

    companion object {
        fun versionKey(entity: OnlineEntityType, id: String): String = "${entity.name}:$id"

        private const val PREFS = "randevu_online_v2"
        private const val KEY_PENDING = "pending"
        private const val KEY_SNAPSHOT = "snapshot"
        private const val KEY_VERSIONS = "versions"
        private const val KEY_CONFLICT = "conflict"
        private const val KEY_LAST_BOOTSTRAP = "last_bootstrap"
        private const val KEY_IMPORTED = "imported"
        private const val KEY_IMPORT_HANDLED = "import_handled"
    }
}
