package com.ethanhua.davinci.library

import java.io.InputStream

/**
 * 类描述
 *
 * @author ethanhua
 * @version 0.1
 * @since 2018/12/6
 */
interface IImageHttpRequest {

    fun load(url:String):InputStream?

}