package co.kica.tapdancer

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Vibrator
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.content.res.ResourcesCompat
import java.util.*
import kotlin.math.roundToInt

class PlayerSurface(context: Context,
                    private val activity: PlayActivity) : SurfaceView(context), Runnable {
    private var thread: Thread? = null
    private var surfaceHolder: SurfaceHolder = holder

    @Volatile var running = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var random: Random = Random()
    private val tapedeck: Drawable
    private val tapedeck_inserted: Drawable
    private val viewBackground: Drawable
    private val www: Drawable
    private val help: Drawable
    private val menu: Drawable
    private var touched_x = 0f
    private var touched_y = 0f
    private var touched = false
    private var scrolly = "               Welcome to TapDancer ... Press EJECT to load a file... "
    private val audio: AudioManager
    private var www_h = 0
    private val counter_x = 450
    private val counter_y = 245
    @JvmField
    var inserted = false
    private var lastScrolly = ""
    fun onResumeMySurfaceView() {
        running = true
        thread = Thread(this)
        thread!!.start()
    }

    fun onPauseMySurfaceView() {
        var retry = true
        running = false
        while (retry) {
            try {
                thread!!.join()
                retry = false
            } catch (e: InterruptedException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
    }

    override fun run() {
        // TODO Auto-generated method stub
        while (running) {
            if (surfaceHolder.surface.isValid) {
                val canvas = surfaceHolder.lockCanvas()
                //... actual drawing on canvas
                paint.style = Paint.Style.FILL
                paint.strokeWidth = 3f
                paint.setARGB(176, 255, 192, 0)
                val s_w = canvas.width.toFloat()
                val s_h = canvas.height.toFloat()
                val x_mod = s_w / 480f
                val y_mod = s_h / 320f

                // calc size of deck
                val h = canvas.height
                val x1 = (canvas.width - h) / 2
                val y1 = 0
                val x2 = x1 + h

                // draw background
                viewBackground.setBounds(0, 0, canvas.width, canvas.height)
                viewBackground.draw(canvas)
                www_h = (canvas.width - canvas.height) / 3
                www.setBounds(0, canvas.height - www_h, www_h - 1, canvas.height)
                www.draw(canvas)
                paint.typeface = ResourcesCompat.getFont(context, R.font.atarcc)

                // draw help
                help.setBounds(0, 0, www_h - 1, www_h - 1)
                help.draw(canvas)

                // draw softmenu key
                menu.setBounds(0, ((s_h - www_h) / 2).roundToInt(), www_h - 1, ((s_h + www_h) / 2).roundToInt())
                menu.draw(canvas)

                // draw tape deck
                if (inserted) {
                    tapedeck_inserted.setBounds(x1, y1, x2, h)
                    tapedeck_inserted.draw(canvas)
                } else {
                    tapedeck.setBounds(x1, y1, x2, h)
                    tapedeck.draw(canvas)
                }
                //System.out.println("Canvas is w = "+canvas.getWidth()+", h = "+canvas.getHeight());

                // draw debug indication
                if (touched) {
                    canvas.drawCircle(touched_x, touched_y, 32f, paint)
                }

                // draw message
                paint.textSize = y_mod * 15
                var tmp = scrolly.substring(0, 15)
                val seed = "@@@@@@@@@@@@@@@"
                val bounds = Rect()
                paint.getTextBounds(seed, 0, seed.length, bounds)
                canvas.drawText(tmp, (s_w - bounds.right) / 2, y_mod * 225, paint)
                val ending = scrolly.substring(0, 1)
                scrolly = scrolly.substring(1) + ending

                // volume level
                val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                //audio.set

                // draw volume
                tmp = current.toString()
                if (tmp.length < 2) {
                    tmp = "0$tmp"
                }
                paint.getTextBounds(tmp, 0, tmp.length, bounds)
                canvas.drawText(tmp, canvas.width - bounds.right.toFloat(), canvas.height - bounds.bottom.toFloat(), paint)
                val rect = Rect()
                rect.left = canvas.width - bounds.right
                rect.right = canvas.width
                rect.bottom = canvas.height - paint.fontSpacing.roundToInt()
                val potential = (rect.bottom * ((max - current).toFloat() / max.toFloat())).roundToInt()
                rect.top = potential
                canvas.drawRect(rect, paint)
                paint.textSize = y_mod * 12

                // draw counter
                tmp = activity.counterPos.toString()
                while (tmp.length < 3) {
                    tmp = "0$tmp"
                }

                // ratio
                val cvt = canvas.height / 512f
                val ccx = counter_x * cvt + (canvas.width - canvas.height) / 2
                val ccy = counter_y * cvt
                paint.getTextBounds(tmp, 0, tmp.length, bounds)
                canvas.drawText(tmp, ccx - bounds.right / 2, ccy + paint.fontSpacing / 2, paint)
                surfaceHolder.unlockCanvasAndPost(canvas)
                try {
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // TODO Auto-generated method stub
        touched_x = event.x
        touched_y = event.y
        val s_w = surfaceHolder.surfaceFrame.width().toFloat()
        val s_h = surfaceHolder.surfaceFrame.height().toFloat()

        // check for www button
        if (touched_x < www_h && touched_y > s_h - www_h && event.action == MotionEvent.ACTION_DOWN) {
            activity.clickLaunchWeb()
            return true
        }

        // check for help button
        if (touched_x < www_h && touched_y < www_h && event.action == MotionEvent.ACTION_DOWN) {
            activity.clickLaunchHelp()
            return true
        }

        // check for settings button
        if (touched_x < www_h && touched_y >= (s_h - www_h) / 2 && touched_y <= (s_h + www_h) / 2 && event.action == MotionEvent.ACTION_DOWN) {
            activity.launchSettings()
            return true
        }
        val x_mod = 480f / s_w
        val y_mod = 320f / s_h

        // convert points
        val glx = x_mod * touched_x - 240f
        val gly = (s_h - touched_y) * y_mod - 160f
        println("Touch at ($glx, $gly), w = $s_w, h = $s_h")
        val vec = Vector(glx, gly)
        val button = getButtonPress(vec)
        val action = event.action
        touched = action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE
        if (button != "" && action == MotionEvent.ACTION_DOWN) {
            val v = activity.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            // Vibrate for 25 milliseconds
            v.vibrate(25)
            println("BUTTON: $button")
            if (button == "PLAY") {
                activity.clickPlayFile(this)
            }
            if (button == "STOP") {
                activity.clickStopFile(this)
            }
            if (button == "PAUSE") {
                activity.clickPauseFile(this)
            }
            if (button == "EJECT") {
                activity.clickChooseFile(this)
            }
        }
        return true //processed
    }

    inner class Vector(var x: Float, var y: Float)

    private fun getButtonPress(pos: Vector): String {
        if (pos.x > -111.4 && pos.x < -72 && pos.y > -147.4 && pos.y < -81) {
            return "EJECT"
        }
        if (pos.x > -62.7 && pos.x < -23.3 && pos.y > -147.4 && pos.y < -81) {
            return "PLAY"
        }
        if (pos.x > -15 && pos.x < 25.8 && pos.y > -147.4 && pos.y < -81) {
            return "PAUSE"
        }
        return if (pos.x > 33.6 && pos.x < 73.2 && pos.y > -147.4 && pos.y < -81) {
            "STOP"
        } else ""
    }

    fun getScrolly(): String {
        return scrolly
    }

    fun setScrolly(scrolly: String) {
        if (scrolly != lastScrolly) {
            this.scrolly = scrolly
            lastScrolly = scrolly
        }
    }

    init {
        // TODO Auto-generated constructor stub
        val res = this.resources
        tapedeck = res.getDrawable(R.drawable.td_player)
        tapedeck_inserted = res.getDrawable(R.drawable.td_player_inserted)
        viewBackground = res.getDrawable(R.drawable.tapdancer_background)
        www = res.getDrawable(R.drawable.td_www)
        help = res.getDrawable(R.drawable.td_help)
        menu = res.getDrawable(R.drawable.td_menu)
        audio = context.getSystemService(AUDIO_SERVICE) as AudioManager
    }
}