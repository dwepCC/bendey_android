package com.bendey.restaurant.core.realtime.store

import com.bendey.restaurant.core.realtime.dispatcher.RealtimeBatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Resultado de patch/remove en Domain Store. Puerto de `stores/contract.ts` (Tauri). */
enum class PatchResult { APPLIED, MISSING, IGNORED }

data class StoreMeta(
    val hydratedAt: String? = null,
    val lastEventAt: String? = null,
    val lastEventId: String? = null,
    val branchId: Int? = null,
    val loading: Boolean = false,
    val stale: Boolean = false,
    val version: Int = 0,
)

data class StoreSnapshot<T>(
    val entities: Map<String, T> = emptyMap(),
    val ids: List<String> = emptyList(),
    val meta: StoreMeta = StoreMeta(),
)

/** Contrato obligatorio para todos los Domain Stores — REALTIME_FRONTEND_ARCHITECTURE.md §3.6.1. */
interface RealtimeStore<T> {
    val state: StateFlow<StoreSnapshot<T>>
    fun hydrate(snapshot: StoreSnapshot<T>)
    fun patch(id: String, partial: T.() -> T): PatchResult
    fun upsert(entity: T): PatchResult
    fun remove(id: String): PatchResult
    fun reset()
    fun setMeta(mutate: (StoreMeta) -> StoreMeta)
    fun getSnapshot(): StoreSnapshot<T>
}

/** Puerto de `stores/createStore.ts` (Tauri) — factory con batch integrado. */
fun <T> createRealtimeStore(
    getId: (T) -> Any,
    sortIds: ((a: String, b: String, entities: Map<String, T>) -> Int)? = null,
): RealtimeStore<T> = RealtimeStoreImpl(getId, sortIds)

private class RealtimeStoreImpl<T>(
    private val getId: (T) -> Any,
    private val sortIds: ((a: String, b: String, entities: Map<String, T>) -> Int)?,
) : RealtimeStore<T> {

    private var snapshot: StoreSnapshot<T> = StoreSnapshot()
    private val _state = MutableStateFlow(snapshot)
    override val state: StateFlow<StoreSnapshot<T>> = _state.asStateFlow()
    private var dirty = false

    init {
        RealtimeBatch.registerFlushOnCommit { commitIfNeeded() }
    }

    private fun commitIfNeeded() {
        if (!dirty) return
        dirty = false
        snapshot = snapshot.copy(meta = snapshot.meta.copy(version = snapshot.meta.version + 1))
        _state.value = snapshot
    }

    private fun scheduleNotify() {
        dirty = true
        if (RealtimeBatch.getBatchDepth() == 0) commitIfNeeded()
    }

    override fun hydrate(snapshot: StoreSnapshot<T>) {
        this.snapshot = snapshot.copy(
            entities = snapshot.entities.toMap(),
            ids = snapshot.ids.toList(),
            meta = snapshot.meta.copy(loading = false, stale = false),
        )
        scheduleNotify()
    }

    override fun patch(id: String, partial: T.() -> T): PatchResult {
        val existing = snapshot.entities[id] ?: return PatchResult.MISSING
        val entities = snapshot.entities.toMutableMap()
        entities[id] = existing.partial()
        snapshot = snapshot.copy(entities = entities)
        scheduleNotify()
        return PatchResult.APPLIED
    }

    override fun upsert(entity: T): PatchResult {
        val key = getId(entity).toString()
        val exists = snapshot.entities.containsKey(key)
        val entities = snapshot.entities.toMutableMap()
        entities[key] = entity
        val nextIds = if (exists) {
            snapshot.ids
        } else {
            val sorter = sortIds
            val merged = snapshot.ids + key
            if (sorter != null) merged.sortedWith { a, b -> sorter(a, b, entities) } else merged
        }
        snapshot = snapshot.copy(entities = entities, ids = nextIds)
        scheduleNotify()
        return PatchResult.APPLIED
    }

    override fun remove(id: String): PatchResult {
        if (!snapshot.entities.containsKey(id)) return PatchResult.MISSING
        val entities = snapshot.entities.toMutableMap()
        entities.remove(id)
        snapshot = snapshot.copy(entities = entities, ids = snapshot.ids.filter { it != id })
        scheduleNotify()
        return PatchResult.APPLIED
    }

    override fun reset() {
        snapshot = StoreSnapshot()
        scheduleNotify()
    }

    override fun setMeta(mutate: (StoreMeta) -> StoreMeta) {
        snapshot = snapshot.copy(meta = mutate(snapshot.meta))
        scheduleNotify()
    }

    override fun getSnapshot(): StoreSnapshot<T> = snapshot
}
