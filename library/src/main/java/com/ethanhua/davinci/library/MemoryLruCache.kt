package com.ethanhua.davinci.library

import android.graphics.Bitmap
import android.support.v4.util.LruCache

/**
 * 类描述
 *
 * @author ethanhua
 * @version 0.1
 * @since 2018/12/5
 */
const val MEMORY_DEFAULT_CACHE_SIZE = 10 * 1024 * 1024
class MemoryLruCache(size: Int = MEMORY_DEFAULT_CACHE_SIZE) : LruCache<String, Bitmap>(size), IMemoryCache {

    override fun clear() {
        trimToSize(0)
    }

}