package com.ethanhua.davinci.sample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.ethanhua.davinci.library.Davinci
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    val avatar = "https://ethanhua.github.io/img/avatar.jpg"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val context = this
        Davinci.init(context)
        btnLoad.setOnClickListener {
            GlobalScope.launch(Main) {
                img.setImageBitmap(
                    Davinci.load(context, avatar,img).await()
                )
            }
        }
        btnClear.setOnClickListener {
            Davinci.clearCache()
        }
        GlobalScope.launch(Main) {
            img.setImageBitmap(
                Davinci.load(context, avatar,img).await()
            )
        }
    }

}
