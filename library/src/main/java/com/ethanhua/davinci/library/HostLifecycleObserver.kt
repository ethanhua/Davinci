package com.ethanhua.davinci.library

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.OnLifecycleEvent
import android.content.Context

/**
 * 类描述
 *
 * @author ethanhua
 * @version 0.1
 * @since 2018/12/11
 */
class HostLifecycleObserver : LifecycleObserver{

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy(owner: LifecycleOwner?) {
        owner?.apply {
            lifecycle.removeObserver(this@HostLifecycleObserver)
            Davinci.cancelJob(this as Context)
        }
    }
}