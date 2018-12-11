package com.ethanhua.davinci.library

/**
 * 类描述
 *
 * @author ethanhua
 * @version 0.1
 * @since 2018/12/5
 */
data class Key(val url: String) {

    fun getSafeKey():String{
        return hashCode().toString()
    }

}