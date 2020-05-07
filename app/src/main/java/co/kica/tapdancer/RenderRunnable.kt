package co.kica.tapdancer

import android.app.Activity
import android.os.Environment
import android.os.Message
import android.os.RemoteException
import android.util.Log
import androidx.preference.PreferenceManager
import co.kica.fileutils.SmartFileInputStream
import co.kica.tap.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.util.*
import kotlin.math.roundToInt

class RenderRunnable(private val activity: RenderActivity,
                     private val tapFile: String,
                     private val index: Int) : Runnable {

    private var result = Activity.RESULT_CANCELED
    private var signal = true

    fun cancel() {
        signal = false
    }

    override fun run() {
        // here goes the code that executes the runnable
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val useLowSampleRate = sharedPrefs.getBoolean("prefUseLowSampleRate", false)
        val sr = if (useLowSampleRate) 22050 else 44100
        val tapfile = tapFile
        //String fn = (new File(tapfile)).getName().replace(".tap", ".wav").replace(".TAP", ".wav").replace(".CAS", ".wav").replace(".cas", ".wav");

        // test if output dir exists
        val outputdir = Environment.getExternalStorageDirectory().toString() + File.separator + "TapDancer"
        val od = File(outputdir)
        if (!od.exists()) {
            od.mkdirs()
            // write no media to the dir
            val nm = File("$outputdir${File.separator}.nomedia")
            try {
                nm.createNewFile()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                Log.w(javaClass.name, "Exceptions generating nomedia tag", e)
            }
        }

        //String outputpath = (new File( Environment.getExternalStorageDirectory(), fn )).getAbsolutePath();
        var md5 = "0000000000000000000000000000000"
        try {
            md5 = getMD5DigestForFile(tapfile)
        } catch (e2: Exception) {
            // TODO Auto-generated catch block
            Log.w(javaClass.name, "Exceptions generating file checksum ", e2)
        }
        if (index > 0) md5 = md5 + "." + Integer.toHexString(index)
        val outputpath = "$outputdir/$md5.manifest"
        val baseName = md5
        val f = File(outputpath)

        // make PRG / T64 re-render if the option has changed
        if (f.exists()) {
            val type = sharedPrefs.getString("prefPRGLoaderType", "1")
            val o_type = type!!.toInt()
            if (tapfile.toLowerCase(Locale.getDefault()).contains(".prg") || tapfile.toLowerCase(Locale.getDefault()).contains(".p00") ||
                    tapfile.toLowerCase(Locale.getDefault()).contains(".t64")) {
                // check if it was with the same loader
                var ibr: IntermediateBlockRepresentation? = IntermediateBlockRepresentation(outputdir, baseName)
                if (ibr!!.loaderType != o_type) {
                    f.delete()
                }
                ibr = null
                System.gc()
            } else if (tapfile.toLowerCase(Locale.getDefault()).contains(".tzx") || tapfile.toLowerCase(Locale.getDefault()).contains(".tap")) {
                var ibr: IntermediateBlockRepresentation? = IntermediateBlockRepresentation(outputdir, baseName)
                if (ibr!!.renderedSampleRate != sr) {
                    f.delete()
                }
                ibr = null
                System.gc()
            }
        }
        if (!f.exists()) {
            Log.i(javaClass.name, "Rendering audio to $outputpath")
            try {
                var tap: C64Tape? = C64Tape()
                tap!!.load(tapfile)
                if (tap.isValid) {
                    val t = Thread(RenderPercentPublisher(tap, this))
                    t.start()
                    tap.writeAudioStreamData(outputdir, baseName)
                    result = Activity.RESULT_OK
                } else {
                    tap = null
                    var msx: MSXTape? = MSXTape()
                    msx!!.load(tapfile)
                    if (msx.isValid) {
                        val t = Thread(RenderPercentPublisher(msx, this))
                        t.start()
                        msx.writeAudioStreamData(outputdir, baseName)
                        result = Activity.RESULT_OK
                    } else {
                        msx = null
                        var uef: UEFTape? = UEFTape()
                        uef!!.load(tapfile)
                        if (uef.isValid) {
                            val t = Thread(RenderPercentPublisher(uef, this))
                            t.start()
                            uef.writeAudioStreamData(outputdir, baseName)
                            result = Activity.RESULT_OK
                        } else {
                            uef = null
                            val tzx = TZXTape(sr)
                            tzx.load(tapfile)
                            if (tzx.isValid) {
                                val t = Thread(RenderPercentPublisher(tzx, this))
                                t.start()
                                tzx.writeAudioStreamData(outputdir, baseName)
                                result = Activity.RESULT_OK
                            } else {
                                var fuji: AtariTape? = AtariTape()
                                fuji!!.load(tapfile)
                                if (fuji.isValid) {
                                    val t = Thread(RenderPercentPublisher(fuji, this))
                                    t.start()
                                    fuji.writeAudioStreamData(outputdir, baseName)
                                    result = Activity.RESULT_OK
                                } else {
                                    fuji = null
                                    if (tapfile.toLowerCase(Locale.getDefault()).contains(".prg") ||
                                            tapfile.toLowerCase(Locale.getDefault()).contains(".t64") ||
                                            tapfile.toLowerCase(Locale.getDefault()).contains(".p00")) {
                                        val type = sharedPrefs.getString("prefPRGLoaderType", "1")
                                        val o_type = type!!.toInt()
                                        val prg = C64Program()
                                        prg.idx = index
                                        prg.loadModel = o_type
                                        prg.load(tapfile)
                                        val t = Thread(RenderPercentPublisher(prg, this))
                                        t.start()
                                        prg.writeAudioStreamData(outputdir, baseName)
                                        result = Activity.RESULT_OK
                                    } else {
                                        val zxt = ZXTAP(sr)
                                        zxt.load(tapfile)
                                        result = if (zxt.isValid) {
                                            val t = Thread(RenderPercentPublisher(zxt, this))
                                            t.start()
                                            zxt.writeAudioStreamData(outputdir, baseName)
                                            Activity.RESULT_OK
                                        } else {
                                            Activity.RESULT_CANCELED
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(javaClass.name, "Exceptions rendering audio", e)
            }
        } else {
            result = Activity.RESULT_OK
        }
        if (signal) {
            val messenger = activity.messenger
            val msg = Message.obtain()
            msg.arg1 = result
            msg.obj = "$outputdir:$baseName"
            try {
                messenger?.send(msg)
            } catch (e1: RemoteException) {
                Log.w(javaClass.name, "Exception sending message", e1)
            }
        }
    }

    @Throws(Exception::class)
    private fun createChecksum(filename: String): ByteArray {
        val fis: InputStream = SmartFileInputStream(filename)
        val buffer = ByteArray(1024)
        val complete = MessageDigest.getInstance("MD5")
        var numRead: Int
        do {
            numRead = fis.read(buffer)
            if (numRead > 0) {
                complete.update(buffer, 0, numRead)
            }
        } while (numRead != -1)
        fis.close()
        return complete.digest()
    }

    @Throws(Exception::class)
    private fun getMD5DigestForFile(filename: String): String {
        val b = createChecksum(filename)
        var result = ""
        for (i in b.indices) {
            result += ((b[i].toInt() and 0xff) + 0x100).toString(16).substring(1)
        }
        return result
    }

    inner class RenderPercentPublisher(private val tape: GenericTape?, private val rs: RenderRunnable) : Runnable {
        override fun run() {
            while (tape!!.renderPercent < 1) {
                // publish it and sleep
                try {
                    rs.sendPercentMessage((100 * tape.renderPercent).roundToInt())
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
            }
        }

    }

    fun sendPercentMessage(round: Int) {
        val messenger = activity.messenger
        val msg = Message.obtain()
        msg.arg1 = 364 // simple message id so we know what to do with it...
        msg.arg2 = round
        try {
            messenger?.send(msg)
        } catch (e1: RemoteException) {
            Log.w(javaClass.name, "Exception sending message", e1)
        }
    }

}