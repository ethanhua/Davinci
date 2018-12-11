package com.ethanhua.davinci.library

import android.support.v4.util.Preconditions
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock


/**
 * 类描述
 *
 * @author ethanhua
 * @version 0.1
 * @since 2018/12/10
 */
class DiskCacheWriteLocker {
    private val locks = HashMap<String,WriteLock>()
    private val writeLockPool = WriteLockPool()

    fun acquire(safeKey: String) {
        var writeLock: WriteLock?
        synchronized(this) {
            writeLock = locks[safeKey]
            if (writeLock == null) {
                writeLock = writeLockPool.obtain()
                locks[safeKey] = writeLock!!
            }
            writeLock!!.interestedThreads++
        }
        writeLock!!.lock.lock()
    }

    fun release(safeKey: String) {
        var writeLock: WriteLock?
        synchronized(this) {
            writeLock = locks[safeKey]
            writeLock?.apply {
                if (interestedThreads < 1) {
                    throw IllegalStateException(
                        "Cannot release a lock that is not held"
                                + ", safeKey: " + safeKey
                                + ", interestedThreads: " + interestedThreads
                    )
                }
                interestedThreads--
                if (interestedThreads == 0) {
                    val removed = locks.remove(safeKey)
                    if (removed != writeLock) {
                        throw IllegalStateException(
                            ("Removed the wrong lock"
                                    + ", expected to remove: " + writeLock
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
        writeLock?.lock?.unlock()
    }

    private class WriteLock {
        val lock: Lock = ReentrantLock()
        var interestedThreads: Int = 0
    }

    private class WriteLockPool {
        private val pool = ArrayDeque<WriteLock>()
        private val MAX_POOL_SIZE = 10

        fun obtain(): WriteLock {
            var result: WriteLock?
            synchronized(pool) {
                result = pool.poll()
            }
            if (result == null) {
                result = WriteLock()
            }
            return result as WriteLock
        }

        fun offer(writeLock: WriteLock) {
            synchronized(pool) {
                if (pool.size < MAX_POOL_SIZE) {
                    pool.offer(writeLock)
                }
            }
        }
    }
}