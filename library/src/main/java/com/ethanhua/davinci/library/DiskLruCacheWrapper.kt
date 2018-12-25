package com.ethanhua.davinci.library

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bumptech.glide.disklrucache.DiskLruCache
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * 磁盘lru缓存
 *
 * @author ethanhua
 * @version 0.1
 * @since 2018/12/5
 */
const val File_CACHE_NAME = "davinci_bitmap_cache"
const val APP_VERSION = 1
const val VALUE_COUNT = 1
const val DISK_DEFAULT_CACHE_SIZE = 100 * 1024 * 1024
class DiskLruCacheWrapper(private val dir: File, val size: Long = DISK_DEFAULT_CACHE_SIZE.toLong()) :IDiskCache{

    private var mDiskLruCache: DiskLruCache?

    private val mWriteMutex by lazy {
        DiskCacheWriteMutex()
    }

    init {
        mDiskLruCache = getDiskLruCache()
    }

    @Synchronized
    private fun getDiskLruCache(): DiskLruCache? {
        if (mDiskLruCache == null) {
            mDiskLruCache = DiskLruCache.open(dir, APP_VERSION, VALUE_COUNT, size)
        }
        return mDiskLruCache
    }

    override fun get(key: Key): Bitmap? {
        val safeKey = key.getSafeKey()
        // 对于图片缓存 相同的key 值bitmap总是相同的，就算此处存在并发问题 比如在两次get之间有一次put,put总是put相同的值
        val value = getDiskLruCache()?.get(safeKey)
        return value?.getFile(0)?.path?.let {
            BitmapFactory.decodeFile(it)
        }
    }

    override fun put(key: Key, bitmap: Bitmap) {
        val safeKey = key.getSafeKey()
        GlobalScope.launch {
            // 用挂起而非直接阻塞
            mWriteMutex.acquire(safeKey).lock()
            try {
                // 如果有值了不重复存储
                val currentValue = getDiskLruCache()?.get(safeKey)
                if (currentValue != null) {
                    return@launch
                }
                // 在对同一个key进行put操作时需要阻塞等待前面的操作完成，保证文件存储操作的原子性
                var out: FileOutputStream? = null
                try {
                    val editor = getDiskLruCache()?.edit(safeKey)
                    val targetFile = editor?.getFile(0)
                    out = FileOutputStream(targetFile)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                    editor?.commit()
                } catch (e: Exception) {

                } finally {
                    try {
                        out?.close()
                    } catch (e: Exception) {

                    }
                }
            } catch (e: Exception) {

            } finally {
                mWriteMutex.release(safeKey)
            }
        }
    }

    override fun delete(key: Key) {
        getDiskLruCache()?.remove(key.hashCode().toString())
    }

    @Synchronized
    override fun clear() {
        try {
            mDiskLruCache?.delete()
        } catch (e: Exception) {

        } finally {
            mDiskLruCache = null
        }
    }
}

