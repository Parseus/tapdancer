package co.kica.tapdancer

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SplashActivity : AppCompatActivity(R.layout.splash_2) {

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val d = Date(System.currentTimeMillis() + SPLASH_ACTIVITY_DURATION)
        Timer().schedule(object : TimerTask() {
            override fun run() {
                startActivity(Intent(this@SplashActivity, SecondSplashActivity::class.java))
            }
        }, d /*amount of time in milliseconds before execution*/)
    }

    companion object {
        private const val SPLASH_ACTIVITY_DURATION = 2000
    }

}