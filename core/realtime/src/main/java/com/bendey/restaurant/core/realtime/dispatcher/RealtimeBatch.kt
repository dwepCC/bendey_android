package com.bendey.restaurant.core.realtime.dispatcher

/**
 * Coordinador global de batch — un evento -> un commit -> un notify por store.
 * Puerto de `dispatcher/batch.ts` (Tauri). El Dispatcher corre secuencialmente
 * (un evento a la vez), por lo que un contador simple basta — sin necesidad de Mutex.
 */
object RealtimeBatch {
    private var batchDepth = 0
    private val pendingFlushes = LinkedHashSet<() -> Unit>()

    fun getBatchDepth(): Int = batchDepth

    fun registerFlushOnCommit(cb: () -> Unit): () -> Unit {
        pendingFlushes.add(cb)
        return { pendingFlushes.remove(cb) }
    }

    private fun flushAll() {
        pendingFlushes.forEach { cb ->
            try {
                cb()
            } catch (_: Exception) {
                /* store notify */
            }
        }
    }

    /** Ejecuta [fn] dentro de un batch; notifica stores una sola vez al finalizar. */
    fun run(fn: () -> Unit) {
        batchDepth++
        try {
            fn()
        } finally {
            batchDepth--
            if (batchDepth == 0) {
                flushAll()
            }
        }
    }

    fun resetForTests() {
        batchDepth = 0
        pendingFlushes.clear()
    }
}
