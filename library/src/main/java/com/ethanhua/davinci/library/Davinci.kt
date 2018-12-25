package com.ethanhua.davinci.library

import android.annotation.SuppressLint
import android.arch.lifecycle.LifecycleOwner
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import java.util.*
import kotlin.coroutines.coroutineContext

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

    private suspend fun loadImageActual(key: Key): Bitmap? {
        return getFromMemoryCache(key) ?:
            getFromDiskCache(key) ?:
            getFromNetWork(key)
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
            mMemoryLruCache.put(key.getSafeKey(), it)
        }
        return bitmap
    }

    private fun getFromNetWorkActual(key: Key): Bitmap? {
        val bitmapStream = DefaultImageHttpRequest().load(key.url)
        val bitmap =  BitmapFactory.decodeStream(bitmapStream)
        bitmap?.apply {
            mMemoryLruCache.put(key.getSafeKey(), bitmap)
            mDiskLruCache.put(key, bitmap)
        }
        return bitmap
    }

    private fun getFromMemoryCache(key: Key): Bitmap? {
        return mMemoryLruCache.get(key.getSafeKey())
    }

    private suspend fun getFromDiskCache(
        key: Key
    ): Bitmap? = getFromCacheAsync(key, ::getFromDiskCacheActual)

    private suspend fun getFromNetWork(
        key: Key
    ): Bitmap? = getFromCacheAsync(key, ::getFromNetWorkActual)

    private suspend fun getFromCacheAsync(
        key: Key,
        requestAction: (key: Key) -> Bitmap?
    ): Bitmap? {
        mSubAsyncJobs[key]?.cancel()
        // 使用父协程上下文启动新的协程，方便统一管理
        val loadingJob = GlobalScope.async(coroutineContext + Dispatchers.Default) {
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

    suspend fun load(context: Context, url: String, view: View?): Deferred<Bitmap?> {
        // 每次启动协程使用context相关的父协程作为上下文参数传入
        return GlobalScope.async(Main + getParentJob(context)) {
            if (context is LifecycleOwner) {
                context.lifecycle.addObserver(mLifecycleObserver)
            }
            var key = Key(url)
            view?.apply {
                val size = getSize(this).await()
                key = Key(url, size.first, size.second)
            }
            loadImageActual(key)
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