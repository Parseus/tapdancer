package co.kica.tapdancer

import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SecondSplashActivity : AppCompatActivity(R.layout.splash_1) {

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        super.onCreate(savedInstanceState)

        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val d = Date(System.currentTimeMillis() + SPLASH_ACTIVITY_DURATION)
        Timer().schedule(object : TimerTask() {
            override fun run() {
                startActivity(Intent(this@SecondSplashActivity, PlayActivity::class.java))
            }
        }, d /*amount of time in milliseconds before execution*/)
    }

    companion object {
        private const val SPLASH_ACTIVITY_DURATION = 3000L
    }
}