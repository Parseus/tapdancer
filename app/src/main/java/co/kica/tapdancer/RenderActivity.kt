package co.kica.tapdancer

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Messenger
import android.view.View
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import co.kica.tap.C64Tape

class RenderActivity : AppCompatActivity() {
    private val tap: C64Tape? = null
    private lateinit var filename: String
    private val converted: String? = null
    private var pb: ProgressBar? = null
    var messenger: Messenger? = null
        private set
    private val handler: Handler = object : Handler() {
        override fun handleMessage(message: Message) {
            val outputpath = message.obj
            if (message.arg1 == 364) {
                // handle status update...
                pb!!.progress = message.arg2
            } else if (message.arg1 == Activity.RESULT_OK && outputpath != null) {
                //Toast.makeText(RenderActivity.this, "Audio Ready", Toast.LENGTH_LONG).show();
                // move to another activity
                // start playback service
                val intent = Intent(this@RenderActivity, PlayActivity::class.java)
                intent.putExtra("wavfile", outputpath.toString())
                startActivity(intent)
            } else {
                val context: Context = this@RenderActivity
                val alertDialogBuilder = AlertDialog.Builder(
                        context)

                // set title
                alertDialogBuilder.setTitle("Render Failed")
                // set dialog message
                alertDialogBuilder
                        .setMessage("It is possible that the file was corrupt, or you have found a bug. Would you like to try a different file?")
                        .setCancelable(false)
                        .setPositiveButton("Yes") { _, _ -> // if this button is clicked, close
                            // current activity
                            val intent = Intent(this@RenderActivity, FileChooser::class.java)
                            startActivity(intent)
                        }
                        .setNegativeButton("No") { _, _ -> // if this button is clicked, just close
                            // the dialog box and do nothing
                            val intent = Intent(this@RenderActivity, PlayActivity::class.java)
                            startActivity(intent)
                        }

                // create alert dialog
                val alertDialog = alertDialogBuilder.create()

                // show it
                alertDialog.show()
            }
        }
    }
    private var task: Thread? = null
    private var renderer: RenderRunnable? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_render)
        val intent = intent
        filename = intent.getStringExtra(FileChooser.PICKED_MESSAGE)!!
        val index = intent.getStringExtra(FileChooser.PICKED_SUBITEM)
        var idx = 0
        if (!index.isNullOrEmpty()) {
            idx = index.toInt()
        }

        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        this.window.setBackgroundDrawableResource(R.drawable.tapdancer_background)

        val fn = findViewById<View>(R.id.filename) as TextView
        fn.text = filename.replaceFirst(".+[/]".toRegex(), "")
        fn.typeface = ResourcesCompat.getFont(this, R.font.atarcc)

        // get progress bar reference
        pb = findViewById<View>(R.id.progressBar1) as ProgressBar

        // do the render
        messenger = Messenger(handler)
        /*Intent rintent = new Intent(this, RenderService.class);
        rintent.putExtra("MESSENGER", messenger);
        rintent.putExtra("tapfile", filename);
        startService(rintent);*/

        /* we now use a runnable here to do our stuff, benefit being that it will
         * be killed the instance the back button is pressed.
         *
         */renderer = RenderRunnable(this, filename, idx)
        task = Thread(renderer)
        task!!.start()
    }

    override fun onStop() {
        // if we have a task kill it
        if (task?.isAlive!!) {
            if (renderer != null) {
                renderer!!.cancel()
                renderer = null
            }
            task!!.interrupt()
            task = null
        }

        super.onStop()
        System.gc()
    }

    override fun onPause() {
        if (task?.isAlive!!) {
            if (renderer != null) {
                renderer!!.cancel()
                renderer = null
            }
            task!!.interrupt()
            task = null
        }

        super.onPause()
        System.gc()
    }
}