package com.ethanhua.davinci.library

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.os.Environment
import android.support.v4.view.ViewCompat
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * 类描述
 *
 * @author ethanhua
 * @version 0.1
 * @since 2018/12/10
 */

const val PENDING_SIZE = 0

fun getCacheFile(context: Context, dirName: String): File {
    val cachePath = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
        || !Environment.isExternalStorageRemovable()
    ) {
        context.externalCacheDir?.path
    } else {
        context.cacheDir?.path
    }
    return File(cachePath + File.separator + dirName)
}

suspend fun getSize(view: View): Deferred<Pair<Int, Int>> {
    return GlobalScope.async(Main) {
        val width = getTargetSize(view, true)
        val height = getTargetSize(view, false)
        // if have layout just return the actual size
        if (isValidSize(width) && isValidSize(height)) {
            Pair(width, height)
        } else {
            // wait for layout
            getSizeAfterLayout(view)
        }
    }
}

fun getRealSize(view: View): Pair<Int, Int> {
    val width = getTargetSize(view, true)
    val height = getTargetSize(view, false)
    return Pair(width, height)
}

suspend fun getSizeAfterLayout(view: View): Pair<Int, Int> {
    return suspendCoroutine {
        view.viewTreeObserver.apply {
            addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    Log.e("event","onPreDraw")
                    if (isAlive) {
                        removeOnPreDrawListener(this)
                    }
                    it.resume(getRealSize(view))
                    return true
                }
            })
        }
    }
}

fun isValidSize(size: Int): Boolean {
    return size != PENDING_SIZE
}


fun getTargetSize(view: View, isWidth: Boolean): Int {
    val viewSize = if (isWidth) view.width else view.height
    val paramSize = (if (isWidth) view.layoutParams?.width else view.layoutParams?.height) ?: PENDING_SIZE
    val paddingSize = if (view is ViewGroup && !isClipPadding(view)) {
        0
    } else {
        if (isWidth)
            view.paddingLeft + view.paddingRight else
            view.paddingTop + view.paddingBottom
    }
    return getTargetDimen(view, viewSize, paramSize, paddingSize, isWidth)
}


fun getTargetDimen(
    view: View,
    viewSize: Int,
    paramSize: Int,
    paddingSize: Int,
    isWidth: Boolean
): Int {

    val adjustedParamsSize = paramSize - paddingSize
    // case 1 具体尺寸参数 >0 直接返回
    if (adjustedParamsSize > 0) {
        Log.e("event", "ParamsSize:$adjustedParamsSize")
        return adjustedParamsSize
    }

    // 如果在下次layout计划中或者还没有layout过就等待layout完成
    if (view.isLayoutRequested || !ViewCompat.isLaidOut(view)) {
        Log.e("event", "await layout")
        return PENDING_SIZE
    }

    // 已经layout过了

    val adjustedViewSize = viewSize - paddingSize

    // 实际尺寸大于 0 可直接返回
    if (adjustedViewSize > 0) {
        Log.e("event", "viewsize$adjustedViewSize")
        return adjustedViewSize
    }

    val parentView = getFirstClipParentView(view)

    if (parentView != null) {
        return getTargetSize(parentView, isWidth)
    }
    Log.e("event", "maxsize:${getMaxDisplayLength(view.context)}")
    // 获取最大尺寸
    return getMaxDisplayLength(view.context)
}

fun isClipChildren(viewGroup: ViewGroup): Boolean {
    var clip = true
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        clip = viewGroup.clipChildren
    }
    return clip
}

fun isClipPadding(viewGroup: ViewGroup): Boolean {
    var clip = true
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        clip = viewGroup.clipToPadding
    }
    return clip
}

fun getFirstClipParentView(view: View): ViewGroup? {
    val parentView = view.parent as? ViewGroup
    return parentView?.apply {
        if (!isClipChildren(parentView)) {
            getFirstClipParentView(this)
        }
    }
}

@SuppressLint("RestrictedApi")
private fun getMaxDisplayLength(context: Context): Int {
    if (maxDisplayLength == null) {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = windowManager?.defaultDisplay
        val displayDimensions = Point()
        display?.getSize(displayDimensions)
        maxDisplayLength = Math.max(displayDimensions.x, displayDimensions.y)
    }
    return maxDisplayLength as Int
}

var maxDisplayLength: Int? = null
