package com.ethanhua.davinci.library

import android.annotation.SuppressLint
import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.*

/**
 * 图片加载类，采用Kotlin协程代替传统java线程池的并发操作，资源利用更轻量，代码可读性更友好
 *
 * @author ethanhua
 * @version 0.1
 * @since 2018/12/5
 */
@SuppressLint("StaticFieldLeak")
object Davinci {

    private lateinit var mContext: Context

    private val mLifecycleObserver by lazy(LazyThreadSafetyMode.NONE) {
        HostLifecycleObserver()
    }

    private val mParentJobs by lazy(LazyThreadSafetyMode.NONE) {
        HashMap<Context, Job>()
    }

    private val mSubAsyncJobs by lazy(LazyThreadSafetyMode.NONE) {
        HashMap<Key, Job>()
    }

    private val mMemoryLruCache by lazy(LazyThreadSafetyMode.NONE) {
        MemoryLruCache()
    }

    private val mDiskLruCache by lazy {
        DiskLruCacheWrapper(getCacheFile(mContext, File_CACHE_NAME))
    }

    private suspend fun loadImageActual(context: Context, key: Key): Bitmap? {
        return getFromMemoryCache(key) ?:
               with(getParentJob(context)) {
                   getFromDiskCache(this, key) ?:
                   getFromNetWork(this, key)
        }
    }

    private fun getParentJob(context: Context): Job {
        var parentJob = mParentJobs[context]
        if (parentJob == null) {
            parentJob = Job()
            mParentJobs[context] = parentJob
        }
        return parentJob
    }

    private fun getFromDiskCacheActual(key: Key): Bitmap? {
        val bitmap = mDiskLruCache.get(key)
        bitmap?.let {
            mMemoryLruCache.put(key, it)
        }
        return bitmap
    }

    private fun getFromNetWorkActual(key: Key): Bitmap? {
        val bitmap = DefaultImageHttpRequest().loadImage(key.url)
        bitmap?.apply {
            mMemoryLruCache.put(key, bitmap)
            GlobalScope.launch {
                mDiskLruCache.put(key, bitmap)
            }
        }
        return bitmap
    }

    private fun getFromMemoryCache(key: Key): Bitmap? {
        return mMemoryLruCache[key]
    }

    private suspend fun getFromDiskCache(
        parentJob: Job,
        key: Key
    ): Bitmap? = getFromCacheAsync(parentJob, key, ::getFromDiskCacheActual)

    private suspend fun getFromNetWork(
        parentJob: Job,
        key: Key
    ): Bitmap? = getFromCacheAsync(parentJob, key, ::getFromNetWorkActual)

    private suspend fun getFromCacheAsync(
        parentJob: Job,
        key: Key,
        requestAction: (key: Key) -> Bitmap?
    ): Bitmap? {
        mSubAsyncJobs[key]?.cancel()
        val loadingJob = GlobalScope.async(Dispatchers.IO + parentJob) {
            requestAction(key)
        }
        mSubAsyncJobs[key] = loadingJob
        val bitmap = loadingJob.await()
        mSubAsyncJobs.remove(key)
        return bitmap
    }

    fun init(context: Context) {
        mContext = context.applicationContext
    }

    fun load(context: Context, url: String): Bitmap? {
        if (context is LifecycleOwner) {
            context.lifecycle.addObserver(mLifecycleObserver)
        }
        return runBlocking {
            loadImageActual(context, Key(url))
        }
    }

    fun cancelJob(context: Context) {
        mParentJobs[context]?.cancel()
        mParentJobs.remove(context)
    }

    fun clearCache() {
        mMemoryLruCache.clear()
        mDiskLruCache.clear()
    }
}