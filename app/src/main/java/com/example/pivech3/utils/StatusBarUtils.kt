package com.example.pivech3.utils

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.core.view.WindowCompat

object StatusBarUtils {

    /**
     * 设置状态栏颜色和图标颜色
     * @param activity 当前Activity
     * @param statusBarColor 状态栏颜色资源ID
     * @param lightStatusBar 状态栏图标是否亮色（true=黑色，false=白色）
     */
    fun setStatusBarColor(
        activity: Activity,
        statusBarColor: Int,
        lightStatusBar: Boolean = true
    ) {
        activity.window.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 设置状态栏颜色
                this.statusBarColor = activity.getColor(statusBarColor)

                // 设置状态栏图标颜色
                val windowInsetsController = WindowCompat.getInsetsController(this, decorView)
                windowInsetsController.isAppearanceLightStatusBars = lightStatusBar
            }
        }
    }

    /**
     * 设置透明状态栏，内容延伸到状态栏
     * @param activity 当前Activity
     * @param lightStatusBar 状态栏图标是否亮色
     */
    fun setTransparentStatusBar(activity: Activity, lightStatusBar: Boolean = true) {
        activity.window.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 设置透明状态栏
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                statusBarColor = android.graphics.Color.TRANSPARENT

                // 设置状态栏图标颜色
                val windowInsetsController = WindowCompat.getInsetsController(this, decorView)
                windowInsetsController.isAppearanceLightStatusBars = lightStatusBar
            }
        }
    }

    /**
     * 获取状态栏高度
     */
    fun getStatusBarHeight(context: android.content.Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}