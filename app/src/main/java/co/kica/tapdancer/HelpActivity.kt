package co.kica.tapdancer

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity

class HelpActivity : AppCompatActivity(R.layout.activity_help) {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val wv = findViewById<WebView>(R.id.webView1)
        wv.loadUrl("file:///android_asset/html/index.html")
        wv.clearHistory()
    }

    fun clickCloseHelp(view: View) {
        finish()
    }
}