package com.ethanhua.davinci.library

import android.graphics.Bitmap
import java.io.File

/**
 * 类描述
 *
 * @author ethanhua
 * @version 0.1
 * @since 2018/12/5
 */
interface IMemoryCache {

    fun get(key: Key): Bitmap?

    fun put(key: Key, resource: Bitmap): Bitmap?

    fun remove(key: Key): Bitmap?

    fun clear()

}