package com.helloluckyhuang.lbspoiapp

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.helloluckyhuang.lbspoiapp.ui.floatframe.hideFloat
import com.helloluckyhuang.lbspoiapp.ui.floatframe.showFloat

class AppLifecycleObserver : DefaultLifecycleObserver {
    override fun onStart(owner: LifecycleOwner) {
        // 应用进入前台
        hideFloat()
    }

    override fun onStop(owner: LifecycleOwner) {
        // 应用进入后台
        showFloat()
    }
}