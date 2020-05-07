package co.kica.tapdancer

import android.app.Activity
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Message
import android.os.RemoteException
import android.util.Log
import androidx.preference.PreferenceManager
import co.kica.tap.IntermediateBlockRepresentation

class PlaybackRunnable(private var playActivity: PlayActivity,
                       private val path: String,
                       private val name: String) : Runnable {

    private var result = Activity.RESULT_CANCELED
    private var audio: AudioTrack? = null
    private var minPauseDuration = 8000
    private val lastByte: Byte = 0x00
    private val sameCount = 0
    public var state = STATE_INITIALIZED
    private var length = 0
    private var position = 0

    private val pauseOnLongSilence = true
    private var longPauseDuration = 0
    private var pauseOnSilence = false
    private var pauseFirstSilence = false
    private var longSilencesOnly = false
    var isEnhancedPauseBehaviour = false
    private var cue: IntermediateBlockRepresentation? = null
    private var breakPoint = -1
    private val posUpdateListener: AudioTrack.OnPlaybackPositionUpdateListener? = null
    private var savedPlaybackHeadPosition = 0
    private var resumeCurrentBlock = false
    private var invertWaveform = false
    private var renderSampleRate = 0

    override fun run() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(playActivity)
        System.err.println(sharedPrefs.getString("prefShortSilenceDuration", "ndef"))
        minPauseDuration = sharedPrefs.getString("prefShortSilenceDuration", "8000")!!.toInt()
        longPauseDuration = sharedPrefs.getString("prefLongSilenceDuration", "44100")!!.toInt()
        pauseOnSilence = sharedPrefs.getBoolean("prefPauseDuringSilence", false)
        pauseFirstSilence = sharedPrefs.getBoolean("prefPauseFirstSilence", false)
        longSilencesOnly = sharedPrefs.getBoolean("prefLongSilencesOnly", false)
        resumeCurrentBlock = false
        invertWaveform = sharedPrefs.getBoolean("prefInvertWaveform", false)
        isEnhancedPauseBehaviour = sharedPrefs.getBoolean("prefPauseNextSilence", false)
        val path = path
        val name = name
        cue = IntermediateBlockRepresentation(path, name)
        Log.i(javaClass.name, "Playing file $path/$name")
        try {

            // At this point we already have the IBR data available and loaded
            // so we start at block index 1
            cue!!.reset()
            length = cue!!.length

            //audio.play();
            state = STATE_PLAYING
            renderSampleRate = cue!!.renderedSampleRate

            // now we fill and write the buffer to the audio track as fast as possible
            position = 0

            //byte[] buffer = cue.getCurrentBuffer();
            var chunk = cue!!.getCurrentBuffer(false)!!.size
            while (chunk > 0 && state != STATE_STOPPED) {
                if (isEnhancedPauseBehaviour && breakPoint != -1 && cue!!.playingBlock == breakPoint && state == STATE_PLAYING) {
                    pause()
                    breakPoint = -1
                } else if (pauseOnSilence) {

                    // if playing and we have silence
                    if (state == STATE_PLAYING && cue!!.type == "SILENCE" && cue!!.playingBlock != 1) {
                        var minToPause = minPauseDuration
                        if (longSilencesOnly) minToPause = longPauseDuration
                        println("block duration = " + cue!!.duration + ", firstsilence = " + cue!!.isFirstSilence + ", mintopause = " + minToPause)
                        if (cue!!.getCurrentBuffer(false)!!.size >= minToPause || cue!!.isFirstSilence && pauseFirstSilence) {
                            pause()
                        }
                    }
                }

                // if we are paused, pause at this point...
                if (state == STATE_PAUSED) {
                    sendScrollMessage("                   Paused :) ...")
                    while (state == STATE_PAUSED) {
                        Thread.sleep(500)
                    }
                    if (state == STATE_STOPPED) {
                        sendScrollMessage("                   Stopped ...")
                    }
                }
                if (audio != null) {
                    audio!!.stop()
                    audio!!.release()
                    audio = null
                }
                audio = AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        renderSampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_8BIT,
                        cue!!.getCurrentBuffer(false)!!.size,
                        AudioTrack.MODE_STATIC
                )
                sendScrollMessage("                   Playing...")
                audio!!.write(cue!!.getCurrentBuffer(invertWaveform)!!, 0, chunk)
                //buffer = null;
                System.gc()
                if (resumeCurrentBlock) {
                    // restore saved position
                    println("RESUMING FROM $savedPlaybackHeadPosition")
                    audio!!.playbackHeadPosition = savedPlaybackHeadPosition
                    resumeCurrentBlock = false
                }
                audio!!.play()

                // after starting playback wait 500ms
                Thread.sleep(500)
                while (audio != null && audio!!.playbackHeadPosition < chunk && state != STATE_STOPPED) {
                    savedPlaybackHeadPosition = audio!!.playbackHeadPosition
                    if (state == STATE_PAUSED) {
                        // special ops pause
                        if (audio != null) {
                            savedPlaybackHeadPosition = audio!!.playbackHeadPosition
                            resumeCurrentBlock = true
                            audio!!.pause()
                            audio!!.flush()
                            audio!!.release()
                            //audio = null;
                        }
                        break
                    }
                    println("WAITING FOR SAMPLE TO COMPLETE")
                    Thread.sleep(500)
                    updateCounter()
                }
                if (state == STATE_STOPPED) {
                    break
                }

                // update offset base point
                if (!resumeCurrentBlock) {
                    position += chunk

                    // send position
                    chunk = cue!!.nextBuffer()
                    //cue.getCurrentBuffer();
                }
            }
            sendScrollMessage("                   Stopped...")

            //dis.close();
            if (audio != null) {
                audio!!.stop()
                audio!!.release()
            }
            state = STATE_STOPPED
            result = Activity.RESULT_OK
        } catch (e: Exception) {
            Log.w(javaClass.name, "Exceptions playing wave", e)
        }
        sendStopMessage()
        cue = null
        System.gc()
        val messenger = playActivity.messenger
        val msg = Message.obtain()
        msg.arg1 = result
        try {
            messenger?.send(msg)
        } catch (e1: RemoteException) {
            Log.w(javaClass.name, "Exception sending message", e1)
        }
    }

    @Synchronized
    fun stop() {
        state = STATE_STOPPED
        if (audio != null && (state == STATE_PLAYING || state == STATE_PAUSED)) {
            audio!!.stop()
            audio!!.release()
            audio = null
        }
        cue = null
        System.gc()
    }

    @Synchronized
    fun pause() {
        //if (audio != null) {
        if (state == STATE_PAUSED) {
            //audio.play();
            state = STATE_PLAYING
        } else if (state == STATE_PLAYING) {
            //audio.pause();
            state = STATE_PAUSED
            resumeCurrentBlock = true
            if (audio != null) {
                savedPlaybackHeadPosition = audio!!.playbackHeadPosition
                audio!!.pause()
                audio!!.flush()
                audio!!.release()
                audio = null
            }
        }
        //}
    }

    @Synchronized
    fun play() {
        if (state == STATE_PAUSED) {
            state = STATE_PLAYING
        }
    }

    private val trackPosition: Int
        get() = (position + savedPlaybackHeadPosition) / renderSampleRate

    private val trackLength: Int
        get() = length / renderSampleRate

    fun setPauseBreakpoint() {
        breakPoint = if (cue!!.blockType(cue!!.playingBlock) == "SILENCE") {
            cue!!.playingBlock + 2
        } else {
            cue!!.playingBlock + 1
        }
        println("SET BREAKY ACHEY POINT INDEX TO $breakPoint")
    }

    private fun sendSilenceMessage() {
        val messenger = playActivity.messenger
        val msg = Message.obtain()
        msg.arg1 = 99999
        msg.arg2 = sameCount
        try {
            messenger?.send(msg)
        } catch (e1: RemoteException) {
            Log.w(javaClass.name, "Exception sending message", e1)
        }
    }

    private fun sendStopMessage() {
        val messenger = playActivity.messenger
        val msg = Message.obtain()
        msg.arg1 = 9999
        msg.arg2 = 0
        try {
            messenger?.send(msg)
        } catch (e1: RemoteException) {
            Log.w(javaClass.name, "Exception sending message", e1)
        }
    }

    private fun updateCounter() {
        val messenger = playActivity.messenger
        val msg = Message.obtain()
        msg.arg1 = 999
        msg.arg2 = trackLength - trackPosition
        try {
            messenger?.send(msg)
        } catch (e1: RemoteException) {
            Log.w(javaClass.name, "Exception sending message", e1)
        }
    }

    private fun sendScrollMessage(scrolly: String?) {
        val messenger = playActivity.messenger
        val msg = Message.obtain()
        msg.arg1 = 11111
        msg.arg2 = trackLength - trackPosition
        msg.obj = scrolly
        try {
            messenger?.send(msg)
        } catch (e1: RemoteException) {
            Log.w(javaClass.name, "Exception sending message", e1)
        }
    }

    companion object {
        const val STATE_PLAYING = 1
        const val STATE_INITIALIZED = 2
        const val STATE_PAUSED = 3
        const val STATE_STOPPED = 4
        var BUFFER_SIZE = 16
        const val DISK_SIZE = 8192
    }

}