package co.kica.tapdancer

import android.content.Intent
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(R.layout.activity_main) {

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    fun clickChooseFile(view: View) {
        // start the file picker activity
        val intent = Intent(this, FileChooser::class.java)
        startActivity(intent)
    }
}