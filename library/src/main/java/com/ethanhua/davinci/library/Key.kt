package com.ethanhua.davinci.library

/**
 * 类描述
 *
 * @author ethanhua
 * @version 0.1
 * @since 2018/12/5
 */
const val ORIGIN_SIZE = -1

data class Key(val url: String, val width: Int = ORIGIN_SIZE, val height: Int = ORIGIN_SIZE) {

    fun getSafeKey(): String {
        return hashCode().toString()
    }

}