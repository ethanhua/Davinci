package com.ethanhua.davinci.library

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * 类描述
 *
 * @author ethanhua
 * @version 0.1
 * @since 2018/12/10
 */

fun getCacheFile(context: Context, dirName: String): File {
    val cachePath = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
        || !Environment.isExternalStorageRemovable()) {
        context.externalCacheDir?.path
    } else {
        context.cacheDir?.path
    }
    return File(cachePath + File.separator + dirName)
}