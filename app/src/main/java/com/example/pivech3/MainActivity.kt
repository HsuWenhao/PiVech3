package com.example.pivech3

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.pivech3.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Immersive status bar (沉浸式状态栏):
        // - status bar is transparent (set in theme)
        // - content is allowed to draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Apply the status bar inset to the app bar so the toolbar renders under the status bar.
        // Note: when the app bar is hidden (fullscreen video), we must NOT keep top padding here,
        // otherwise it will look like a large blank bar in landscape.
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarMain.appBarLayout) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val shouldPad = view.visibility == View.VISIBLE
            view.setPadding(
                view.paddingLeft,
                if (shouldPad) statusBarInsets.top else 0,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }

        // Apply system bar insets to the NavHost content only when NOT in fullscreen video.
        // In fullscreen video we draw edge-to-edge and hide system bars.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val navHost = findViewById<View>(R.id.nav_host_fragment_content_main)
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val isVideoPage = binding.appBarMain.appBarLayout.visibility != View.VISIBLE
            navHost.setPadding(
                navHost.paddingLeft,
                if (isVideoPage) 0 else systemBars.top,
                navHost.paddingRight,
                if (isVideoPage) 0 else systemBars.bottom
            )
            insets
        }

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_control, R.id.nav_slideshow, R.id.nav_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Fullscreen RTSP playback mode on the video page (nav_control).
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isVideoPage = destination.id == R.id.nav_control
            setVideoFullscreenMode(isVideoPage)
            drawerLayout.setDrawerLockMode(
                if (isVideoPage) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED
            )
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    navController.navigate(R.id.nav_home)
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_control -> {
                    navController.navigate(R.id.nav_control)
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_slideshow -> {
                    navController.navigate(R.id.nav_slideshow)
                    binding.drawerLayout.closeDrawers()
                    true
                }
                R.id.nav_settings -> {
                    navController.navigate(R.id.nav_settings)
                    binding.drawerLayout.closeDrawers()
                    true
                }
                else -> false
            }
        }
    }

    private fun setVideoFullscreenMode(enabled: Boolean) {
        binding.appBarMain.appBarLayout.visibility = if (enabled) View.GONE else View.VISIBLE
        binding.appBarMain.fab.visibility = if (enabled) View.GONE else View.VISIBLE

        // 1) Auto landscape while on Control page.
        requestedOrientation = if (enabled) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (enabled) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }

        // Re-apply insets after toggling visibilities/orientation.
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return when (item.itemId) {
            R.id.action_settings -> {
                navController.navigate(R.id.nav_settings)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}