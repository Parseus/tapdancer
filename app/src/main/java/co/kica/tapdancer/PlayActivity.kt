package co.kica.tapdancer

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import co.kica.tap.IntermediateBlockRepresentation

class PlayActivity : AppCompatActivity() {
    private val audio: AudioTrack? = null
    private var currentfile: String? = ""
    private var currentName = ""
    private var currentPath = ""
    private val pintent: Intent? = null
    private var mBoundService: PlaybackRunnable? = null
    private var task: Thread? = null

    //private boolean mBound = false;
    val state = 0
    private fun showAiplaneSuggestion() {

        // do the check in here
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val edit = prefs.edit()
        if (prefs.getBoolean("dontShowAirplane", false)) {
            return
        }

        // custom dialog
        val context: Context = this
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.airplanmode)
        dialog.setTitle("Reducing Noise")
        val cb = dialog.findViewById<View>(R.id.dontShowWarningAgain) as CheckBox
        val dialogButton = dialog.findViewById<View>(R.id.dialogButtonOk) as Button
        // if button is clicked, close the custom dialog
        dialogButton.setOnClickListener {
            if (cb == null) {
                println("CheckBox cb is NULL!!!!")
            }

            // save value
            edit.putBoolean("dontShowAirplane", cb.isChecked)
            edit.commit()
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun initFromPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val manager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volume_level = prefs.getInt("tap.volume", -1)
        if (volume_level != -1) {
            // restore volume level
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, volume_level, 0)
        }
    }

    private fun saveToPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val manager = this.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volume_level = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val edit = prefs.edit()
        edit.putInt("tap.volume", volume_level)
        edit.commit()
    }

    fun mBound(): Boolean {
        return task != null && task!!.isAlive
    }

    private val handler: Handler = object : Handler() {
        override fun handleMessage(message: Message) {
            if (message.arg1 == Activity.RESULT_OK) {
                val outputpath = message.obj
            } else if (message.arg1 == 999) {
                // do counter update
                updatedCounter = message.arg2
            } else if (message.arg1 == 9999) {
                // do stop
                clickStopFile(surface)
            } else if (message.arg1 == 99999) {
                // do stop
                //PlayActivity.this.clickPauseFile(surface);
                if (mBound()) {
                    mBoundService!!.pause()
                    refreshState()
                    if (mBoundService!!.state == PlaybackRunnable.STATE_PAUSED) {
                        surface!!.setScrolly("               Paused...       ")
                    } else {
                        surface!!.setScrolly("               Playing...       ")
                    }
                }
            } else if (message.arg1 == 11111) {
                // do stop
                //PlayActivity.this.clickPauseFile(surface);
                surface!!.setScrolly((message.obj as String))
            }
        }
    }
    private var surface: PlayerSurface? = null
    private var counterLength = 0
    private var updatedCounter = 0
    var messenger: Messenger? = null
        private set

    fun refreshState() {
        surface!!.inserted = currentfile != ""
    }

    override fun onNewIntent(intent: Intent) {
        currentfile = intent.getStringExtra("wavfile")
        if (currentfile == null) {
            currentfile = ""
            surface!!.setScrolly("               No tape loaded. Press EJECT to load a tape... ")
        } else {
            // we have a file...
            val parts = currentfile!!.split("[:]").toTypedArray()
            currentPath = parts[0]
            currentName = parts[1]

            //File z = new File(currentfile);
            //this.counterLength = (int)((z.length() - 44) / 44100);
            updateCountPos()
            surface!!.setScrolly("               Ready.  PRESS PLAY ON TAPE ...")
        }
        refreshState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_player);
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        surface = PlayerSurface(this, this)
        setContentView(surface)
        volumeControlStream = AudioManager.STREAM_MUSIC
        initFromPreferences()
        val intent = intent
        currentfile = intent.getStringExtra("wavfile")
        if (currentfile == null) {
            currentfile = ""
        } else {
            // we have a file...
            val parts = currentfile!!.split("[:]").toTypedArray()
            currentPath = parts[0]
            currentName = parts[1]
            updateCountPos()
            surface!!.setScrolly("               Ready.  PRESS PLAY ON TAPE ...")
        }
        refreshState()
        showAiplaneSuggestion()
        val rt = Runtime.getRuntime()
        val maxMemory = rt.maxMemory()
        Log.v("onCreate", "maxMemory:" + java.lang.Long.toString(maxMemory))
    }

    private fun updateCountPos() {
        if (currentfile == "") {
            counterLength = 0
            return
        }
        val temp = IntermediateBlockRepresentation(currentPath, currentName)
        counterLength = temp.length / temp.renderedSampleRate
    }

    override fun onStop() {
        if (mBound()) {
            mBoundService!!.stop()
            //unbindService(mConnection);
            freeService()
        }
        saveToPreferences()
        super.onStop()
        System.gc()
    }

    fun clickChooseFile(view: View?) {
        // start the file picker activity
        if (task != null && task!!.isAlive) {
            clickStopFile(view)
        }
        currentfile = ""
        surface!!.inserted = false
        surface!!.setScrolly("                       No tape loaded. Press EJECT to load a tape...                     ")
        val intent = Intent(this, FileChooser::class.java)
        startActivity(intent)
    }

    fun clickHandleTape(view: View?) {
        // moo
    }

    fun clickPlayFile(view: View?) {
        if (currentfile == "") return
        if (mBound() && mBoundService!!.state == PlaybackRunnable.STATE_PAUSED) {
            //System.out.println("Shit we must be confused...");
            clickPauseFile(view)
            return
        }
        if (mBound() && mBoundService!!.state == PlaybackRunnable.STATE_PLAYING) {
            //System.out.println("Shit we must be confused...");
            clickPauseFile(view)
            return
        }
        messenger = Messenger(handler)

        // cleanup if needed
        if (mBound()) {
            mBoundService!!.pause()
            mBoundService!!.stop()
            task!!.interrupt()
            freeService()
        }
        mBoundService = PlaybackRunnable(this, currentPath, currentName)
        task = Thread(mBoundService)
        task!!.start()
        try {
            Thread.sleep(1000)
        } catch (e: InterruptedException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        refreshState()
        //surface.scrolly = "               Playing...       ";
    }

    fun clickPauseFile(view: View?) {
        if (currentfile == "") return

        //bindService();
        if (mBound()) {
            if (mBoundService!!.isEnhancedPauseBehaviour && mBoundService!!.state == PlaybackRunnable.STATE_PLAYING) {
                println("SET BREAKPOINT")
                mBoundService!!.setPauseBreakpoint()
            } else {
                println("ACTUAL PAUSE")
                mBoundService!!.pause()
            }
            refreshState()
        }
    }

    private fun freeService() {
        task = null
        mBoundService = null
    }

    fun clickStopFile(view: View?) {
        if (currentfile == "") return
        if (mBound()) {
            mBoundService!!.pause()
            mBoundService!!.stop()
            freeService()
            refreshState()
            surface!!.setScrolly("               Stopped...       ")
        }
        surface!!.setScrolly("               Stopped...       ")
    }

    /* service handling */
    override fun onResume() {
        // TODO Auto-generated method stub
        super.onResume()
        surface!!.onResumeMySurfaceView()
        initFromPreferences()
    }

    override fun onPause() {
        clickStopFile(surface)
        super.onPause()
        surface!!.onPauseMySurfaceView()
        saveToPreferences()
    }

    val counterPos: Int
        get() = if (mBound()) {
            updatedCounter
        } else if (currentfile != "") {
            counterLength
        } else {
            0
        }

    fun clickLaunchWeb() {
        val webIntent = Intent(Intent.ACTION_VIEW)
        webIntent.data = Uri.parse("http://tapdancer.info")
        this.startActivity(webIntent)
    }

    fun clickLaunchHelp() {
        val intent = Intent(this, HelpActivity::class.java)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.quit -> {
                // quit program
                finish()
                true
            }
            R.id.help -> {
                clickLaunchHelp()
                true
            }
            R.id.web -> {
                clickLaunchWeb()
                true
            }
            R.id.menu_settings -> {
                launchSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun launchSettings() {
        val intent = Intent(this, UserSettingsActivity::class.java)
        startActivityForResult(intent, RESULT_SETTINGS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RESULT_SETTINGS -> {
            }
        }
    }

    override fun onBackPressed() {
        clickStopFile(null)
        //		Intent intent = new Intent(this, JYTSpotActivity.class);
//		startActivity(intent);
        finish()
    }

    companion object {
        private const val RESULT_SETTINGS = 0
    }
}