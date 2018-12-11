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
interface IDiskCache {

    fun get(key: Key): Bitmap?

    fun put(key: Key, bitmap: Bitmap)

    fun delete(key: Key)

    fun clear()

}