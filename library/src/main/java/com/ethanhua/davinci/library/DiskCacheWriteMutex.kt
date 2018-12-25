package com.ethanhua.davinci.library

import kotlinx.coroutines.sync.Mutex
import java.util.*


/**
 * 磁盘缓存写入互斥量
 *
 * @author ethanhua
 * @version 0.1
 * @since 2018/12/10
 */
class DiskCacheWriteMutex {
    private val mutexes = HashMap<String,WriteMutex>()
    private val writeLockPool = WriteMutexPool()

    fun acquire(safeKey: String): Mutex {
        var writeMutex: WriteMutex?
        synchronized(this) {
            writeMutex = mutexes[safeKey]
            if (writeMutex == null) {
                writeMutex = writeLockPool.obtain()
                mutexes[safeKey] = writeMutex!!
            }
            writeMutex!!.interestedThreads++
        }
        return writeMutex!!.lock
    }

    fun release(safeKey: String) {
        var writeMutex: WriteMutex?
        synchronized(this) {
            writeMutex = mutexes[safeKey]
            writeMutex?.apply {
                if (interestedThreads < 1) {
                    throw IllegalStateException(
                        "Cannot release a lock that is not held"
                                + ", safeKey: " + safeKey
                                + ", interestedThreads: " + interestedThreads
                    )
                }
                interestedThreads--
                if (interestedThreads == 0) {
                    val removed = mutexes.remove(safeKey)
                    if (removed != writeMutex) {
                        throw IllegalStateException(
                            ("Removed the wrong lock"
                                    + ", expected to remove: " + writeMutex
                                    + ", but actually removed: " + removed
                                    + ", safeKey: " + safeKey)
                        )
                    }
                    if (removed != null) {
                        writeLockPool.offer(removed)
                    }
                }
            }
        }
        writeMutex?.lock?.unlock()
    }

    private class WriteMutex {
        val lock: Mutex = Mutex()
        var interestedThreads: Int = 0
    }

    private class WriteMutexPool {
        private val pool = ArrayDeque<WriteMutex>()
        private val MAX_POOL_SIZE = 10

        fun obtain(): WriteMutex {
            var result: WriteMutex?
            synchronized(pool) {
                result = pool.poll()
            }
            if (result == null) {
                result = WriteMutex()
            }
            return result as WriteMutex
        }

        fun offer(writeMutex: WriteMutex) {
            synchronized(pool) {
                if (pool.size < MAX_POOL_SIZE) {
                    pool.offer(writeMutex)
                }
            }
        }
    }
}